# Event Management Desktop App

Настольное приложение для организаторов конференций по информационной безопасности. Клиент реализован на JavaFX 21 и использует Java Swing для экранов каталога и рабочих панелей. Хранилище данных — MySQL 8, подключение выполняется через пул HikariCP.

## Стек

- Java 21
- JavaFX 21 (графическая оболочка + интеграция с имеющимися Swing-экранами)
- Java Swing (рабочие панели и диалоги)
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

2. Выполните миграционный скрипт с таблицами и демонстрационными данными:

   ```bash
   mysql -u event_user -p event_app < database/schema.sql
   ```

## Конфигурация приложения

Основные параметры соединения находятся в `app/src/main/resources/application.properties`:

```properties
app.datasource.jdbcUrl=jdbc:mysql://localhost:3306/event_app?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
app.datasource.username=event_user
app.datasource.password=event_password
app.datasource.maximumPoolSize=10
app.datasource.driverClassName=com.mysql.cj.jdbc.Driver
```

Обновите значения под вашу среду (хост, порт, имя пользователя, пароль). При необходимости настройте параметры пула соединений HikariCP.

## Сборка

Соберите модуль `app` с зависимостями JavaFX и MySQL:

```bash
cd app
mvn clean package
```

Команда сформирует исполняемый архив `target/eventapp-1.0.0-jar-with-dependencies.jar`.

## Запуск

### Через Maven

Для корректной инициализации JavaFX 21 используйте Maven-плагин:

```bash
cd app
mvn javafx:run
```

### Через готовый архив

Для запуска из собранного JAR понадобится JDK 21 с доступными JavaFX модулями в classpath. Пример для Linux/macOS (предполагается, что переменная `PATH_TO_FX` указывает на каталог `lib` официальной JavaFX 21):

```bash
java \
  --module-path "$PATH_TO_FX" \
  --add-modules javafx.controls,javafx.graphics,javafx.swing,javafx.base \
  -jar target/eventapp-1.0.0-jar-with-dependencies.jar
```

При запуске появляется приветственное окно JavaFX, оформленное в соответствии с руководством по стилю (фон и шрифты Comic Sans MS). Нажмите «Открыть рабочий стол», чтобы открыть рабочие формы Swing. Закрытие всех окон завершается корректным отключением от MySQL.

## Файлы ресурсов

- `database/schema.sql` — SQL-скрипт для MySQL.
- Логотип компании не поставляется в репозитории: при необходимости создайте каталог `app/src/main/resources/images` и добавьте собственный файл `logo.png` для отображения фирменной графики.

## Примечания

- В справочниках используются демонстрационные изображения (пути задаются вручную).
- Автоматические тесты не предусмотрены заданием.
