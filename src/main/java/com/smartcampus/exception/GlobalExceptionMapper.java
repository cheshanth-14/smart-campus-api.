package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 — Global Safety Net (500)
 *
 * Catch-all mapper for ANY Throwable not handled by a more specific mapper.
 * Intercepts NullPointerException, IndexOutOfBoundsException, and any other
 * unexpected runtime errors.
 *
 * Security rationale (see README for full answer):
 *   Stack traces expose internal file paths, library names and versions,
 *   class/method names, and server technology details — all valuable
 *   reconnaissance for an attacker.  This mapper swallows the trace server-
 *   side (it IS logged via java.util.logging for operators) and returns only
 *   a safe generic message to the client.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String errorCode = "INTERNAL_SERVER_ERROR";
        String message = "An unexpected error occurred on the server. Please contact the system administrator.";

        // If it's a standard JAX-RS exception (404, 405, etc.), keep its status
        if (ex instanceof javax.ws.rs.WebApplicationException) {
            javax.ws.rs.WebApplicationException wae = (javax.ws.rs.WebApplicationException) ex;
            status = wae.getResponse().getStatus();
            errorCode = "API_ERROR";
            message = ex.getMessage();
        } else {
            // Only log non-web exceptions as SEVERE
            LOGGER.log(Level.SEVERE, "Unhandled exception: " + ex.getMessage(), ex);
        }

        ErrorResponse body = new ErrorResponse(errorCode, message, status);

        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
