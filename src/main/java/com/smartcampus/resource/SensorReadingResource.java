package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Part 4 — Deep Nesting with Sub-Resources
 *
 * Handles all operations on the readings collection for a specific sensor.
 * This class is NOT annotated with @Path at class level — it is reached
 * exclusively via the sub-resource locator in {@link SensorResource}.
 *
 * Effective paths (after JAX-RS path composition):
 *   GET  /api/v1/sensors/{sensorId}/readings   — fetch full reading history
 *   POST /api/v1/sensors/{sensorId}/readings   — append a new reading
 *
 * Side effect on POST (Part 4.2):
 *   A successful POST updates {@code Sensor.currentValue} to the new reading's
 *   value, keeping the parent sensor record consistent with the latest data.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    /**
     * Constructor called by the sub-resource locator in SensorResource.
     * The sensorId is injected from the URL path segment.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ── GET /sensors/{sensorId}/readings ──────────────────────────────────────

    /**
     * Returns the complete historical reading list for this sensor.
     * Returns 404 if the parent sensor does not exist.
     */
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND",
                            "Sensor not found with id: " + sensorId, 404))
                    .build();
        }

        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    // ── POST /sensors/{sensorId}/readings ─────────────────────────────────────

    /**
     * Appends a new reading to the sensor's historical log.
     *
     * State constraint (Part 5.3):
     *   If the sensor's current status is "MAINTENANCE", new readings cannot
     *   be accepted.  {@link SensorUnavailableException} is thrown and mapped
     *   to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
     *
     * Side effect:
     *   On success, the parent {@link Sensor#setCurrentValue} is updated to
     *   the value of this reading so that GET /sensors/{id} always reflects
     *   the most recent measurement.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND",
                            "Sensor not found with id: " + sensorId, 404))
                    .build();
        }

        // State constraint: MAINTENANCE sensors cannot receive new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        if (reading == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("BAD_REQUEST",
                            "Request body must be a valid SensorReading JSON object.", 400))
                    .build();
        }

        // Auto-populate id and timestamp if not provided by the client
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.getReadingsForSensor(sensorId).add(reading);

        // ── Side effect: update the sensor's currentValue ─────────────────────
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
