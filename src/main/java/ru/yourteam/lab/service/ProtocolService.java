package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.repository.ProtocolRepository;
import ru.yourteam.lab.validator.ProtocolValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис управления протоколами.
 *
 * Этап 6: данные хранятся синхронно в памяти (Map) и в PostgreSQL.
 * При старте загружаются из БД в память.
 */
public class ProtocolService {

    private final Map<Long, Protocol> storage = new ConcurrentHashMap<>();
    private final ProtocolRepository repository;
    private final AuthService authService;

    public ProtocolService(AuthService authService, ProtocolRepository repository) {
        this.authService = authService;
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Загрузка из БД при старте
    // -------------------------------------------------------------------------

    public void loadFromDatabase() {
        List<Protocol> all = repository.findAll();
        storage.clear();
        for (Protocol protocol : all) {
            storage.put(protocol.getId(), protocol);
        }
        System.out.println("[DB] Загружено " + all.size() + " протоколов из PostgreSQL");
    }

    // -------------------------------------------------------------------------
    // Создание
    // -------------------------------------------------------------------------

    public Protocol add(String name, Set<MeasurementParam> requiredParams) {
        authService.requireLogin();

        ProtocolValidator.validateName(name);
        ProtocolValidator.validateRequiredParams(requiredParams);

        Protocol protocol = new Protocol(name, requiredParams);
        protocol.setOwnerUsername(authService.getCurrentUsername());

        // 1. Сохраняем в БД (получаем ID)
        repository.insert(protocol);

        // 2. Сохраняем в память
        storage.put(protocol.getId(), protocol);

        System.out.println("[DB] Создан протокол #" + protocol.getId());
        return protocol;
    }

    // -------------------------------------------------------------------------
    // Чтение (только из памяти)
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
    // Удаление
    // -------------------------------------------------------------------------

    public void delete(long id) {
        if (!storage.containsKey(id)) {
            throw new IllegalArgumentException("Протокол с id=" + id + " не найден");
        }

        Protocol protocol = storage.get(id);
        authService.requireOwnership(protocol.getOwnerUsername(), "Protocol#" + id);

        // 1. Удаляем из БД
        repository.delete(id);

        // 2. Удаляем из памяти
        storage.remove(id);

        System.out.println("[DB] Удалён протокол #" + id);
    }

    // -------------------------------------------------------------------------
    // Для совместимости с UI
    // -------------------------------------------------------------------------

    public Map<Long, Protocol> getStorage() {
        return storage;
    }
}