package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    @Transactional
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message, Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> rootStateMachine) {
        Optional.ofNullable(message)
                .flatMap(m -> Optional.ofNullable(message.getHeaders().getOrDefault(ORDER_ID_HEADER, -1L)))
                .ifPresent(orderId -> {
                    log.debug("Saving state for order id: {} status: {}", orderId, state.getId());
                    BeerOrder beerOrder = beerOrderRepository.findById((UUID) orderId).orElseThrow();
                    beerOrder.setOrderStatus(state.getId());
                    beerOrderRepository.saveAndFlush(beerOrder);
                });
    }
}
