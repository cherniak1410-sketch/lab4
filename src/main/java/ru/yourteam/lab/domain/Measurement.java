package ru.yourteam.lab.domain;

import java.time.Instant;

public final class Measurement {
    // Уникальный номер измерения. Программа назначает сама.
    public long id;

    // К какому образцу относится измерение (id образца).
    // Должен ссылаться на реально существующий Sample.
    public long sampleId;

    // Что измеряли (PH, CONDUCTIVITY...). Выбирается из списка MeasurementParam.
    public MeasurementParam param;

    // Значение измерения. Должно быть обычным числом (не текст, не NaN/Infinity).
    public double value;

    // Единицы измерения (например: "pH", "mS/cm", "NTU", "mg/L").
    // Нельзя пустое. Желательно до 16 символов.
    public String unit;

    // Метод измерения (например: "electrode", "sensor", "spectro").
    // Нельзя пустое. Желательно до 64 символов.
    public String method;

    // Время, когда измерили. Если пользователь не вводит — ставится текущее время.
    public Instant measuredAt;

    // Кто добавил измерение (логин пользователя). На ранних этапах можно "SYSTEM".
    // Не должен быть пустым.
    public String ownerUsername;

    // Когда запись создана. Программа ставит автоматически.
    public Instant createdAt;

    // Когда запись правили (если разрешаете update значения/метода/единиц).
    public Instant updatedAt;
}
