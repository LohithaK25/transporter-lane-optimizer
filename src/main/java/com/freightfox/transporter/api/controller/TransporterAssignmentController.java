package com.freightfox.transporter.api.controller;
import com.freightfox.transporter.api.dto.response.ApiErrorResponse;
import com.freightfox.transporter.api.dto.request.AssignmentRequest;
import com.freightfox.transporter.api.dto.response.AssignmentResponse;
import com.freightfox.transporter.service.TransporterAssignmentService;
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
    name = "Transporter Assignment",
    description = "Calculate cost-optimized lane assignments"
)
public class TransporterAssignmentController {

    private final TransporterAssignmentService assignmentService;

    public TransporterAssignmentController(
            TransporterAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @Operation(
        summary = "Get optimized assignments",
        description = "Calculates the minimum-cost assignment using the maximum feasible number of transporters within the supplied limit."
    )

    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transporter assignments generated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid assignment request",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "A valid assignment could not be generated",
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

    @PostMapping("/assignment")
    public ResponseEntity<AssignmentResponse> getAssignment(
            @Valid @RequestBody AssignmentRequest request
    ) {

        return ResponseEntity.ok(
                assignmentService.getOptimizedAssignment(
                        request.maxTransporters()
                )
        );
    }
}