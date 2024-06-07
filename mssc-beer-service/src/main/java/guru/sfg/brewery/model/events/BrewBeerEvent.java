package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerDto;


public class BrewBeerEvent extends BeerEvent{
    public BrewBeerEvent(BeerDto beerDto) {
        super(beerDto);
    }
}
