package ru.yourteam.lab.domain;

import java.time.Instant;

public final class Reagent {
    public long id;
    public String name;        // название (обязательно)
    public String formula;     // формула (может быть null)
    public String cas;         // CAS номер (может быть null)
    public String hazardClass; // класс опасности (может быть null)
    public String ownerUsername;
    public Instant createdAt;
    public Instant updatedAt;
}
