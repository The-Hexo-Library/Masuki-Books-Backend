package com.masukibooks.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ReviewRequest {
    @NotNull
    private UUID productId;

    @NotNull
    private UUID orderId;

    @Min(1) @Max(5)
    private int rating;

    private String title;

    @NotBlank
    private String body;
}
