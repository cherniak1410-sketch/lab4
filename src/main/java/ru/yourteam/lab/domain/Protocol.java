package ru.yourteam.lab.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class Protocol {
    private long id;
    private String name;
    private Set<MeasurementParam> requiredParams;
    private String ownerUsername;
    private Instant createdAt;
    private Instant updatedAt;

    // Конструктор
    public Protocol(String name, Set<MeasurementParam> requiredParams) {
        this.name = name;
        this.requiredParams = new HashSet<>(requiredParams); // копируем, чтобы не изменяли оригинал
        this.ownerUsername = "SYSTEM";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Геттеры
    public long getId() { return id; }
    public String getName() { return name; }
    public Set<MeasurementParam> getRequiredParams() {
        return new HashSet<>(requiredParams); // возвращаем копию, чтобы не изменяли извне
    }
    public String getOwnerUsername() { return ownerUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Сеттеры
    public void setId(long id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void setRequiredParams(Set<MeasurementParam> requiredParams) {
        this.requiredParams = new HashSet<>(requiredParams);
        this.updatedAt = Instant.now();
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
        this.updatedAt = Instant.now();
    }

    // Добавить один параметр в протокол
    public void addRequiredParam(MeasurementParam param) {
        this.requiredParams.add(param);
        this.updatedAt = Instant.now();
    }

    // Удалить параметр из протокола
    public void removeRequiredParam(MeasurementParam param) {
        this.requiredParams.remove(param);
        this.updatedAt = Instant.now();
    }
}