package com.smartcampus.exception;

/**
 * Thrown when a POST /sensors body references a roomId that does not exist
 * in the system.
 *
 * Mapped to HTTP 422 Unprocessable Entity by
 * {@link LinkedResourceNotFoundExceptionMapper}.
 *
 * Why 422 and not 404?
 *   HTTP 404 means the *request URL* was not found.  Here, the URL is valid
 *   (/api/v1/sensors exists).  The problem is that the *payload* contains a
 *   reference to a non-existent resource.  HTTP 422 accurately signals that
 *   the request was syntactically correct JSON but semantically unprocessable
 *   due to a failed business-rule validation inside the body.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String field;
    private final String value;

    public LinkedResourceNotFoundException(String field, String value) {
        super("Referenced resource not found: field='" + field + "', value='" + value + "'");
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }
}
