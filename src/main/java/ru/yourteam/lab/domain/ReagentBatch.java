package ru.yourteam.lab.domain;

import java.time.Instant;
import java.time.LocalDate;

public final class ReagentBatch {
    public long id;
    public long reagentId;      // к какому реактиву относится
    public String label;        // номер партии
    public double initialQty;   // начальное количество
    public double currentQty;   // текущий остаток
    public String unit;         // "g" или "mL"
    public String location;     // где хранится
    public LocalDate expiryDate;// срок годности (может быть null)
    public String status;       // "ACTIVE", "EXPIRED", "EMPTY"
    public Instant createdAt;
    public Instant updatedAt;
}