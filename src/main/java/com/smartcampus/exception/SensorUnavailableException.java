package com.smartcampus.exception;

/**
 * Thrown when a POST to /sensors/{sensorId}/readings is attempted while
 * the target sensor has status "MAINTENANCE".
 *
 * Mapped to HTTP 403 Forbidden by {@link SensorUnavailableExceptionMapper}.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor '" + sensorId + "' is in MAINTENANCE mode and cannot accept new readings.");
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }
}
