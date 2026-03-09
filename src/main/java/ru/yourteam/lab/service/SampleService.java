package ru.yourteam.lab.service;

import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.validator.SampleValidator;

import java.util.*;

public class SampleService {
    private final Map<Long, Sample> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Sample add(String name, String type, String location) {
        // Валидация
        SampleValidator.validateName(name);
        SampleValidator.validateType(type);
        SampleValidator.validateLocation(location);

        // Создание
        Sample sample = new Sample(name, type, location);
        sample.setId(nextId++);

        // Сохранение
        storage.put(sample.getId(), sample);
        return sample;
    }

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

    public Sample update(long id, String field, String value) {
        Sample sample = getById(id);

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
        if (sample.getStatus() == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("образец уже ARCHIVED");
        }
        sample.setStatus(SampleStatus.ARCHIVED);
        return sample;
    }

    public List<Sample> filterByStatus(SampleStatus status) {
        List<Sample> result = new ArrayList<>();
        for (Sample s : storage.values()) {
            if (s.getStatus() == status) {
                result.add(s);
            }
        }
        return result;
    }

    public List<Sample> filterByOwner(String username) {
        List<Sample> result = new ArrayList<>();
        for (Sample s : storage.values()) {
            if (s.getOwnerUsername().equals(username)) {
                result.add(s);
            }
        }
        return result;
    }
}
