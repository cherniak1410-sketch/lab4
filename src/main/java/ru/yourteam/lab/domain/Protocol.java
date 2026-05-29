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

    // Конструктор для создания нового протокола
    public Protocol(String name, Set<MeasurementParam> requiredParams) {
        this.name = name;
        this.requiredParams = new HashSet<>(requiredParams);
        this.ownerUsername = "SYSTEM";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Геттеры
    public long getId() { return id; }
    public String getName() { return name; }
    public Set<MeasurementParam> getRequiredParams() {
        return new HashSet<>(requiredParams);
    }
    public String getOwnerUsername() { return ownerUsername; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Сеттеры для обычных полей
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

    // Сеттеры для восстановления из БД
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Методы для работы с параметрами
    public void addRequiredParam(MeasurementParam param) {
        this.requiredParams.add(param);
        this.updatedAt = Instant.now();
    }

    public void removeRequiredParam(MeasurementParam param) {
        this.requiredParams.remove(param);
        this.updatedAt = Instant.now();
    }
}