package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("integrationTest")
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {
        log.debug("Inside BeerOrderAllocationListener");
        BeerOrderDto beerOrderDto = (BeerOrderDto) msg.getPayload();
        log.debug("Payload: {}", beerOrderDto);
        AllocateOrderResult allocateOrderResult = AllocateOrderResult
                .builder()
                .allocationError(false)
                .pendingInventory(false)
                .beerOrderDto(beerOrderDto)
                .build();
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESULT, allocateOrderResult);
    }
}
