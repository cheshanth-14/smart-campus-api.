package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.1 — Resource Conflict (409)
 *
 * Maps {@link RoomNotEmptyException} to HTTP 409 Conflict.
 * Returns a structured JSON error body — never a raw stack trace.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorResponse body = new ErrorResponse(
                "RESOURCE_CONFLICT",
                "Room '" + ex.getRoomId() + "' cannot be decommissioned. "
                        + "It currently has " + ex.getSensorCount()
                        + " active hardware sensor(s) assigned. "
                        + "Please remove or reassign all sensors before deleting this room.",
                409
        );
        return Response
                .status(Response.Status.CONFLICT)   // 409
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
