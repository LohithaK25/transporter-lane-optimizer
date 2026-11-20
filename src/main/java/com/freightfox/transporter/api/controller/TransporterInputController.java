package com.freightfox.transporter.api.controller;
import com.freightfox.transporter.api.dto.response.ApiErrorResponse;
import com.freightfox.transporter.api.dto.request.SubmitInputRequest;
import com.freightfox.transporter.api.dto.response.ApiMessageResponse;
import com.freightfox.transporter.service.TransporterInputService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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

    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Input data saved successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })

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