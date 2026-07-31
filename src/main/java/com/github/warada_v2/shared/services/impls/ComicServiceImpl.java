package com.github.warada_v2.shared.services.impls;

import com.github.warada_v2.shared.services.ComicService;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.exceptions.ResourceNotFoundException;
import com.github.warada_v2.shared.models.PageRequest;
import com.github.warada_v2.shared.repositories.ComicRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ComicServiceImpl implements ComicService {

  private static final Logger log = Logger.getLogger(ComicServiceImpl.class);

  @Inject ComicRepository comicRepository;

  @Override
  public List<Comic> getPageableComics(PageRequest pageRequest) {
    if (pageRequest == null) {
      pageRequest = new PageRequest();
    }
    return comicRepository.findPageableComics(pageRequest.toSort(), pageRequest.toPage());
  }

  @Override
  public Comic findById(Long id) {
    return comicRepository
        .findByIdOptional(id)
        .orElseThrow(() -> new ResourceNotFoundException("Comic", "id", id));
  }
}
