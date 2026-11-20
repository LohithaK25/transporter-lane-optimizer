package com.freightfox.transporter.api.dto.response;

public record ApiMessageResponse(
        String status,
        String message
) {
}