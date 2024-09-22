package guru.springframework.msscbeerservice.services.inventory;

import guru.springframework.msscbeerservice.services.inventory.model.BeerInventoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

import static guru.springframework.msscbeerservice.services.inventory.BeerInventoryServiceRestTemplateImpl.INVENTORY_PATH;

@FeignClient(name = "inventory-service")
public interface InventoryServiceFeignClient {
    @GetMapping(value = INVENTORY_PATH)
    ResponseEntity<List<BeerInventoryDto>> getOnhandInventory(@PathVariable UUID beerId);
}
