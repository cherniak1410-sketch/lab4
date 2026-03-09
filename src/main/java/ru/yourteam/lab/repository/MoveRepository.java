package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.StockMove;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MoveRepository {
    private final Map<Long, StockMove> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public StockMove save(StockMove move) {
        if (move.id == 0) {
            move.id = nextId++;
        }
        storage.put(move.id, move);
        return move;
    }

    public Optional<StockMove> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<StockMove> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<StockMove> findByBatchId(long batchId) {
        return storage.values().stream()
                .filter(m -> m.batchId == batchId)
                .sorted((m1, m2) -> m2.movedAt.compareTo(m1.movedAt)) // сначала новые
                .collect(Collectors.toList());
    }
}
