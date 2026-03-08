package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class MeasurementRepository {
    private final Map<Long, Measurement> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Measurement save(Measurement measurement) {
        if (measurement.id == 0) {
            measurement.id = nextId++;
        }
        measurement.updatedAt = Instant.now();
        storage.put(measurement.id, measurement);
        return measurement;
    }

    public Optional<Measurement> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Measurement> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Measurement> findBySampleId(long sampleId) {
        return storage.values().stream()
                .filter(m -> m.sampleId == sampleId)
                .collect(Collectors.toList());
    }

    public List<Measurement> findBySampleIdAndParam(long sampleId, MeasurementParam param) {
        return storage.values().stream()
                .filter(m -> m.sampleId == sampleId)
                .filter(m -> m.param == param)
                .collect(Collectors.toList());
    }
}
