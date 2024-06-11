package guru.springframework.msscbeerservice.services.order;

import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateBeerOrderResult;
import guru.springframework.msscbeerservice.config.JmsConfig;
import guru.springframework.msscbeerservice.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static guru.springframework.msscbeerservice.config.JmsConfig.VALIDATE_ORDER_RESULT;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;
    private final BeerRepository beerRepository;
    private final BeerOrderValidator beerOrderValidator;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void listen(ValidateBeerOrderRequest validateBeerOrderRequest) {
        log.debug("Validating beer order request: {}", validateBeerOrderRequest.getBeerOrderDto().getId());
        boolean isOrderValid = beerOrderValidator.validateOrder(validateBeerOrderRequest.getBeerOrderDto());
        ValidateBeerOrderResult validateBeerOrderResult = ValidateBeerOrderResult.builder()
                .orderId(validateBeerOrderRequest.getBeerOrderDto().getId())
                .isValid(isOrderValid)
                .build();
        log.debug("Order {} is {}. Sending event to queue", validateBeerOrderRequest.getBeerOrderDto().getId(), isOrderValid ? "valid" : "invalid");
        jmsTemplate.convertAndSend(VALIDATE_ORDER_RESULT, validateBeerOrderResult);
    }
}
