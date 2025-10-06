CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE chapter_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comic_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE image_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE _users
(
    token_version integer not null,
    created_at    timestamp(6),
    id            bigint  not null,
    updated_at    timestamp(6),
    email         varchar(255) unique,
    password      varchar(255),
    role          varchar(255),
    salt          varchar(255),
    status        varchar(255),
    username      varchar(255) unique,
    primary key (id)
);
CREATE TABLE chapters
(
    position        integer,
    comic_id        bigint,
    created_at      timestamp(6),
    id              bigint not null,
    updated_at      timestamp(6),
    chapter_num     varchar(255),
    crawling_status varchar(255),
    source_url      varchar(255),
    type            varchar(255),
    primary key (id)
);
CREATE TABLE comics
(
    chapter_count   integer,
    created_at      timestamp(6),
    id              bigint       not null,
    thumb_image_id  bigint unique,
    updated_at      timestamp(6),
    slug            varchar(200) not null unique,
    author          varchar(255),
    crawling_status varchar(255) not null,
    description     varchar(255),
    origin_id       varchar(255),
    origin_url      varchar(255),
    status          varchar(255) not null,
    tags            varchar(255),
    title           varchar(255),
    primary key (id)
);
CREATE TABLE images
(
    position   integer,
    chapter_id bigint,
    created_at timestamp(6),
    id         bigint not null,
    updated_at timestamp(6),
    file_name  varchar(255),
    origin_url varchar(255),
    type       varchar(255),
    primary key (id)
);
ALTER TABLE IF EXISTS chapters
    ADD CONSTRAINT FK9c6gj8y5kn52iwhl7he13bmsf FOREIGN KEY (comic_id) REFERENCES comics;
ALTER TABLE IF EXISTS comics
    ADD CONSTRAINT FK4ygd3lkbnmlrs9a4mh3a7aa3g FOREIGN KEY (thumb_image_id) REFERENCES images;
ALTER TABLE IF EXISTS images
    ADD CONSTRAINT FKmpod7x0uie61dul7wvkbgpqce FOREIGN KEY (chapter_id) REFERENCES chapters;