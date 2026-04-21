package com.github.warada_v2.core.repositories;

import com.github.warada_v2.shared.entities.Chapter;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ChapterRepository implements PanacheRepository<Chapter> {

    public Optional<Chapter> findByOriginPathParams(String originPathParams) {
        return find("originPathParams", originPathParams).firstResultOptional();
    }

    public void persistOrUpdate(Chapter chapter) {
        if (chapter.getId() == null) {
            getEntityManager().persist(chapter);
        } else {
            getEntityManager().merge(chapter);
        }
    }
}
