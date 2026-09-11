package co.edu.konradlorenz.kapp.user.repository;

import co.edu.konradlorenz.kapp.user.domain.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Document access for profiles keyed by id or by e-mail.
 *
 * <p>The directory listing is deliberately not here: it needs a query built at runtime
 * from three optional filters, which a derived method name cannot express. See
 * {@link UserDirectoryRepository}.
 */
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    /**
     * @param email must already be lowercased by the caller. The unique index is on the
     *              stored value, so a lookup with different capitalisation finds nothing
     *              and the upsert it guards would create a duplicate account
     */
    Optional<UserProfile> findByEmail(String email);
}
