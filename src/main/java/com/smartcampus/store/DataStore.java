package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton in-memory data store.
 *
 * Uses ConcurrentHashMap for rooms and sensors (thread-safe map operations)
 * and CopyOnWriteArrayList for sensor reading histories (safe concurrent reads).
 *
 * Because JAX-RS resource classes are request-scoped by default (a new instance
 * is created per request), all resources obtain the SAME DataStore instance via
 * getInstance(), ensuring data is shared across requests without race conditions
 * on the map-level operations.
 */
public class DataStore {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── In-memory collections ─────────────────────────────────────────────────

    /** Keyed by Room.id */
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    /** Keyed by Sensor.id */
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * Keyed by Sensor.id → ordered list of readings for that sensor.
     * CopyOnWriteArrayList makes concurrent reads safe without locking.
     */
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ConcurrentHashMap<String, Room> getRooms() { return rooms; }

    public ConcurrentHashMap<String, Sensor> getSensors() { return sensors; }

    public ConcurrentHashMap<String, List<SensorReading>> getReadings() { return readings; }

    /**
     * Convenience: get or initialise the readings list for a sensor.
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}
