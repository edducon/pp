SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS participant_registrations;
DROP TABLE IF EXISTS event_tasks;
DROP TABLE IF EXISTS moderation_requests;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS jury_members;
DROP TABLE IF EXISTS moderators;
DROP TABLE IF EXISTS participants;
DROP TABLE IF EXISTS organizers;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS directions;
DROP TABLE IF EXISTS cities;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE directions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ORGANIZER','PARTICIPANT','MODERATOR','JURY') NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    phone VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE organizers (
    account_id BIGINT PRIMARY KEY,
    company VARCHAR(255),
    website VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE participants (
    account_id BIGINT PRIMARY KEY,
    company VARCHAR(255),
    job_title VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE moderators (
    account_id BIGINT PRIMARY KEY,
    expertise VARCHAR(255),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE jury_members (
    account_id BIGINT PRIMARY KEY,
    achievements TEXT,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    city_id BIGINT NOT NULL,
    direction_id BIGINT NOT NULL,
    organizer_id BIGINT NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES cities(id),
    FOREIGN KEY (direction_id) REFERENCES directions(id),
    FOREIGN KEY (organizer_id) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE moderation_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    organizer_id BIGINT NOT NULL,
    moderator_id BIGINT NULL,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL,
    message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (organizer_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (moderator_id) REFERENCES accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE event_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    stage VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status ENUM('TODO','IN_PROGRESS','DONE','BLOCKED') NOT NULL,
    due_date DATE NULL,
    assignee VARCHAR(255),
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE participant_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    status ENUM('REQUESTED','APPROVED','DECLINED','CANCELLED') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_registration(event_id, participant_id),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO cities (name) VALUES
 ('Москва'),
 ('Санкт-Петербург'),
 ('Новосибирск');

INSERT INTO directions (title) VALUES
 ('SOC & Incident Response'),
 ('Malware Research'),
 ('Cloud Security');

INSERT INTO accounts (email, password_hash, role, first_name, last_name, middle_name, phone) VALUES
 ('organizer@event.local', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', 'ORGANIZER', 'Анна', 'Ковалёва', 'Игоревна', '+7-900-000-01-01'),
 ('participant@event.local', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', 'PARTICIPANT', 'Илья', 'Сергеев', 'Андреевич', '+7-900-000-02-02'),
 ('moderator@event.local', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', 'MODERATOR', 'Марина', 'Васильева', 'Петровна', '+7-900-000-03-03'),
 ('jury@event.local', '2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', 'JURY', 'Роман', 'Филиппов', 'Юрьевич', '+7-900-000-04-04');

INSERT INTO organizers (account_id, company, website) VALUES
 (1, 'Event Security Summit', 'https://securitysummit.local');

INSERT INTO participants (account_id, company, job_title) VALUES
 (2, 'Secure Future', 'Security Analyst');

INSERT INTO moderators (account_id, expertise) VALUES
 (3, 'SOC Automation');

INSERT INTO jury_members (account_id, achievements) VALUES
 (4, 'Победитель конкурса «Лучший CISO 2023»');

INSERT INTO events (title, description, start_date, end_date, city_id, direction_id, organizer_id, capacity, status) VALUES
 ('Security Operations Bootcamp', 'Интенсив для аналитиков SOC с практическими лабораториями', '2024-06-15', '2024-06-17', 1, 1, 1, 150, 'PUBLISHED'),
 ('Malware Reverse Challenge', 'Соревнование по анализу вредоносного кода и Threat Hunting', '2024-07-05', '2024-07-06', 2, 2, 1, 80, 'PUBLISHED');

INSERT INTO event_tasks (event_id, stage, title, status, due_date, assignee, notes) VALUES
 (1, 'Подготовка', 'Согласовать программу с модератором', 'IN_PROGRESS', '2024-05-20', 'Марина Васильева', 'Требуется обновить секцию про автоматизацию SOC'),
 (1, 'Маркетинг', 'Запустить email-рассылку', 'TODO', '2024-05-25', 'Анна Ковалёва', 'Использовать свежую базу подписчиков'),
 (2, 'Подготовка', 'Собрать тестовые образцы', 'IN_PROGRESS', '2024-06-10', 'Роман Филиппов', 'Подготовить 5 новых образцов под Windows 11');

INSERT INTO participant_registrations (event_id, participant_id, status) VALUES
 (1, 2, 'APPROVED');

INSERT INTO moderation_requests (event_id, organizer_id, moderator_id, status, message) VALUES
 (1, 1, 3, 'APPROVED', 'Согласовано и опубликовано'),
 (2, 1, NULL, 'PENDING', 'Просьба назначить модератора');
