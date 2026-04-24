package com.smartcampus.exception;

/**
 * Thrown when a DELETE /rooms/{roomId} is attempted on a room that still
 * has one or more sensors assigned to it.
 *
 * Mapped to HTTP 409 Conflict by {@link RoomNotEmptyExceptionMapper}.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' cannot be deleted — it still has "
                + sensorCount + " sensor(s) assigned.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId() { return roomId; }
    public int getSensorCount() { return sensorCount; }
}
