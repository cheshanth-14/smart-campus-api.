package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Operations & Linking
 *
 * Manages the /api/v1/sensors collection.
 *
 * Endpoints:
 *   GET  /api/v1/sensors              — list all sensors (optionally filter by ?type=)
 *   POST /api/v1/sensors              — register a new sensor (validates roomId exists)
 *   GET  /api/v1/sensors/{sensorId}   — fetch a single sensor
 *
 * Sub-resource locator (Part 4):
 *   ANY  /api/v1/sensors/{sensorId}/readings → delegates to SensorReadingResource
 *
 * @QueryParam vs @PathParam for filtering:
 *   Query parameters (?type=CO2) modify/narrow a collection without changing
 *   the resource identity.  Using a path segment (/sensors/type/CO2) wrongly
 *   implies "CO2" is itself a child resource, pollutes the URL hierarchy, and
 *   makes combining filters awkward.  @QueryParam is the idiomatic REST choice
 *   for optional, combinable search/filter criteria.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /sensors  (optional ?type= filter) ────────────────────────────────

    /**
     * Returns all sensors.  If the optional {@code type} query parameter is
     * supplied, only sensors whose type matches (case-insensitive) are returned.
     *
     * Example: GET /api/v1/sensors?type=CO2
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = store.getSensors().values()
                .stream()
                .filter(s -> type == null || type.isBlank()
                        || type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());

        return Response.ok(result).build();
    }

    // ── POST /sensors ─────────────────────────────────────────────────────────

    /**
     * Registers a new sensor.
     *
     * Validation (Part 3.1):
     *   The {@code roomId} field in the request body MUST reference an existing
     *   room.  If not, {@link LinkedResourceNotFoundException} is thrown, which
     *   the mapper converts to HTTP 422 Unprocessable Entity.
     *
     * @Consumes(APPLICATION_JSON):
     *   JAX-RS inspects the Content-Type header before invoking this method.
     *   A mismatch (e.g. text/plain or application/xml) causes the runtime to
     *   return HTTP 415 Unsupported Media Type without ever reaching this code.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("BAD_REQUEST",
                            "Request body must be a valid Sensor JSON object.", 400))
                    .build();
        }

        // Validate that the referenced room actually exists
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()
                || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }

        // Auto-generate ID if absent
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // Default status to ACTIVE
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        // Prevent duplicate sensor IDs
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("DUPLICATE_ID",
                            "A sensor with id '" + sensor.getId() + "' already exists.", 409))
                    .build();
        }

        // Persist sensor
        store.getSensors().put(sensor.getId(), sensor);

        // Register sensor ID in its parent room's sensor list
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise an empty readings list for this sensor
        store.getReadingsForSensor(sensor.getId());

        URI location = UriBuilder.fromResource(SensorResource.class)
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    // ── GET /sensors/{sensorId} ───────────────────────────────────────────────

    /**
     * Returns detailed information for a specific sensor.
     * This endpoint is required by the sub-resource navigation (clients need
     * to confirm a sensor exists before drilling into its readings).
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND",
                            "Sensor not found with id: " + sensorId, 404))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── Sub-resource locator: /sensors/{sensorId}/readings ────────────────────

    /**
     * Part 4.1 — Sub-Resource Locator Pattern.
     *
     * This method has NO HTTP-method annotation (@GET, @POST, …).
     * JAX-RS recognises it as a sub-resource locator: it resolves the path
     * segment and delegates ALL subsequent method matching to the returned
     * {@link SensorReadingResource} instance.
     *
     * Benefits over a monolithic controller:
     *   • Separation of concerns — reading logic lives in its own class.
     *   • Independent testability of SensorReadingResource.
     *   • Cleaner, scalable codebase as the API grows.
     *   • Lazy instantiation — the sub-resource object is only created when
     *     the /readings path is actually reached.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
