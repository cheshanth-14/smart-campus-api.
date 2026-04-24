package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 — API Request & Response Logging Filter
 *
 * Implements BOTH ContainerRequestFilter (inbound) and ContainerResponseFilter
 * (outbound) in a single class annotated with @Provider so Jersey auto-registers
 * it for every request/response cycle without any extra configuration.
 *
 * Why filters over inline Logger.info() calls?
 *   Logging is a cross-cutting concern — it has nothing to do with business
 *   logic.  Scattering Logger.info() inside every resource method violates the
 *   DRY principle, makes the business code harder to read, and risks forgetting
 *   to add logging to new methods.  A filter is declared once, applies
 *   everywhere automatically, and can be enabled/disabled from a single place.
 *   This aligns with the Aspect-Oriented Programming (AOP) principle.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    // ── Inbound ───────────────────────────────────────────────────────────────

    /**
     * Called before the request reaches any resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST ] --> %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    // ── Outbound ──────────────────────────────────────────────────────────────

    /**
     * Called after the resource method has returned a response.
     * Logs the HTTP method, URI, and the final HTTP status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] <-- %s %s  [Status: %d]",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()
        ));
    }
}
