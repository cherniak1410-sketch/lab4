package ru.yourteam.lab.domain;

import java.time.Instant;

public class Measurement {
    private long id;
    private long sampleId;
    private MeasurementParam param;
    private double value;
    private String unit;
    private String method;
    private Instant measuredAt;
    private String ownerUsername;
    private final Instant createdAt;
    private Instant updatedAt;

    // Конструктор для создания нового измерения
    public Measurement(long sampleId, MeasurementParam param, double value,
                       String unit, String method) {
        this.sampleId = sampleId;
        this.param = param;
        this.value = value;
        this.unit = unit;
        this.method = method;
        this.ownerUsername = "SYSTEM";
        this.measuredAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Геттеры (получить значения)
    public long getId() { return id; }
    public long getSampleId() { return sampleId; }
    public MeasurementParam getParam() { return param; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public String getMethod() { return method; }
    public Instant getMeasuredAt() { return measuredAt; }
    public String getOwnerUsername() { return ownerUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Сеттеры (изменить значения)
    public void setId(long id) { this.id = id; }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
        this.updatedAt = Instant.now();
    }

    public void setParam(MeasurementParam param) {
        this.param = param;
        this.updatedAt = Instant.now();
    }

    public void setValue(double value) {
        this.value = value;
        this.updatedAt = Instant.now();
    }

    public void setUnit(String unit) {
        this.unit = unit;
        this.updatedAt = Instant.now();
    }

    public void setMethod(String method) {
        this.method = method;
        this.updatedAt = Instant.now();
    }

    public void setMeasuredAt(Instant measuredAt) {
        this.measuredAt = measuredAt;
        this.updatedAt = Instant.now();
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
        this.updatedAt = Instant.now();
    }
}
