package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.validator.MeasurementValidator;

import java.util.*;
import java.util.DoubleSummaryStatistics;

/**
 * Сервис управления измерениями.
 *
 * Этап 5: создание требует авторизации; владелец = текущий пользователь.
 * Измерения не редактируются (только добавляются), поэтому проверка прав
 * нужна только при add.
 */
public class MeasurementService {

    private final Map<Long, Measurement> storage = new LinkedHashMap<>();
    private long nextId = 1;
    private final AuthService authService;

    public MeasurementService(AuthService authService) {
        this.authService = authService;
    }

    // Создание

    public Measurement add(long sampleId, String paramStr, double value,
                           String unit, String method) {
        authService.requireLogin();

        MeasurementValidator.validateParam(paramStr);
        MeasurementValidator.validateValue(value);
        MeasurementValidator.validateUnit(unit);
        MeasurementValidator.validateMethod(method);

        MeasurementParam param = MeasurementParam.valueOf(paramStr);

        Measurement measurement = new Measurement(sampleId, param, value, unit, method);
        measurement.setId(nextId++);
        measurement.setOwnerUsername(authService.getCurrentUsername());

        storage.put(measurement.getId(), measurement);
        return measurement;
    }

    // -------------------------------------------------------------------------
    // Чтение (доступно всем)
    // -------------------------------------------------------------------------

    public Optional<Measurement> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Measurement> getBySampleId(long sampleId) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : storage.values()) {
            if (m.getSampleId() == sampleId) result.add(m);
        }
        result.sort((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt()));
        return result;
    }

    public List<Measurement> getBySampleIdAndParam(long sampleId, MeasurementParam param) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : storage.values()) {
            if (m.getSampleId() == sampleId && m.getParam() == param) result.add(m);
        }
        result.sort((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt()));
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
        result.put("min",   stats.getMin());
        result.put("max",   stats.getMax());
        result.put("avg",   stats.getAverage());
        return result;
    }

    // -------------------------------------------------------------------------
    // Внутренний доступ
    // -------------------------------------------------------------------------

    public Map<Long, Measurement> getStorage() { return storage; }

    public void setNextId(long nextId) { this.nextId = nextId; }

    public long getNextId() { return nextId; }
    public void delete(long id) {
        if (!storage.containsKey(id)) {
            throw new IllegalArgumentException("Измерение с id=" + id + " не найден");
        }
        storage.remove(id);
    }
}
