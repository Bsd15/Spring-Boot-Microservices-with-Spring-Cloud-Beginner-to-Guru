package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        log.debug("Inside newBeerOrder");
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        log.debug("Saved Beer Order Id: {}", beerOrder.getId());
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void validateBeerOrder(UUID beerOrderId, boolean isOrderValid) {
        log.debug("Beer Order {} is {}", beerOrderId, isOrderValid ? "valid" : "invalid");
        log.debug("Inside validateBeerOrder. Total saved beer orders: {}", beerOrderRepository.findAll().size());
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (isOrderValid) {
                log.debug("Beer order {} is valid", beerOrder.getId());
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                //wait for status change
                awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);

                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();

                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, () -> log.error("Order Not Found at validateBeerOrder    Id: {}", beerOrderId));
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);
            updateAllocatedQty(beerOrderDto);
        }, () -> log.error("Order Id Not Found: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);
            updateAllocatedQty(beerOrderDto);
        }, () -> log.error("Order Id Not Found: " + beerOrderDto.getId()));
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

        allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });

            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("Order Not Found. Id: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
        }, () -> log.error("Order Not Found. Id: " + beerOrderDto.getId()));
    }

    @Override
    public void beerOrderPickedUp(UUID beerOrderId) {
        log.debug("Inside pickUpBeerOrder");
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);
        beerOrderOptional.ifPresentOrElse(
                beerOrder -> sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEERORDER_PICKED_UP),
                () -> log.error("Beer Order: {} not found", beerOrderId)
        );
    }

    @Override
    public void cancelOrder(UUID id) {
        beerOrderRepository.findById(id).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
        }, () -> log.error("Order Not Found. Id: {}", id));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum event) {
        log.debug("Inside sendBeerOrderEvent");
        log.trace("Even to send: {}", event);
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = build(beerOrder);
        Message<BeerOrderEventEnum> message = MessageBuilder
                .withPayload(event)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId())
                .build();
        stateMachine.sendEvent(Mono.just(message)).subscribe();
    }

    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order Found");
                } else {
                    log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderStatus().name());
                }
            }, () -> {
                log.debug("Order Id Not Found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine = stateMachineFactory.getStateMachine(beerOrder.getId());
        stateMachine.stopReactively().subscribe();

        stateMachine
                .getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                            sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                            sma.resetStateMachineReactively(
                                    new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null)
                            ).subscribe();
                        }
                );
        stateMachine.startReactively().subscribe();

        return stateMachine;
    }

}
