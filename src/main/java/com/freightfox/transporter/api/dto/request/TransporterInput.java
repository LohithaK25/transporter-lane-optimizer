package com.freightfox.transporter.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TransporterInput(

        @NotNull
        @Positive
        Long id,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        List<@NotNull @Valid LaneQuoteInput> laneQuotes

) {
}