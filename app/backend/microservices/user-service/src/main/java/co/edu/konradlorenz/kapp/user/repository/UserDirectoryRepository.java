package co.edu.konradlorenz.kapp.user.repository;

import co.edu.konradlorenz.kapp.user.domain.UserProfile;
import co.edu.konradlorenz.kapp.user.domain.UserRole;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Runs the filtered, paged directory query built by {@link UserDirectoryQueries}.
 */
@Repository
public class UserDirectoryRepository {

    private final MongoTemplate mongoTemplate;

    public UserDirectoryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<UserProfile> findPage(UserRole role, Boolean active, List<String> terms,
                                      int page, int size) {
        return mongoTemplate.find(UserDirectoryQueries.page(role, active, terms, page, size),
                UserProfile.class);
    }

    /**
     * Counts against the filter only. The paging clauses are dropped on purpose: a count
     * carrying {@code skip} and {@code limit} would report the size of the page rather
     * than the size of the result set, and the client would see one page and be told
     * there are no more.
     */
    public long count(UserRole role, Boolean active, List<String> terms) {
        return mongoTemplate.count(new Query(UserDirectoryQueries.filter(role, active, terms)),
                UserProfile.class);
    }
}
