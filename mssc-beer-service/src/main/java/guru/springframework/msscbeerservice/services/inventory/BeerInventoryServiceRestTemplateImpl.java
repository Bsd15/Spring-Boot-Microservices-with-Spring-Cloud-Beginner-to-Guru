package guru.springframework.msscbeerservice.services.inventory;

import guru.springframework.msscbeerservice.services.inventory.model.BeerInventoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by jt on 2019-06-07.
 */
@Slf4j
@Component
@Profile("!local-discovery")
public class BeerInventoryServiceRestTemplateImpl implements BeerInventoryService {

    public static final String INVENTORY_PATH = "/api/v1/beer/{beerId}/inventory";

    private final String inventoryPath;
    private final RestTemplate restTemplate;
    private final String beerInventoryServiceHost;

    public BeerInventoryServiceRestTemplateImpl(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${sfg.brewery.beer-inventory-service.host}") String beerInventoryServiceHost,
            @Value("${sfg.brewery.beer-inventory-service.inventory-path}") String inventoryPath
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.beerInventoryServiceHost = beerInventoryServiceHost;
        this.inventoryPath = inventoryPath;
    }

    @Override
    public Integer getOnhandInventory(UUID beerId) {

        log.debug("Calling Inventory Service");

        ResponseEntity<List<BeerInventoryDto>> responseEntity = restTemplate
                .exchange(beerInventoryServiceHost + inventoryPath, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<BeerInventoryDto>>() {
                        }, (Object) beerId);

        //sum from inventory list
        return Objects.requireNonNull(responseEntity.getBody())
                .stream()
                .mapToInt(BeerInventoryDto::getQuantityOnHand)
                .sum();
    }
}
