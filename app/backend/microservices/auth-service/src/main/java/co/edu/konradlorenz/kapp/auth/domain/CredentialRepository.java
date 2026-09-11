package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CredentialRepository extends MongoRepository<Credential, String> {

    /**
     * Case-insensitive because people type their e-mail however they like, while the
     * unique index is on the stored form. Registration must normalise to lower case so
     * the two agree.
     */
    Optional<Credential> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * By the id user-service knows the person as. Credentials and profiles are separate
     * documents in separate databases; {@code userId} is the only thing that joins them, and
     * it is what an administrator's "deactivate" has to travel on.
     */
    Optional<Credential> findByUserId(String userId);
}
