package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Part 5.2 — Dependency Validation (422 Unprocessable Entity)
 *
 * Maps {@link LinkedResourceNotFoundException} to HTTP 422.
 *
 * 422 vs 404 rationale (also documented in README):
 *   404 signals the *URL* was not found.  422 signals that the *payload*
 *   refers to a resource that does not exist — the endpoint is reachable,
 *   the JSON is valid, but the business logic cannot process it.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                "UNPROCESSABLE_ENTITY",
                "The payload references a non-existent resource. "
                        + "Field: '" + ex.getField() + "', "
                        + "Value: '" + ex.getValue() + "'. "
                        + "Ensure the referenced resource exists before linking.",
                422
        );
        return Response
                .status(422)                        // 422 Unprocessable Entity
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
