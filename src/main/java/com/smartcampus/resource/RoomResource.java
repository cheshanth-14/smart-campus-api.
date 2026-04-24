package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.ErrorResponse;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Part 2 — Room Management
 *
 * Manages the /api/v1/rooms collection.
 *
 * Endpoints:
 *   GET    /api/v1/rooms          — list all rooms
 *   POST   /api/v1/rooms          — create a new room (returns 201 + Location)
 *   GET    /api/v1/rooms/{roomId} — fetch a single room
 *   DELETE /api/v1/rooms/{roomId} — decommission a room (409 if sensors exist)
 *
 * DELETE idempotency:
 *   First call on an existing room → 204 No Content (room removed).
 *   Subsequent calls → 404 Not Found.
 *   The server state after both calls is identical (room is gone), so DELETE
 *   satisfies the idempotency requirement even though the HTTP status differs
 *   on repeat calls.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /rooms ─────────────────────────────────────────────────────────────

    /**
     * Returns the full list of all rooms currently registered in the system.
     * Returning full objects avoids N+1 round-trips for the client, which is
     * acceptable here because room payloads are small.
     */
    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(store.getRooms().values())).build();
    }

    // ── POST /rooms ────────────────────────────────────────────────────────────

    /**
     * Creates a new room.  If no id is supplied in the JSON body, one is
     * generated automatically.  Returns 201 Created with a Location header
     * pointing to the new resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room == null) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("BAD_REQUEST", "Request body must be a valid Room JSON object.", 400))
                    .build();
        }

        // Auto-generate an ID if the client did not supply one
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId(UUID.randomUUID().toString());
        }

        // Prevent duplicate IDs
        if (store.getRooms().containsKey(room.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("DUPLICATE_ID",
                            "A room with id '" + room.getId() + "' already exists.", 409))
                    .build();
        }

        store.getRooms().put(room.getId(), room);

        URI location = UriBuilder.fromResource(RoomResource.class)
                .path(room.getId())
                .build();

        return Response.created(location).entity(room).build();
    }

    // ── GET /rooms/{roomId} ───────────────────────────────────────────────────

    /**
     * Returns detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND",
                            "Room not found with id: " + roomId, 404))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /rooms/{roomId} ────────────────────────────────────────────────

    /**
     * Decommissions (permanently removes) a room.
     *
     * Business logic constraint (Part 2.2):
     *   A room that still has sensors assigned cannot be deleted.
     *   Attempting to do so throws RoomNotEmptyException, which is mapped to
     *   HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND",
                            "Room not found with id: " + roomId, 404))
                    .build();
        }

        // Safety check: refuse deletion if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.getRooms().remove(roomId);
        return Response.noContent().build();   // 204
    }
}
