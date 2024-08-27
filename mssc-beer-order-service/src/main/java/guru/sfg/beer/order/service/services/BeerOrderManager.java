package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;

import java.util.UUID;

public interface BeerOrderManager {
    BeerOrder newBeerOrder(BeerOrder beerOrder);
    void validateBeerOrder(UUID beerOrderId, boolean isOrderValid);
    void beerOrderAllocationPassed(BeerOrderDto beerOrder);
    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrder);
    void beerOrderAllocationFailed(BeerOrderDto beerOrder);
    void beerOrderPickedUp(UUID beerOrderId);
    void cancelOrder(UUID id);
}
