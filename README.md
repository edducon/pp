# Event Management Desktop App

Полностью обновлённое настольное приложение для управления мероприятиями в сфере информационной безопасности. Клиент написан на
JavaFX 21 и обращается к базе данных MySQL 8 через пул соединений HikariCP. Из коробки доступен сценарий для демонстрационного
наполнения БД.

## Стек

- Java 21
- JavaFX 21 (чистый UI без смешения со Swing)
- MySQL 8
- Maven 3.9+
- HikariCP

## Предварительные требования

1. Установите JDK 21 и убедитесь, что `JAVA_HOME` указывает на него.
2. Установите сервер MySQL 8 и создайте пользователя/БД для приложения.
3. Установите Apache Maven 3.9 или новее.

## Подготовка базы данных MySQL

1. Создайте схему и пользователя:

   ```sql
   CREATE DATABASE event_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'event_user'@'%' IDENTIFIED BY 'event_password';
   GRANT ALL PRIVILEGES ON event_app.* TO 'event_user'@'%';
   FLUSH PRIVILEGES;
   ```

2. Обновлённый SQL-скрипт с таблицами и демонстрационными данными находится в `app/src/main/resources/db/schema.sql`. Его можно
   выполнить одной командой:

   ```bash
   mysql -u event_user -p event_app < app/src/main/resources/db/schema.sql
   ```

   Скрипт создаёт все справочники (города, направления), пользователей с различными ролями, пару примерных мероприятий, задачи,
   а также заявки на участие и модерацию.

## Конфигурация приложения

Параметры подключения находятся в `app/src/main/resources/application.properties`:

```properties
app.datasource.jdbcUrl=jdbc:mysql://localhost:3306/event_app?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
app.datasource.username=event_user
app.datasource.password=event_password
app.datasource.maximumPoolSize=10
app.datasource.driverClassName=com.mysql.cj.jdbc.Driver
```

При необходимости обновите хост/порт и учётные данные. Пул соединений настроен на 10 подключений и автоматически закрывается при
завершении работы клиента.

## Сборка

```bash
cd app
mvn clean package
```

Команда формирует исполняемый архив `target/eventapp-1.0.0-jar-with-dependencies.jar` с зависимостями JavaFX.

## Запуск

### Через Maven

```bash
cd app
mvn javafx:run
```

### Через собранный JAR

Для запуска из JAR понадобится JDK 21 с установленными модулями JavaFX. Пример для Linux/macOS (переменная `PATH_TO_FX` должна
указывать на каталог `lib` JavaFX 21):

```bash
java \
  --module-path "$PATH_TO_FX" \
  --add-modules javafx.controls,javafx.graphics,javafx.base \
  -jar target/eventapp-1.0.0-jar-with-dependencies.jar
```

## Что нового по сравнению с предыдущей версией

- Полностью переписанная архитектура: JavaFX вместо смешения с Swing, чёткое разделение на слои конфигурации, репозиториев и
  сервисов.
- Актуальная схема MySQL с учётом ролей, заявок на модерацию, регистраций участников и канбан-доски задач.
- Приветственный экран с регистрацией в одну вкладку для всех ролей (организатор, участник, модератор, член жюри).
- Панель управления с вкладками в зависимости от роли: каталог мероприятий, управление событиями и задачами организатора,
  обработка заявок модератора, справочник участников.
- Поддержка быстрой инициализации базы данных через встроенный SQL-скрипт (запускается автоматически при первом старте клиента).

## Примечания

- Для хэширования паролей используется SHA-256; значения в демо-данных соответствуют паролю `password`.
- Автоматические тесты не предусмотрены в рамках задания.
