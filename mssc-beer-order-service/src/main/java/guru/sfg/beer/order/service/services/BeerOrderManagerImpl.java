package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
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

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    @Override
    @Transactional
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        log.debug("Inside newBeerOrder");
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        log.debug("Inside newBeerOrder. Total saved beer orders: {}", beerOrderRepository.findAll().size());
        return savedBeerOrder;
    }

    @Override
    @Transactional
    public void validateBeerOrder(UUID beerOrderId, boolean isOrderValid) {
        log.debug("Beer Order {} is {}", beerOrderId, isOrderValid ? "valid" : "invalid");
        log.debug("Inside validateBeerOrder. Total saved beer orders: {}", beerOrderRepository.findAll().size());
        BeerOrder beerOrder = beerOrderRepository.findOneById(beerOrderId);
        if (isOrderValid) {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
            BeerOrder validatedBeerOrder = beerOrderRepository.findOneById(beerOrderId);
            sendBeerOrderEvent(validatedBeerOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findOneById(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQty(beerOrderDto, beerOrder);
    }

    @Override
    @Transactional
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findOneById(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);

        updateAllocatedQty(beerOrderDto, beerOrder);
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.findOneById(beerOrderDto.getId());

        allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(beerOrderLine.getId() .equals(beerOrderLineDto.getId())){
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(beerOrder);
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.findOneById(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
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
