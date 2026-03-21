package com.masukibooks.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaveProgressRequest {

    @NotNull
    @Min(1)
    private Integer currentPage;

    @Min(0)
    private Long readingTimeSeconds;
}
