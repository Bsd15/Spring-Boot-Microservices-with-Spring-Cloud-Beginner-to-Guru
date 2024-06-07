package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.events.NewInventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewInventoryListener {

    private final BeerInventoryRepository beerInventoryRepository;

    @JmsListener(destination = JmsConfig.NEW_INVENTORY_QUEUE)
    public void listen(NewInventoryEvent newInventoryEvent) {
        log.debug("Received event: {}", newInventoryEvent);
        beerInventoryRepository.save(
                BeerInventory.builder()
                        .beerId(newInventoryEvent.getBeerDto().getId())
                        .upc(newInventoryEvent.getBeerDto().getUpc())
                        .quantityOnHand(newInventoryEvent.getBeerDto().getQuantityOnHand())
                        .build()
        );
    }
}
