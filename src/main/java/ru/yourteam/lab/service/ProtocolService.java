package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.validator.ProtocolValidator;

import java.util.*;

/**
 * Сервис управления протоколами.
 *
 * Этап 5: создание требует авторизации; владелец = текущий пользователь.
 * SYSTEM-протоколы может редактировать любой авторизованный пользователь.
 */
public class ProtocolService {

    private final Map<Long, Protocol> storage = new LinkedHashMap<>();
    private long nextId = 1;
    private final AuthService authService;

    public ProtocolService(AuthService authService) {
        this.authService = authService;
    }

    // -------------------------------------------------------------------------
    // Создание
    // -------------------------------------------------------------------------

    public Protocol add(String name, Set<MeasurementParam> requiredParams) {
        authService.requireLogin();

        ProtocolValidator.validateName(name);
        ProtocolValidator.validateRequiredParams(requiredParams);

        Protocol protocol = new Protocol(name, requiredParams);
        protocol.setId(nextId++);
        protocol.setOwnerUsername(authService.getCurrentUsername());

        storage.put(protocol.getId(), protocol);
        return protocol;
    }

    // -------------------------------------------------------------------------
    // Чтение (доступно всем)
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Внутренний доступ
    // -------------------------------------------------------------------------

    public Map<Long, Protocol> getStorage() { return storage; }

    public void setNextId(long nextId) { this.nextId = nextId; }

    public long getNextId() { return nextId; }
    public void delete(long id) {
        if (!storage.containsKey(id)) {
            throw new IllegalArgumentException("Протокол с id=" + id + " не найден");
        }
        storage.remove(id);
    }
}
