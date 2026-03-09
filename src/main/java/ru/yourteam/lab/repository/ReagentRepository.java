package ru.yourteam.lab.repository;

import ru.yourteam.lab.domain.Reagent;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ReagentRepository {
    private final Map<Long, Reagent> storage = new LinkedHashMap<>();
    private long nextId = 1;

    public Reagent save(Reagent reagent) {
        if (reagent.id == 0) {
            reagent.id = nextId++;
        }
        reagent.updatedAt = Instant.now();
        storage.put(reagent.id, reagent);
        return reagent;
    }

    public Optional<Reagent> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Reagent> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Reagent> search(String query) {
        if (query == null || query.isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.toLowerCase();
        return storage.values().stream()
                .filter(r -> (r.name != null && r.name.toLowerCase().contains(lowerQuery)) ||
                        (r.formula != null && r.formula.toLowerCase().contains(lowerQuery)) ||
                        (r.cas != null && r.cas.toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
    }
}