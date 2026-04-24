package com.smartcampus.model;

/**
 * Standard error envelope returned by all Exception Mappers.
 * Ensures no raw stack traces ever reach the client.
 */
public class ErrorResponse {

    private String error;    // machine-readable error code
    private String message;  // human-readable explanation
    private int status;      // HTTP status code (mirrored for convenience)

    // ── Constructors ─────────────────────────────────────────────────────────

    public ErrorResponse() {}

    public ErrorResponse(String error, String message, int status) {
        this.error = error;
        this.message = message;
        this.status = status;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
