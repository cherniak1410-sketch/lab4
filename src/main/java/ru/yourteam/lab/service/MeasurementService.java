package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.repository.MeasurementRepository;
import ru.yourteam.lab.validator.MeasurementValidator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

/**
 * Сервис управления измерениями.
 *
 * Этап 7: добавлена расширенная фильтрация и статистика.
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

        repository.insert(measurement);
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

    // -------------------------------------------------------------------------
    // Расширенная фильтрация (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Универсальный фильтр измерений. Все параметры необязательны (null = не фильтровать).
     *
     * @param sampleId      фильтр по ID образца (0 = не фильтровать)
     * @param param         фильтр по параметру
     * @param owner         фильтр по владельцу
     * @param minValue      нижняя граница значения (включительно)
     * @param maxValue      верхняя граница значения (включительно)
     * @param measuredFrom  нижняя граница даты измерения
     * @param measuredTo    верхняя граница даты измерения
     */
    public List<Measurement> filter(long sampleId,
                                    MeasurementParam param,
                                    String owner,
                                    Double minValue,
                                    Double maxValue,
                                    Instant measuredFrom,
                                    Instant measuredTo) {
        return storage.values().stream()
                .filter(m -> !m.isDeleted())
                .filter(m -> sampleId <= 0 || m.getSampleId() == sampleId)
                .filter(m -> param == null || m.getParam() == param)
                .filter(m -> owner == null || owner.isBlank()
                        || m.getOwnerUsername().equalsIgnoreCase(owner))
                .filter(m -> minValue == null || m.getValue() >= minValue)
                .filter(m -> maxValue == null || m.getValue() <= maxValue)
                .filter(m -> measuredFrom == null || !m.getMeasuredAt().isBefore(measuredFrom))
                .filter(m -> measuredTo == null   || !m.getMeasuredAt().isAfter(measuredTo))
                .sorted((a, b) -> b.getMeasuredAt().compareTo(a.getMeasuredAt()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Статистика по измерениям (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Статистика (count/min/max/avg) для конкретного образца и параметра.
     */
    public Map<String, Object> getStats(long sampleId, MeasurementParam param) {
        List<Measurement> measurements = getBySampleIdAndParam(sampleId, param);
        if (measurements.isEmpty()) {
            throw new IllegalArgumentException("нет измерений " + param + " для sample=" + sampleId);
        }
        DoubleSummaryStatistics stats = measurements.stream()
                .mapToDouble(Measurement::getValue)
                .summaryStatistics();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", stats.getCount());
        result.put("min",   stats.getMin());
        result.put("max",   stats.getMax());
        result.put("avg",   stats.getAverage());
        return result;
    }

    /**
     * Статистика по всем параметрам для одного образца.
     * Возвращает Map: param -> {count, min, max, avg}
     */
    public Map<MeasurementParam, Map<String, Object>> getStatsBySample(long sampleId) {
        Map<MeasurementParam, Map<String, Object>> result = new LinkedHashMap<>();
        for (MeasurementParam p : MeasurementParam.values()) {
            List<Measurement> list = getBySampleIdAndParam(sampleId, p);
            if (!list.isEmpty()) {
                DoubleSummaryStatistics stats = list.stream()
                        .mapToDouble(Measurement::getValue)
                        .summaryStatistics();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("count", stats.getCount());
                m.put("min",   stats.getMin());
                m.put("max",   stats.getMax());
                m.put("avg",   stats.getAverage());
                result.put(p, m);
            }
        }
        return result;
    }

    /**
     * Глобальная статистика по каждому параметру (по всем образцам).
     * Возвращает Map: param -> {count, min, max, avg}
     */
    public Map<MeasurementParam, Map<String, Object>> getGlobalStats() {
        Map<MeasurementParam, Map<String, Object>> result = new LinkedHashMap<>();
        for (MeasurementParam p : MeasurementParam.values()) {
            DoubleSummaryStatistics stats = storage.values().stream()
                    .filter(m -> !m.isDeleted() && m.getParam() == p)
                    .mapToDouble(Measurement::getValue)
                    .summaryStatistics();
            if (stats.getCount() > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("count", stats.getCount());
                m.put("min",   stats.getMin());
                m.put("max",   stats.getMax());
                m.put("avg",   stats.getAverage());
                result.put(p, m);
            }
        }
        return result;
    }

    /**
     * Количество измерений по владельцу.
     */
    public Map<String, Long> countByOwner() {
        return storage.values().stream()
                .filter(m -> !m.isDeleted())
                .collect(Collectors.groupingBy(Measurement::getOwnerUsername, Collectors.counting()));
    }

    /**
     * Покрытие параметров для образца: какие из 4 параметров есть хоть одно измерение.
     */
    public Set<MeasurementParam> getCoveredParams(long sampleId) {
        return storage.values().stream()
                .filter(m -> !m.isDeleted() && m.getSampleId() == sampleId)
                .map(Measurement::getParam)
                .collect(Collectors.toSet());
    }

    /**
     * Общая сводка по измерениям.
     */
    public Map<String, Long> getSummary() {
        long total   = storage.values().stream().filter(m -> !m.isDeleted()).count();
        long deleted = storage.values().stream().filter(Measurement::isDeleted).count();

        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Всего измерений", total);
        map.put("Удалённых (soft)", deleted);
        for (MeasurementParam p : MeasurementParam.values()) {
            long cnt = storage.values().stream()
                    .filter(m -> !m.isDeleted() && m.getParam() == p).count();
            map.put("  " + p.name(), cnt);
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Удаление / Восстановление
    // -------------------------------------------------------------------------

    public void delete(long id) {
        Measurement measurement = storage.get(id);
        if (measurement == null) {
            throw new IllegalArgumentException("Измерение с id=" + id + " не найдено");
        }

        authService.requireOwnership(measurement.getOwnerUsername(), "Measurement#" + id);

        repository.updateDeleted(id, true);
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

        repository.updateDeleted(id, false);
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
