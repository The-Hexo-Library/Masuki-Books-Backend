package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PublicLibraryRequest {

    @NotNull
    private UUID productId;

    private Boolean isFeatured;

    private String visibility;

    private String notes;

    private Boolean editable;
}
