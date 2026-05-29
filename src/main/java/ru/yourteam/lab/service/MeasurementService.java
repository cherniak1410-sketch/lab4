package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.repository.MeasurementRepository;
import ru.yourteam.lab.validator.MeasurementValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.DoubleSummaryStatistics;

/**
 * Сервис управления измерениями.
 *
 * Этап 6: данные хранятся синхронно в памяти (Map) и в PostgreSQL.
 * При старте загружаются из БД в память.
 */
public class MeasurementService {

    private final Map<Long, Measurement> storage = new ConcurrentHashMap<>();
    private final MeasurementRepository repository;
    private final AuthService authService;

    public MeasurementService(AuthService authService, MeasurementRepository repository) {
        this.authService = authService;
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Загрузка из БД при старте
    // -------------------------------------------------------------------------

    public void loadFromDatabase() {
        List<Measurement> all = repository.findAll();
        storage.clear();
        for (Measurement measurement : all) {
            storage.put(measurement.getId(), measurement);
        }
        System.out.println("[DB] Загружено " + all.size() + " измерений из PostgreSQL");
    }

    // -------------------------------------------------------------------------
    // Создание
    // -------------------------------------------------------------------------

    public Measurement add(long sampleId, String paramStr, double value,
                           String unit, String method) {
        authService.requireLogin();

        MeasurementValidator.validateParam(paramStr);
        MeasurementValidator.validateValue(value);
        MeasurementValidator.validateUnit(unit);
        MeasurementValidator.validateMethod(method);

        MeasurementParam param = MeasurementParam.valueOf(paramStr);

        Measurement measurement = new Measurement(sampleId, param, value, unit, method);
        measurement.setOwnerUsername(authService.getCurrentUsername());

        // 1. Сохраняем в БД (получаем ID)
        repository.insert(measurement);

        // 2. Сохраняем в память
        storage.put(measurement.getId(), measurement);

        System.out.println("[DB] Создано измерение #" + measurement.getId() + " для sample #" + sampleId);
        return measurement;
    }

    // -------------------------------------------------------------------------
    // Чтение (только из памяти)
    // -------------------------------------------------------------------------

    public Optional<Measurement> findById(long id) {
        Measurement measurement = storage.get(id);
        if (measurement != null && !measurement.isDeleted()) {
            return Optional.of(measurement);
        }
        return Optional.empty();
    }

    public List<Measurement> getBySampleId(long sampleId) {
        return storage.values().stream()
                .filter(m -> !m.isDeleted() && m.getSampleId() == sampleId)
                .sorted((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt()))
                .toList();
    }

    public List<Measurement> getBySampleIdAndParam(long sampleId, MeasurementParam param) {
        return storage.values().stream()
                .filter(m -> !m.isDeleted() && m.getSampleId() == sampleId && m.getParam() == param)
                .sorted((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt()))
                .toList();
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
        return result;
    }

    // -------------------------------------------------------------------------
    // Удаление / Восстановление (мягкое удаление)
    // -------------------------------------------------------------------------

    public void delete(long id) {
        Measurement measurement = storage.get(id);
        if (measurement == null) {
            throw new IllegalArgumentException("Измерение с id=" + id + " не найдено");
        }

        authService.requireOwnership(measurement.getOwnerUsername(), "Measurement#" + id);

        // 1. Мягкое удаление в БД
        repository.updateDeleted(id, true);

        // 2. Обновляем в памяти
        measurement.setDeleted(true);
        storage.put(id, measurement);

        System.out.println("[DB] Удалено (soft delete) измерение #" + id);
    }

    public void restore(long id) {
        Measurement measurement = storage.get(id);
        if (measurement == null || !measurement.isDeleted()) {
            throw new IllegalArgumentException("Измерение с id=" + id + " не найдено или не удалено");
        }

        authService.requireOwnership(measurement.getOwnerUsername(), "Measurement#" + id);

        // 1. Восстанавливаем в БД
        repository.updateDeleted(id, false);

        // 2. Обновляем в памяти
        measurement.setDeleted(false);
        storage.put(id, measurement);

        System.out.println("[DB] Восстановлено измерение #" + id);
    }

    // -------------------------------------------------------------------------
    // Для совместимости с UI
    // -------------------------------------------------------------------------

    public Map<Long, Measurement> getStorage() {
        return storage;
    }
}