# Лабораторная работа: Управление лабораторными образцами и реактивами
## Описание проекта
Консольное приложение для автоматизации учёта в лаборатории.
Позволяет управлять образцами, измерениями, протоколами, реактивами и складскими движениями.
## Требования
- Java 17 или выше
- Maven 3.6+

## Сборка
```bash
mvn clean package
```
## Запуск
```bash
mvn exec:java
```
## Авторы
Бузинова Мария и Александра Черняк
## Функциональность

### 1. Образцы (Samples)
- `sample_add` - создание нового образца
- `sample_list [--status ACTIVE|ARCHIVED] [--mine]` - список образцов
- `sample_show <id>` - детальная информация об образце
- `sample_update <id> field=value ...` - обновление полей образца
- `sample_archive <id>` - архивация образца

### 2. Измерения (Measurements)
- `meas_add <sample_id>` - добавление измерения к образцу
- `meas_list <sample_id> [--param PARAM] [--last N]` - список измерений
- `meas_stats <sample_id> <param>` - статистика по параметру

### 3. Протоколы (Protocols)
- `prot_create` - создание протокола измерений
- `prot_apply <protocol_id> <sample_id>` - проверка выполнения протокола
### 4. Реактивы (Reagents)
- `reag_add` - создание нового реактива
- `reag_list [--q TEXT]` - поиск реактивов
- `reag_show <id>` - информация о реактиве
- `reag_update <id> field=value ...` - обновление реактива

### 5. Партии (Batches)
- `batch_add <reagent_id>` - добавление партии реактива
- `batch_list <reagent_id> [--active]` - список партий
- `batch_show <batch_id>` - информация о партии
- `batch_update <id> field=value ...` - обновление партии

### 6. Складские движения (Stock Moves)
- `move_add <batch_id>` - добавление движения (IN/OUT/DISCARD)
- `move_list <batch_id>` - история движений партии
