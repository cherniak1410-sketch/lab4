package ru.yourteam.lab.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.yourteam.lab.domain.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileStorage {
    private final ObjectMapper objectMapper;

    public FileStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Сохранить все данные
    public void save(String filePath,
                     Map<Long, Sample> samples,
                     Map<Long, Measurement> measurements,
                     Map<Long, Protocol> protocols) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("samples", new ArrayList<>(samples.values()));
        data.put("measurements", new ArrayList<>(measurements.values()));
        data.put("protocols", new ArrayList<>(protocols.values()));
        objectMapper.writeValue(new File(filePath), data);
    }

    // Загрузить все данные (пока заглушка, потом доделаем)
    public Map<String, Object> load(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Файл не найден: " + filePath);
        }

        Map<String, Object> data = objectMapper.readValue(file, Map.class);
        Map<String, Object> result = new HashMap<>();

        // Восстанавливаем образцы
        List<Sample> samples = new ArrayList<>();
        List<Map<String, Object>> samplesData = (List<Map<String, Object>>) data.get("samples");
        if (samplesData != null) {
            for (Map<String, Object> sampleMap : samplesData) {
                samples.add(restoreSample(sampleMap));
            }
        }
        result.put("samples", samples);

        // Восстанавливаем измерения
        List<Measurement> measurements = new ArrayList<>();
        List<Map<String, Object>> measurementsData = (List<Map<String, Object>>) data.get("measurements");
        if (measurementsData != null) {
            for (Map<String, Object> measurementMap : measurementsData) {
                measurements.add(restoreMeasurement(measurementMap));
            }
        }
        result.put("measurements", measurements);

        // Восстанавливаем протоколы
        List<Protocol> protocols = new ArrayList<>();
        List<Map<String, Object>> protocolsData = (List<Map<String, Object>>) data.get("protocols");
        if (protocolsData != null) {
            for (Map<String, Object> protocolMap : protocolsData) {
                protocols.add(restoreProtocol(protocolMap));
            }
        }
        result.put("protocols", protocols);

        return result;
    }
    private Sample restoreSample(Map<String, Object> map) {
        Sample sample = new Sample(
                (String) map.get("name"),
                (String) map.get("type"),
                (String) map.get("location")
        );
        sample.setId(((Number) map.get("id")).longValue());
        sample.setStatus(SampleStatus.valueOf((String) map.get("status")));
        sample.setOwnerUsername((String) map.get("ownerUsername"));
        return sample;
    }

    private Measurement restoreMeasurement(Map<String, Object> map) {
        Measurement measurement = new Measurement(
                ((Number) map.get("sampleId")).longValue(),
                MeasurementParam.valueOf((String) map.get("param")),
                ((Number) map.get("value")).doubleValue(),
                (String) map.get("unit"),
                (String) map.get("method")
        );
        measurement.setId(((Number) map.get("id")).longValue());
        measurement.setOwnerUsername((String) map.get("ownerUsername"));
        return measurement;
    }

    private Protocol restoreProtocol(Map<String, Object> map) {
        List<String> paramsList = (List<String>) map.get("requiredParams");
        Set<MeasurementParam> requiredParams = new HashSet<>();
        if (paramsList != null) {
            for (String param : paramsList) {
                requiredParams.add(MeasurementParam.valueOf(param));
            }
        }

        Protocol protocol = new Protocol(
                (String) map.get("name"),
                requiredParams
        );
        protocol.setId(((Number) map.get("id")).longValue());
        protocol.setOwnerUsername((String) map.get("ownerUsername"));
        return protocol;
    }
}
