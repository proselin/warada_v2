package com.github.warada_v2.repositories;


import com.github.warada_v2.db_entities.Image;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public interface ImageRepository extends PanacheRepository<Image> {
}
