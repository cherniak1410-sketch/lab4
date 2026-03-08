package ru.yourteam.lab.domain;

import java.time.Instant;

public class Sample {
    public long id;
    public String name;
    public String type;
    public String location;
    public SampleStatus status;
    public String ownerUsername;
    public Instant createdAt;
    public Instant updatedAt;
}
