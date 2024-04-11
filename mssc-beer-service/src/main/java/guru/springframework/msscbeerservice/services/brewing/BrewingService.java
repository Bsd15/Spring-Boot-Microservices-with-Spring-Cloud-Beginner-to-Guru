package guru.springframework.msscbeerservice.services.brewing;

import guru.springframework.msscbeerservice.config.JmsConfig;
import guru.springframework.msscbeerservice.domain.Beer;
import guru.springframework.msscbeerservice.repositories.BeerRepository;
import guru.springframework.msscbeerservice.services.inventory.BeerInventoryService;
import guru.springframework.msscbeerservice.web.mappers.BeerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrewingService {
    private final JmsTemplate jmsTemplate;
    private final BeerRepository beerRepository;
    private final BeerInventoryService beerInventoryService;
    private final BeerMapper beerMapper;

    @Scheduled(fixedDelay = 5000)
    public void checkForLowInventory() {
        List<Beer> beers = beerRepository.findAll();

        beers.forEach(beer -> {
            int inventoryOnHand = beerInventoryService.getOnhandInventory(beer.getId());

            log.debug("Min on hand is: {}", beer.getMinOnHand());
            log.debug("Inventory on hand: {}", inventoryOnHand);
            if (beer.getMinOnHand() >= inventoryOnHand) {
                jmsTemplate.convertAndSend(JmsConfig.BREWING_REQUEST_QUEUE, beerMapper.beerToBeerDto(beer));
            }
        });
    }
}
