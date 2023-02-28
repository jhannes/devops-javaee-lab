create table todo_items
(
    id         uuid primary key,
    title      varchar(200) not null,
    body       text,
    created_at timestamp not null default now()
)