package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateBeerOrderResult;
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
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void list(Message msg){

        ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();

        log.debug("########### I RAN ########");

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT,
                ValidateBeerOrderResult.builder()
                        .isValid(true)
                        .orderId(request.getBeerOrderDto().getId())
                        .build());

    }
}
