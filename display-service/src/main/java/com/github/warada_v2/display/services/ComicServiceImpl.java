package com.github.warada_v2.display.services;

import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.display.repositories.ComicRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class ComicServiceImpl implements ComicService {

    private static final Logger log = Logger.getLogger(ComicServiceImpl.class);

    @Inject
    ComicRepository comicRepository;

    @Override
    public List<Comic> getPageableComics(PageRequest pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequest();
        }
        return comicRepository.findPageableComics(pageRequest.toSort(), pageRequest.toPage());
    }

    @Override
    public Comic findById(Long id) {
        return comicRepository.findByIdOptional(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comic", "id", id));
    }
}
