package com.freightfox.transporter.service;

import com.freightfox.transporter.api.dto.response.AssignmentResponse;
import com.freightfox.transporter.api.dto.response.LaneAssignmentResponse;
import com.freightfox.transporter.domain.repository.LaneQuoteRepository;
import com.freightfox.transporter.domain.repository.LaneRepository;
import com.freightfox.transporter.service.optimization.TransporterAssignmentOptimizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransporterAssignmentService {

    private final LaneRepository laneRepository;
    private final LaneQuoteRepository laneQuoteRepository;
    private final TransporterAssignmentOptimizer optimizer;

    public TransporterAssignmentService(
            LaneRepository laneRepository,
            LaneQuoteRepository laneQuoteRepository,
            TransporterAssignmentOptimizer optimizer
    ) {
        this.laneRepository = laneRepository;
        this.laneQuoteRepository = laneQuoteRepository;
        this.optimizer = optimizer;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public AssignmentResponse getOptimizedAssignment(int maxTransporters) {

        var lanes = laneRepository.findAll();

        var quotes = laneQuoteRepository.findAllForOptimization();

        var result = optimizer.optimize(
                lanes,
                quotes,
                maxTransporters
        );

        List<LaneAssignmentResponse> assignments =
                result.assignments()
                        .stream()
                        .map(assignment -> new LaneAssignmentResponse(
                                assignment.laneId(),
                                assignment.transporterId()
                        ))
                        .toList();

        return new AssignmentResponse(
                "success",
                result.totalCost(),
                assignments,
                result.selectedTransporters()
        );
    }
}