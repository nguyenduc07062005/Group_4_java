create table if not exists users (
    id bigint primary key auto_increment,
    username varchar(100) not null,
    password_hash varchar(255) not null,
    full_name varchar(150) not null,
    role varchar(50) not null,
    enabled boolean not null default true,
    constraint uk_users_username unique (username)
);
