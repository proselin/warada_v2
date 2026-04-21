package com.github.warada_v2.core.repositories;

import com.github.warada_v2.shared.entities.Image;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageRepository implements PanacheRepository<Image> {
}
