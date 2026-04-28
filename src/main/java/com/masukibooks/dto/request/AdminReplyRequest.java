package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminReplyRequest {

    @NotNull(message = "Contact submission ID is required")
    private UUID contactSubmissionId;

    @NotBlank(message = "Reply message is required")
    private String replyMessage;
}
