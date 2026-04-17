package com.masukibooks.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CartItemRequest {
    @NotNull
    private UUID productId;

    @Min(1)
    private int quantity = 1;
}
