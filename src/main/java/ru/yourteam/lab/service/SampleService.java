package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.repository.SampleRepository;
import ru.yourteam.lab.validator.SampleValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис управления образцами.
 *
 * Этап 6: данные хранятся синхронно в памяти (Map) и в PostgreSQL.
 * При старте загружаются из БД в память.
 * Каждая операция add/update/delete меняет и БД, и память.
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

        // 1. Сохраняем в БД (получаем ID)
        repository.insert(sample);

        // 2. Сохраняем в память
        storage.put(sample.getId(), sample);

        System.out.println("[DB] Создан образец #" + sample.getId());
        return sample;
    }

    // -------------------------------------------------------------------------
    // Чтение (только из памяти, в БД не ходим)
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
    public List<Sample> filterSamples(
            String type,
            String location,
            String owner
    ) {
        return storage.values().stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> type == null || type.isBlank()
                        || s.getType().equalsIgnoreCase(type))
                .filter(s -> location == null || location.isBlank()
                        || s.getLocation().equalsIgnoreCase(location))
                .filter(s -> owner == null || owner.isBlank()
                        || s.getOwnerUsername().equalsIgnoreCase(owner))
                .toList();
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

        // 1. Обновляем в БД
        repository.update(sample);

        // 2. Обновляем в памяти
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

        // 1. Обновляем в БД
        repository.update(sample);

        // 2. Обновляем в памяти
        storage.put(sample.getId(), sample);

        System.out.println("[DB] Архивирован образец #" + id);
        return sample;
    }

    // -------------------------------------------------------------------------
    // Удаление / Восстановление (мягкое удаление)
    // -------------------------------------------------------------------------

    public void delete(long id) {
        Sample sample = getById(id);
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        // 1. Мягкое удаление в БД
        sample.setDeleted(true);
        repository.update(sample);

        // 2. Обновляем в памяти
        storage.put(id, sample);

        System.out.println("[DB] Удалён (soft delete) образец #" + id);
    }

    public void restore(long id) {
        Sample sample = storage.get(id);
        if (sample == null || !sample.isDeleted()) {
            throw new IllegalArgumentException("образец с id=" + id + " не найден или не удалён");
        }
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        // 1. Восстанавливаем в БД
        sample.setDeleted(false);
        repository.update(sample);

        // 2. Обновляем в памяти
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