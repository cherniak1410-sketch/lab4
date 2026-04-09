package ru.yourteam.lab.service;

import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.validator.ProtocolValidator;

import java.util.*;

public class ProtocolService {
    private final Map<Long, Protocol> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Protocol add(String name, Set<MeasurementParam> requiredParams) {
        // Валидация
        ProtocolValidator.validateName(name);
        ProtocolValidator.validateRequiredParams(requiredParams);

        // Создание
        Protocol protocol = new Protocol(name, requiredParams);
        protocol.setId(nextId++);

        // Сохранение
        storage.put(protocol.getId(), protocol);
        return protocol;
    }

    public Optional<Protocol> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Protocol getById(long id) {
        Protocol protocol = storage.get(id);
        if (protocol == null) {
            throw new IllegalArgumentException("протокол с id=" + id + " не найден");
        }
        return protocol;
    }

    public List<Protocol> getAll() {
        return new ArrayList<>(storage.values());
    }

    public Set<MeasurementParam> checkCompliance(long protocolId, Set<MeasurementParam> measuredParams) {
        Protocol protocol = getById(protocolId);

        Set<MeasurementParam> missing = new HashSet<>(protocol.getRequiredParams());
        missing.removeAll(measuredParams);

        return missing;
    }
    public Map<Long, Protocol> getStorage() {
        return storage;
    }
    public void setNextId(long nextId) {
        this.nextId = nextId;
    }
}

