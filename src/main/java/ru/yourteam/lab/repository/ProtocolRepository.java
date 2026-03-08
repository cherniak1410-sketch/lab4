package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.Protocol;

import java.time.Instant;
import java.util.*;

public class ProtocolRepository {
    private final Map<Long, Protocol> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Protocol save(Protocol protocol) {
        if (protocol.id == 0) {
            protocol.id = nextId++;
        }
        protocol.updatedAt = Instant.now();
        storage.put(protocol.id, protocol);
        return protocol;
    }

    public Optional<Protocol> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Protocol> findAll() {
        return new ArrayList<>(storage.values());
    }
}
