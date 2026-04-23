package com.github.warada_v2.display.repositories;

import com.github.warada_v2.shared.entities.Tag;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public List<Tag> findAllOrderByComicCountDesc() {
        return list("ORDER BY comicCount DESC");
    }
}
