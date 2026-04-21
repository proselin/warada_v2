package com.github.warada_v2.display.services;

import com.github.warada_v2.shared.entities.Tag;
import com.github.warada_v2.display.repositories.TagRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;
import java.util.List;

@ApplicationScoped
public class TagServiceImpl implements TagService {

    private static final Logger log = Logger.getLogger(TagServiceImpl.class);

    @Inject
    TagRepository tagRepository;

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAllOrderByComicCountDesc();
    }

    @Override
    public Tag findById(Long id) {
        return tagRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Tag not found with id: " + id));
    }
}
