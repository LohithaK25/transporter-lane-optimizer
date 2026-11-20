package com.freightfox.transporter.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitInputRequest(

        @NotEmpty
        List<@NotNull @Valid LaneInput> lanes,

        @NotEmpty
        List<@NotNull @Valid TransporterInput> transporters

) {
}