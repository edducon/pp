DROP TABLE IF EXISTS activity_votes;
DROP TABLE IF EXISTS moderation_requests;
DROP TABLE IF EXISTS activity_resources;
DROP TABLE IF EXISTS activity_tasks;
DROP TABLE IF EXISTS activity_participants;
DROP TABLE IF EXISTS event_activity_jury;
DROP TABLE IF EXISTS event_activities;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS jury_members;
DROP TABLE IF EXISTS moderators;
DROP TABLE IF EXISTS participants;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS directions;
DROP TABLE IF EXISTS cities;
DROP TABLE IF EXISTS countries;

CREATE TABLE countries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name_ru VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    code VARCHAR(8) NOT NULL UNIQUE,
    code_numeric INT NOT NULL UNIQUE
);

CREATE TABLE cities (
    id INT PRIMARY KEY,
    name_ru VARCHAR(255) NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    FOREIGN KEY (country_code) REFERENCES countries(code)
);

CREATE TABLE directions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE organizers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(32) NOT NULL UNIQUE,
    last_name VARCHAR(128) NOT NULL,
    first_name VARCHAR(128) NOT NULL,
    middle_name VARCHAR(128),
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    city_id INT NOT NULL,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    photo_path VARCHAR(255) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    FOREIGN KEY (country_code) REFERENCES countries(code),
    FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(32) NOT NULL UNIQUE,
    last_name VARCHAR(128) NOT NULL,
    first_name VARCHAR(128) NOT NULL,
    middle_name VARCHAR(128),
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    city_id INT NOT NULL,
    phone VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    photo_path VARCHAR(255) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    FOREIGN KEY (country_code) REFERENCES countries(code),
    FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE moderators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(32) NOT NULL UNIQUE,
    last_name VARCHAR(128) NOT NULL,
    first_name VARCHAR(128) NOT NULL,
    middle_name VARCHAR(128),
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    city_id INT NOT NULL,
    phone VARCHAR(32) NOT NULL,
    direction_id BIGINT,
    password_hash VARCHAR(128) NOT NULL,
    photo_path VARCHAR(255) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    FOREIGN KEY (country_code) REFERENCES countries(code),
    FOREIGN KEY (city_id) REFERENCES cities(id),
    FOREIGN KEY (direction_id) REFERENCES directions(id)
);

CREATE TABLE jury_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(32) NOT NULL UNIQUE,
    last_name VARCHAR(128) NOT NULL,
    first_name VARCHAR(128) NOT NULL,
    middle_name VARCHAR(128),
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    city_id INT NOT NULL,
    phone VARCHAR(32) NOT NULL,
    direction_id BIGINT,
    password_hash VARCHAR(128) NOT NULL,
    photo_path VARCHAR(255) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    FOREIGN KEY (country_code) REFERENCES countries(code),
    FOREIGN KEY (city_id) REFERENCES cities(id),
    FOREIGN KEY (direction_id) REFERENCES directions(id)
);

CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organizer_id BIGINT NOT NULL,
    title VARCHAR(512) NOT NULL,
    direction_id BIGINT NOT NULL,
    description TEXT,
    logo_path VARCHAR(255),
    city_id INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    FOREIGN KEY (organizer_id) REFERENCES organizers(id),
    FOREIGN KEY (direction_id) REFERENCES directions(id),
    FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE event_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id)
);

CREATE TABLE event_activity_jury (
    activity_id BIGINT NOT NULL,
    jury_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, jury_id),
    FOREIGN KEY (activity_id) REFERENCES event_activities(id),
    FOREIGN KEY (jury_id) REFERENCES jury_members(id)
);

CREATE TABLE activity_participants (
    activity_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, participant_id),
    FOREIGN KEY (activity_id) REFERENCES event_activities(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);

CREATE TABLE activity_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    participant_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES event_activities(id),
    FOREIGN KEY (participant_id) REFERENCES participants(id)
);

CREATE TABLE activity_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    resource_path VARCHAR(255) NOT NULL,
    uploaded_by_moderator BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES event_activities(id),
    FOREIGN KEY (uploaded_by_moderator) REFERENCES moderators(id)
);

CREATE TABLE moderation_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    moderator_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    conflict_activity_id BIGINT,
    response_message VARCHAR(512),
    decline_reason VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES event_activities(id),
    FOREIGN KEY (moderator_id) REFERENCES moderators(id),
    FOREIGN KEY (conflict_activity_id) REFERENCES event_activities(id)
);

CREATE TABLE activity_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    winner_name VARCHAR(255) NOT NULL,
    jury_slot INT NOT NULL,
    jury_member_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (activity_id) REFERENCES event_activities(id)
);

INSERT INTO countries (name_ru, name_en, code, code_numeric) VALUES
    ('Россия', 'Russia', 'RU', 643),
    ('Соединенные Штаты', 'United States', 'US', 840),
    ('Франция', 'France', 'FR', 250);

