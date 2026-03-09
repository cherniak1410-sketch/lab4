package ru.yourteam.lab.domain;

import java.time.Instant;

public class Sample {
    private long id;
    private String name;
    private String type;
    private String location;
    private SampleStatus status;
    private String ownerUsername;
    private Instant createdAt;
    private Instant updatedAt;


    public Sample(String name, String type, String location) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.status = SampleStatus.ACTIVE;
        this.ownerUsername = "SYSTEM";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public SampleStatus getStatus() { return status; }
    public String getOwnerUsername() { return ownerUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }


    public void setId(long id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void setType(String type) {
        this.type = type;
        this.updatedAt = Instant.now();
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = Instant.now();
    }

    public void setStatus(SampleStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
        this.updatedAt = Instant.now();
    }
}