package guru.sfg.beer.order.service.services.listeners;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeerOrderAllocationResultListener {
    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESULT)
    public void listen(AllocateOrderResult allocateOrderResult) {
        log.debug("Inside AllocateOrderResultListener::listen()");
        if(!allocateOrderResult.isAllocationError() && !allocateOrderResult.isPendingInventory()){
            //allocated normally
            beerOrderManager.beerOrderAllocationPassed(allocateOrderResult.getBeerOrderDto());
        } else if(!allocateOrderResult.isAllocationError() && allocateOrderResult.isPendingInventory()) {
            //pending inventory
            beerOrderManager.beerOrderAllocationPendingInventory(allocateOrderResult.getBeerOrderDto());
        } else if(allocateOrderResult.isAllocationError()){
            //allocation error
            beerOrderManager.beerOrderAllocationFailed(allocateOrderResult.getBeerOrderDto());
        }
    }
}
