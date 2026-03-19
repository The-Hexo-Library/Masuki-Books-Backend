package com.masukibooks.service;

import com.masukibooks.entity.Inventory;
import com.masukibooks.entity.InventoryLog;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.InventoryLogRepository;
import com.masukibooks.repository.InventoryRepository;
import com.masukibooks.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    @SuppressWarnings("unused")
    private final ProductRepository productRepository;

    public Inventory getByProduct(UUID productId) {
        return inventoryRepository.findByProductProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }

    @Transactional
    public Inventory adjustStock(UUID productId, int quantityDelta, String reason, UUID changedBy) {
        Inventory inventory = getByProduct(productId);
        int newQty = inventory.getQuantity() + quantityDelta;
        if (newQty < 0) {
            throw new BusinessException("Adjustment would result in negative stock");
        }
        inventory.setQuantity(newQty);
        inventory = inventoryRepository.save(inventory);

        InventoryLog log = InventoryLog.builder()
                .product(inventory.getProduct())
                .changeQty(quantityDelta)
                .reason(reason)
                .build();
        inventoryLogRepository.save(log);

        return inventory;
    }

    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findLowStockItems();
    }
}
