package ru.yourteam.lab.domain;

import java.time.Instant;

public final class StockMove {
    public long id;
    public long batchId;        // к какой партии относится
    public MoveType type;        // IN, OUT, DISCARD
    public double quantity;      // количество
    public String reason;        // причина (может быть null)
    public Instant movedAt;      // когда произошло движение
    public String ownerUsername;
    public Instant createdAt;
}