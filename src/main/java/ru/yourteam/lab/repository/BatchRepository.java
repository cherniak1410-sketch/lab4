package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.ReagentBatch;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class BatchRepository {
    private final Map<Long, ReagentBatch> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public ReagentBatch save(ReagentBatch batch) {
        if (batch.id == 0) {
            batch.id = nextId++;
        }
        batch.updatedAt = Instant.now();
        storage.put(batch.id, batch);
        return batch;
    }

    public Optional<ReagentBatch> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<ReagentBatch> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<ReagentBatch> findByReagentId(long reagentId) {
        return storage.values().stream()
                .filter(b -> b.reagentId == reagentId)
                .collect(Collectors.toList());
    }
}
