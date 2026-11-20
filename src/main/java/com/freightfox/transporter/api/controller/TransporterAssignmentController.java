package com.freightfox.transporter.api.controller;

import com.freightfox.transporter.api.dto.request.AssignmentRequest;
import com.freightfox.transporter.api.dto.response.AssignmentResponse;
import com.freightfox.transporter.service.TransporterAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

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