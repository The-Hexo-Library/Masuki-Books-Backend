package com.masukibooks.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResaleListRequest {

    @NotNull(message = "User library ID is required")
    private String userLibraryId;

    @NotNull(message = "Listing price is required")
    @DecimalMin(value = "1.00", message = "Minimum listing price is 1.00")
    private BigDecimal listingPrice;
}
