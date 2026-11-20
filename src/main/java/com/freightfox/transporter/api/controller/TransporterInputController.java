package com.freightfox.transporter.api.controller;

import com.freightfox.transporter.api.dto.request.SubmitInputRequest;
import com.freightfox.transporter.api.dto.response.ApiMessageResponse;
import com.freightfox.transporter.service.TransporterInputService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/transporters")
@Tag(
    name = "Transporter Input",
    description = "Submit lanes, transporters and lane quotes"
)
public class TransporterInputController {

    private final TransporterInputService inputService;

    public TransporterInputController(
            TransporterInputService inputService
    ) {
        this.inputService = inputService;
    }

    @Operation(
        summary = "Submit input data",
        description = "Validates and replaces the stored lanes, transporters and quotes."
    )
    
    @PostMapping("/input")
    public ResponseEntity<ApiMessageResponse> submitInput(
            @Valid @RequestBody SubmitInputRequest request
    ) {

        inputService.replaceInputData(request);

        return ResponseEntity.ok(
                new ApiMessageResponse(
                        "success",
                        "Input data saved successfully."
                )
        );
    }
}