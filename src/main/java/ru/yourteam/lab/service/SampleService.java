package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.repository.SampleRepository;
import ru.yourteam.lab.validator.SampleValidator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис управления образцами.
 *
 * Этап 7: добавлена расширенная фильтрация и агрегация.
 */
public class SampleService {

    private final Map<Long, Sample> storage = new ConcurrentHashMap<>();
    private final SampleRepository repository;
    private final AuthService authService;

    public SampleService(AuthService authService, SampleRepository repository) {
        this.authService = authService;
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Загрузка из БД при старте
    // -------------------------------------------------------------------------

    public void loadFromDatabase() {
        List<Sample> all = repository.findAll();
        storage.clear();
        for (Sample sample : all) {
            storage.put(sample.getId(), sample);
        }
        System.out.println("[DB] Загружено " + all.size() + " образцов из PostgreSQL");
    }

    // -------------------------------------------------------------------------
    // Создание
    // -------------------------------------------------------------------------

    public Sample add(String name, String type, String location) {
        authService.requireLogin();

        SampleValidator.validateName(name);
        SampleValidator.validateType(type);
        SampleValidator.validateLocation(location);

        Sample sample = new Sample(name, type, location);
        sample.setOwnerUsername(authService.getCurrentUsername());

        repository.insert(sample);
        storage.put(sample.getId(), sample);

        System.out.println("[DB] Создан образец #" + sample.getId());
        return sample;
    }

    // -------------------------------------------------------------------------
    // Чтение (только из памяти)
    // -------------------------------------------------------------------------

    public Optional<Sample> findById(long id) {
        Sample sample = storage.get(id);
        if (sample != null && !sample.isDeleted()) {
            return Optional.of(sample);
        }
        return Optional.empty();
    }

    public Sample getById(long id) {
        return findById(id)
                .orElseThrow(() -> new IllegalArgumentException("образец с id=" + id + " не найден"));
    }

    public List<Sample> getAll() {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparing(Sample::getId))
                .toList();
    }

    public List<Sample> getDeleted() {
        return storage.values().stream()
                .filter(Sample::isDeleted)
                .toList();
    }

    public List<Sample> filterByStatus(SampleStatus status) {
        return storage.values().stream()
                .filter(s -> !s.isDeleted() && s.getStatus() == status)
                .toList();
    }

    public List<Sample> filterByOwner(String username) {
        return storage.values().stream()
                .filter(s -> !s.isDeleted() && s.getOwnerUsername().equals(username))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Расширенная фильтрация (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Универсальный фильтр образцов. Все параметры необязательны (null = не фильтровать).
     *
     * @param status     фильтр по статусу
     * @param owner      фильтр по владельцу (точное совпадение)
     * @param typeContains  подстрока в поле type (без учёта регистра)
     * @param nameContains  подстрока в поле name (без учёта регистра)
     * @param createdFrom   нижняя граница createdAt (включительно)
     * @param createdTo     верхняя граница createdAt (включительно)
     */
    public List<Sample> filter(SampleStatus status,
                               String owner,
                               String typeContains,
                               String nameContains,
                               Instant createdFrom,
                               Instant createdTo) {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> owner == null || owner.isBlank()
                        || s.getOwnerUsername().equalsIgnoreCase(owner))
                .filter(s -> typeContains == null || typeContains.isBlank()
                        || s.getType().toLowerCase().contains(typeContains.toLowerCase()))
                .filter(s -> nameContains == null || nameContains.isBlank()
                        || s.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .filter(s -> createdFrom == null
                        || s.getCreatedAt() == null
                        || !s.getCreatedAt().isBefore(createdFrom))
                .filter(s -> createdTo == null
                        || s.getCreatedAt() == null
                        || !s.getCreatedAt().isAfter(createdTo))
                .sorted(Comparator.comparing(Sample::getId))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Статистика по образцам (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Возвращает статистику по образцам сгруппированную по владельцу:
     *   login -> количество активных образцов
     */
    public Map<String, Long> countByOwner() {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.groupingBy(Sample::getOwnerUsername, Collectors.counting()));
    }

    /**
     * Возвращает статистику по типам образцов:
     *   type -> количество
     */
    public Map<String, Long> countByType() {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.groupingBy(Sample::getType, Collectors.counting()));
    }

    /**
     * Возвращает статистику по статусам образцов:
     *   status -> количество
     */
    public Map<SampleStatus, Long> countByStatus() {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.groupingBy(Sample::getStatus, Collectors.counting()));
    }

    /**
     * Общая сводка: всего, активных, архивных, удалённых.
     */
    public Map<String, Long> getSummary() {
        long total   = storage.values().stream().filter(s -> !s.isDeleted()).count();
        long active  = storage.values().stream()
                .filter(s -> !s.isDeleted() && s.getStatus() == SampleStatus.ACTIVE).count();
        long archived = storage.values().stream()
                .filter(s -> !s.isDeleted() && s.getStatus() == SampleStatus.ARCHIVED).count();
        long deleted = storage.values().stream().filter(Sample::isDeleted).count();

        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Всего образцов", total);
        map.put("Активных", active);
        map.put("Архивных", archived);
        map.put("Удалённых (soft)", deleted);
        return map;
    }

    // -------------------------------------------------------------------------
    // Изменение
    // -------------------------------------------------------------------------

    public Sample update(long id, String field, String value) {
        Sample sample = getById(id);
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        switch (field) {
            case "name":
                SampleValidator.validateName(value);
                sample.setName(value);
                break;
            case "type":
                SampleValidator.validateType(value);
                sample.setType(value);
                break;
            case "location":
                SampleValidator.validateLocation(value);
                sample.setLocation(value);
                break;
            case "status":
                SampleValidator.validateStatus(value);
                sample.setStatus(SampleStatus.valueOf(value));
                break;
            default:
                throw new IllegalArgumentException("нельзя менять поле '" + field + "'");
        }

        repository.update(sample);
        storage.put(sample.getId(), sample);

        System.out.println("[DB] Обновлён образец #" + id);
        return sample;
    }

    public Sample archive(long id) {
        Sample sample = getById(id);
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        if (sample.getStatus() == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("образец уже ARCHIVED");
        }
        sample.setStatus(SampleStatus.ARCHIVED);

        repository.update(sample);
        storage.put(sample.getId(), sample);

        System.out.println("[DB] Архивирован образец #" + id);
        return sample;
    }

    // -------------------------------------------------------------------------
    // Удаление / Восстановление
    // -------------------------------------------------------------------------

    public void delete(long id) {
        Sample sample = getById(id);
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        sample.setDeleted(true);
        repository.update(sample);
        storage.put(id, sample);

        System.out.println("[DB] Удалён (soft delete) образец #" + id);
    }

    public void restore(long id) {
        Sample sample = storage.get(id);
        if (sample == null || !sample.isDeleted()) {
            throw new IllegalArgumentException("образец с id=" + id + " не найден или не удалён");
        }
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        sample.setDeleted(false);
        repository.update(sample);
        storage.put(id, sample);

        System.out.println("[DB] Восстановлен образец #" + id);
    }

    // -------------------------------------------------------------------------
    // Для совместимости с UI
    // -------------------------------------------------------------------------

    public Map<Long, Sample> getStorage() {
        return storage;
    }
}
