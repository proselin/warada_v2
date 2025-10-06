package com.github.warada_v2.services;

import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.models.PageRequest;
import com.github.warada_v2.repositories.ComicRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class ComicService {
    @Inject
    ComicRepository comicRepository;

    public List<Comic> getPageableComics(PageRequest pageRequest) {
        return comicRepository.findPageableComics(pageRequest.toSort(), pageRequest.toPage());
    }

    public Comic findById(Long id) {
        return comicRepository.findByIdOptional(id).orElseThrow(NotFoundException::new);
    }
}
