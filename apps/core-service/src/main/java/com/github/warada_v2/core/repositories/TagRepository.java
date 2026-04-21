package com.github.warada_v2.core.repositories;

import com.github.warada_v2.shared.entities.Tag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public Optional<Tag> findByNormalizedName(String normalizedName) {
        return find("normalizedName", normalizedName).firstResultOptional();
    }

    public List<Tag> findByNormalizedNameIn(List<String> normalizedNames) {
        return list("normalizedName in ?1", normalizedNames);
    }
}
