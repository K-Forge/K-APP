package co.edu.konradlorenz.kapp.auth.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Reads only. Redemption and release go through
 * {@code InvitationCodeService}, which uses atomic server-side updates: anything that
 * loads a code here and saves it back would reintroduce the race the guarded
 * {@code findAndModify} exists to close.
 */
public interface InvitationCodeRepository extends MongoRepository<InvitationCode, String> {

    Optional<InvitationCode> findByCode(String code);
}
