package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("integrationTest")
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {
        log.debug("Inside BeerOrderAllocationListener");
        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
        boolean allocationError = Objects.equals("fail-allocation", request.getBeerOrderDto().getCustomerRef());
        boolean pendingInventory = Objects.equals("partial-allocation", request.getBeerOrderDto().getCustomerRef());
        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            if (pendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });
        log.debug("Payload: {}", request);
        AllocateOrderResult allocateOrderResult = AllocateOrderResult
                .builder()
                .allocationError(allocationError)
                .pendingInventory(pendingInventory)
                .beerOrderDto(request.getBeerOrderDto())
                .build();
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT, allocateOrderResult);
    }
}
