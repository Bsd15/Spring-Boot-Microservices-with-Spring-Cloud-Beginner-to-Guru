package guru.sfg.beer.order.service.sm.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderValidateAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        log.debug("Inside execute");
        UUID beerOrderId = (UUID) stateContext
                .getMessageHeader(BeerOrderManagerImpl.ORDER_ID_HEADER);
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);
        beerOrderOptional.ifPresentOrElse(
                beerOrder -> {

                    ValidateBeerOrderRequest validateBeerOrderRequest = ValidateBeerOrderRequest
                            .builder()
                            .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                            .build();
                    jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER, validateBeerOrderRequest);
                },
                () -> log.error("Order Not Found. Id: {}", beerOrderId)
        );
    }
}
