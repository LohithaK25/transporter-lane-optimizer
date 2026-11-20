package com.freightfox.transporter.api.dto.response;

public record ApiErrorResponse(
        String status,
        String message
) {
}