package guru.springframework.services;

import guru.springframework.commands.UnitOfMeasureCommand;
import guru.springframework.domain.UnitOfMeasure;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Created by jt on 6/28/17.
 */
public interface UnitOfMeasureService {
    Flux<UnitOfMeasureCommand> listAllUoms();

    Mono<UnitOfMeasure> findById(String id);
}
