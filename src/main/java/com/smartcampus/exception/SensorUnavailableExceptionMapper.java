package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.3 — State Constraint (403 Forbidden)
 *
 * Maps {@link SensorUnavailableException} to HTTP 403 Forbidden.
 * Triggered when a client attempts to POST a reading to a sensor whose
 * status is "MAINTENANCE".
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse body = new ErrorResponse(
                "SENSOR_UNAVAILABLE",
                "Sensor '" + ex.getSensorId() + "' is currently in MAINTENANCE mode. "
                        + "New readings cannot be accepted until the sensor is restored to ACTIVE status.",
                403
        );
        return Response
                .status(Response.Status.FORBIDDEN)  // 403
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
