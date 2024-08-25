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

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("integrationTest")
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void listen(Message msg){
        ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();
        boolean isValid = !Objects.equals("fail-validation", request.getBeerOrderDto().getCustomerRef());
        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT,
                ValidateBeerOrderResult.builder()
                        .isValid(isValid)
                        .orderId(request.getBeerOrderDto().getId())
                        .build());

    }
}