INSERT INTO cities (id, name_ru, country_code) VALUES
    (1, 'Москва', 'RU'),
    (2, 'Санкт-Петербург', 'RU'),
    (3, 'Казань', 'RU'),
    (4, 'Новосибирск', 'RU'),
    (5, 'Нью-Йорк', 'US'),
    (6, 'Париж', 'FR'),
    (7, 'Сочи', 'RU');

INSERT INTO directions (name) VALUES
    ('Технологии'),
    ('Искусство'),
    ('Образование');

INSERT INTO organizers (id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, password_hash, photo_path, gender) VALUES
    ('ORG-0001', 'Иванов', 'Иван', 'Иванович', 'ivanov@example.com', '1985-05-12', 'RU', 1, '+7-900-000-00-00', 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 'images/organizer1.png', 'мужской');

INSERT INTO participants (id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, password_hash, photo_path, gender) VALUES
    ('PAR-0001', 'Петров', 'Петр', 'Сергеевич', 'petrov@example.com', '1995-03-22', 'RU', 2, '+7-901-000-00-00', 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 'images/participant1.png', 'мужской');

INSERT INTO moderators (id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, direction_id, password_hash, photo_path, gender) VALUES
    ('MOD-0001', 'Сидорова', 'Анна', 'Алексеевна', 'sidorova@example.com', '1990-07-15', 'RU', 3, '+7-902-000-00-00', 1, 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 'images/moderator1.png', 'женский');

INSERT INTO jury_members (id_number, last_name, first_name, middle_name, email, birth_date, country_code, city_id, phone, direction_id, password_hash, photo_path, gender) VALUES
    ('JURY-0001', 'Морозов', 'Дмитрий', 'Игоревич', 'morozov@example.com', '1982-11-03', 'RU', 4, '+7-903-000-00-00', 2, 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 'images/jury1.png', 'мужской'),
    ('JURY-0002', 'Лебедева', 'Мария', 'Павловна', 'lebedeva@example.com', '1988-02-19', 'RU', 1, '+7-904-000-00-00', 3, 'a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea', 'images/jury2.png', 'женский');

INSERT INTO events (organizer_id, title, direction_id, description, logo_path, city_id, start_time, end_time) VALUES
    (1, 'Tech Expo 2024', 1, 'Трехдневная выставка инновационных технологий.', 'images/tech-expo.png', 1, '2024-06-01 09:00:00', '2024-06-03 18:00:00'),
    (1, 'Art Connect 2024', 2, 'Форум для художников и кураторов из разных городов.', 'images/art-connect.png', 2, '2024-07-10 10:00:00', '2024-07-12 17:00:00');

INSERT INTO event_activities (event_id, title, description, start_time, end_time) VALUES
    (1, 'Открытие выставки', 'Презентация ключевых трендов индустрии.', '2024-06-01 10:00:00', '2024-06-01 11:30:00'),
    (1, 'Демо-день стартапов', 'Выступления команд и отбор лучших проектов.', '2024-06-02 12:00:00', '2024-06-02 13:30:00'),
    (2, 'Пленэр на Неве', 'Практическая сессия на открытом воздухе.', '2024-07-11 11:00:00', '2024-07-11 12:30:00');

INSERT INTO event_activity_jury (activity_id, jury_id) VALUES
    (1, 1),
    (2, 1),
    (3, 2);

INSERT INTO activity_participants (activity_id, participant_id) VALUES
    (1, 1),
    (2, 1);

INSERT INTO activity_tasks (activity_id, title, description, participant_id, created_at) VALUES
    (1, 'Подготовить демонстрационный стенд', 'Собрать оборудование на площадке до начала выставки.', 1, '2024-05-25 09:00:00'),
    (2, 'Загрузить презентации', 'Собрать материалы всех спикеров и подготовить их к показу.', NULL, '2024-05-26 14:30:00');

INSERT INTO activity_resources (activity_id, name, resource_path, uploaded_by_moderator, uploaded_at) VALUES
    (1, 'Программа мероприятия', 'files/program.pdf', 1, '2024-05-20 08:00:00'),
    (3, 'Список материалов', 'files/art-materials.pdf', NULL, '2024-06-15 10:15:00');

INSERT INTO moderation_requests (activity_id, moderator_id, status, conflict_activity_id, response_message, decline_reason, created_at, updated_at) VALUES
    (3, 1, 'APPROVED', NULL, 'Готов к проведению мероприятия.', NULL, '2024-06-20 12:00:00', '2024-06-21 09:00:00'),
    (2, 1, 'PENDING', NULL, NULL, NULL, '2024-05-28 10:00:00', '2024-05-28 10:00:00');

INSERT INTO activity_votes (activity_id, winner_name, jury_slot, jury_member_name) VALUES
    (1, 'TechVision', 1, 'Дмитрий Морозов');
