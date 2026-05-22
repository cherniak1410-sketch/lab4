package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.validator.SampleValidator;

import java.util.*;

/**
 * Сервис управления образцами.
 *
 * Этап 5: при создании объекта владелец берётся из AuthService (текущий пользователь).
 * Изменение и архивирование требуют авторизации и прав на объект.
 * SYSTEM-объекты может редактировать любой авторизованный пользователь.
 */
public class SampleService {

    private final Map<Long, Sample> storage = new LinkedHashMap<>();
    private long nextId = 1;
    private final AuthService authService;

    public SampleService(AuthService authService) {
        this.authService = authService;
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
        sample.setId(nextId++);
        // Владелец = текущий авторизованный пользователь
        sample.setOwnerUsername(authService.getCurrentUsername());

        storage.put(sample.getId(), sample);
        return sample;
    }

    // -------------------------------------------------------------------------
    // Чтение (доступно всем)
    // -------------------------------------------------------------------------

    public Optional<Sample> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Sample getById(long id) {
        Sample sample = storage.get(id);
        if (sample == null) {
            throw new IllegalArgumentException("образец с id=" + id + " не найден");
        }
        return sample;
    }

    public List<Sample> getAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Sample> filterByStatus(SampleStatus status) {
        List<Sample> result = new ArrayList<>();
        for (Sample s : storage.values()) {
            if (s.getStatus() == status) result.add(s);
        }
        return result;
    }

    public List<Sample> filterByOwner(String username) {
        List<Sample> result = new ArrayList<>();
        for (Sample s : storage.values()) {
            if (s.getOwnerUsername().equals(username)) result.add(s);
        }
        return result;
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
        return sample;
    }

    public Sample archive(long id) {
        Sample sample = getById(id);
        authService.requireOwnership(sample.getOwnerUsername(), "Sample#" + id);

        if (sample.getStatus() == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("образец уже ARCHIVED");
        }
        sample.setStatus(SampleStatus.ARCHIVED);
        return sample;
    }

    // -------------------------------------------------------------------------
    // Внутренний доступ (для загрузки файлов)
    // -------------------------------------------------------------------------

    public Map<Long, Sample> getStorage() { return storage; }

    public void setNextId(long nextId) { this.nextId = nextId; }

    public long getNextId() { return nextId; }
    public void delete(long id) {
        if (!storage.containsKey(id)) {
            throw new IllegalArgumentException("Образец с id=" + id + " не найден");
        }
        storage.remove(id);
    }
}
