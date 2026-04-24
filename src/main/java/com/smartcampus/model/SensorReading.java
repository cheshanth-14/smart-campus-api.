package com.smartcampus.model;

/**
 * Represents a single historical measurement captured by a Sensor.
 */
public class SensorReading {

    private String id;        // UUID
    private long timestamp;   // epoch ms
    private double value;     // the recorded metric value

    // ── Constructors ─────────────────────────────────────────────────────────

    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
