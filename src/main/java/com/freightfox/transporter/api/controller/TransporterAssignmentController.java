package com.freightfox.transporter.api.controller;

import com.freightfox.transporter.api.dto.request.AssignmentRequest;
import com.freightfox.transporter.api.dto.response.AssignmentResponse;
import com.freightfox.transporter.service.TransporterAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transporters")
public class TransporterAssignmentController {

    private final TransporterAssignmentService assignmentService;

    public TransporterAssignmentController(
            TransporterAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

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