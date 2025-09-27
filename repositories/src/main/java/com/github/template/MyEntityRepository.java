package com.github.template;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.persistence.Entity;

public interface MyEntityRepository extends PanacheRepository<MyEntity> {

}
