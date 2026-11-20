package com.freightfox.transporter.api.dto.response;

public record LaneAssignmentResponse(
        Long laneId,
        Long transporterId
) {
}