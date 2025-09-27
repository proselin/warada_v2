create table Entities (
                          id bigint not null,
                          field varchar(255),
                          primary key (id)
);

create sequence Entities_SEQ start with 1 increment by 50;