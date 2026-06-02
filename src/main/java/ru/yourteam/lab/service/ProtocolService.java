package ru.yourteam.lab.service;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.repository.ProtocolRepository;
import ru.yourteam.lab.validator.ProtocolValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис управления протоколами.
 *
 * Этап 7: добавлена расширенная фильтрация и статистика.
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

        repository.insert(protocol);
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
    // Расширенная фильтрация (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Универсальный фильтр протоколов. Все параметры необязательны (null = не фильтровать).
     *
     * @param owner         фильтр по владельцу (точное, без учёта регистра)
     * @param nameContains  подстрока в названии
     * @param hasParams     протокол должен содержать ВСЕ из указанных параметров
     * @param hasAnyParam   протокол должен содержать ХОТЯ БЫ ОДИН из указанных параметров
     */
    public List<Protocol> filter(String owner,
                                 String nameContains,
                                 Set<MeasurementParam> hasParams,
                                 Set<MeasurementParam> hasAnyParam) {
        return storage.values().stream()
                .filter(p -> owner == null || owner.isBlank()
                        || p.getOwnerUsername().equalsIgnoreCase(owner))
                .filter(p -> nameContains == null || nameContains.isBlank()
                        || p.getName().toLowerCase().contains(nameContains.toLowerCase()))
                .filter(p -> hasParams == null || hasParams.isEmpty()
                        || p.getRequiredParams().containsAll(hasParams))
                .filter(p -> hasAnyParam == null || hasAnyParam.isEmpty()
                        || hasAnyParam.stream().anyMatch(param -> p.getRequiredParams().contains(param)))
                .sorted(Comparator.comparing(Protocol::getId))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Статистика по протоколам (этап 7)
    // -------------------------------------------------------------------------

    /**
     * Количество протоколов по владельцу.
     */
    public Map<String, Long> countByOwner() {
        return storage.values().stream()
                .collect(Collectors.groupingBy(Protocol::getOwnerUsername, Collectors.counting()));
    }

    /**
     * Сколько протоколов требуют каждый из параметров.
     */
    public Map<MeasurementParam, Long> countByParam() {
        Map<MeasurementParam, Long> result = new LinkedHashMap<>();
        for (MeasurementParam p : MeasurementParam.values()) {
            long cnt = storage.values().stream()
                    .filter(proto -> proto.getRequiredParams().contains(p))
                    .count();
            result.put(p, cnt);
        }
        return result;
    }

    /**
     * Общая сводка по протоколам.
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("Всего протоколов", (long) storage.size());

        // Среднее количество параметров в протоколе
        OptionalDouble avg = storage.values().stream()
                .mapToInt(p -> p.getRequiredParams().size())
                .average();
        map.put("Ср. параметров в протоколе", avg.isPresent()
                ? String.format("%.1f", avg.getAsDouble()) : "—");

        return map;
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

        repository.delete(id);
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
