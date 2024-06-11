package guru.springframework.msscbeerservice.services.order;

import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import guru.springframework.msscbeerservice.repositories.BeerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderValidator {

    private final BeerRepository beerRepository;

    public boolean validateOrder(BeerOrderDto beerOrderDto) {
        boolean isOrderValid = true;
       for (BeerOrderLineDto beerOrderLineDto : beerOrderDto.getBeerOrderLines()) {
           if (beerRepository.findByUpc(beerOrderLineDto.getUpc()) == null) {
               isOrderValid = false;
               break;
           }
       }
        return isOrderValid;
    }
}
