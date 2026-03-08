package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.Sample;
import java.time.Instant;
import java.util.*;

public class SampleRepository {
    private final Map<Long, Sample> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Sample save(Sample sample) {
        if (sample.id == 0) {
            sample.id = nextId++;
        }
        sample.updatedAt = Instant.now();
        storage.put(sample.id, sample);
        return sample;
    }

    public Optional<Sample> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Sample> findAll() {
        return new ArrayList<>(storage.values());
    }
}