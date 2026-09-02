package co.edu.konradlorenz.kapp.semaphore.repository;

import co.edu.konradlorenz.kapp.semaphore.domain.Program;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProgramRepository extends MongoRepository<Program, String> {

    List<Program> findAllByOrderByNameAsc();
}
