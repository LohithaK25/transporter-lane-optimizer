package com.freightfox.transporter.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LaneInput(

        @NotNull
        @Positive
        Long id,

        @NotBlank
        @Size(max = 100)
        String origin,

        @NotBlank
        @Size(max = 100)
        String destination

) {
}