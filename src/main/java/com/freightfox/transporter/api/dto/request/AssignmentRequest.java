package com.freightfox.transporter.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentRequest(

        @NotNull
        @Positive
        Integer maxTransporters

) {
}