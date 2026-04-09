package ru.yourteam.lab.service;

import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.validator.MeasurementValidator;

import java.util.*;

public class MeasurementService {
    private final Map<Long, Measurement> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Measurement add(long sampleId, String paramStr, double value,
                           String unit, String method) {
        // Валидация
        MeasurementValidator.validateParam(paramStr);
        MeasurementValidator.validateValue(value);
        MeasurementValidator.validateUnit(unit);
        MeasurementValidator.validateMethod(method);

        MeasurementParam param = MeasurementParam.valueOf(paramStr);

        // Создание
        Measurement measurement = new Measurement(sampleId, param, value, unit, method);
        measurement.setId(nextId++);

        // Сохранение
        storage.put(measurement.getId(), measurement);
        return measurement;
    }

    public Optional<Measurement> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Measurement> getBySampleId(long sampleId) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : storage.values()) {
            if (m.getSampleId() == sampleId) {
                result.add(m);
            }
        }
        // Сортировка по времени (сначала новые)
        result.sort((m1, m2) -> m2.getMeasuredAt().compareTo(m1.getMeasuredAt()));
        return result;
    }

    public List<Measurement> getBySampleIdAndParam(long sampleId, MeasurementParam param) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : storage.values()) {
            if (m.getSampleId() == sampleId && m.getParam() == param) {
                result.add(m);
            }
        }
        // Сортировка по времени (сначала новые)
        result.sort((m1, m2) -> m2.getMeasuredAt().compareTo(m1.getMeasuredAt()));
        return result;
    }

    public Map<String, Object> getStats(long sampleId, MeasurementParam param) {
        List<Measurement> measurements = getBySampleIdAndParam(sampleId, param);

        if (measurements.isEmpty()) {
            throw new IllegalArgumentException("нет измерений " + param + " для sample=" + sampleId);
        }

        DoubleSummaryStatistics stats = measurements.stream()
                .mapToDouble(Measurement::getValue)
                .summaryStatistics();

        Map<String, Object> result = new HashMap<>();
        result.put("count", stats.getCount());
        result.put("min", stats.getMin());
        result.put("max", stats.getMax());
        result.put("avg", stats.getAverage());

        Map<String, Object> stats = new HashMap<>();
        stats.put("count", measurements.size());
        stats.put("min", min);
        stats.put("max", max);
        stats.put("avg", avg);

        return stats;
    }
    public Map<Long, Measurement> getStorage() {
        return storage;
    }
    public void setNextId(long nextId) {
        this.nextId = nextId;
    }
}