package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocateOrderRequestListener {
    private final JmsTemplate jmsTemplate;
    private final AllocationService allocationService;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest allocateOrderRequest) {
        log.debug("Inside AllocateOrderRequestListener::listen()");
        AllocateOrderResult.AllocateOrderResultBuilder allocateOrderResultBuilder = AllocateOrderResult.builder();
        allocateOrderResultBuilder.beerOrderDto(allocateOrderRequest.getBeerOrderDto());
        boolean allocationError;
        try {
            allocationError = allocationService.allocateOrder(allocateOrderRequest.getBeerOrderDto());
        } catch (Exception e) {
            log.error("Allocation failed for Order Id {}", allocateOrderRequest.getBeerOrderDto().getId(), e);
            allocationError = false;
        }

        allocateOrderResultBuilder.allocationError(allocationError);
        allocateOrderResultBuilder.pendingInventory(!allocationError);

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT, allocateOrderResultBuilder.build());
    }
}
