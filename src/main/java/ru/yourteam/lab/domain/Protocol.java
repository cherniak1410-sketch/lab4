package ru.yourteam.lab.domain;

import java.time.Instant;
import java.util.Set;

public final class Protocol {
    public long id;
    public String name;
    public Set<MeasurementParam> requiredParams;
    public String ownerUsername;
    public Instant createdAt;
    public Instant updatedAt;
}
