package co.edu.konradlorenz.kapp.user.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * The multikey index that makes the directory search cheap.
 *
 * <p>{@code searchTokens} is an array, so MongoDB indexes one key per element. The
 * anchored, flagless regex the service builds ({@code /^varg/}) is turned into an index
 * range over those keys, which is the whole reason the field exists. Drop this index and
 * every search silently becomes a collection scan - correct results, growing latency, no
 * error anywhere.
 *
 * <p>A new change unit rather than an edit to {@code V001}: change units are append-only,
 * because Mongock records the ones it has already run and will not replay a modified one.
 */
@ChangeUnit(id = "user-search-tokens-v002", order = "002", author = "kapp")
public class V002_UserSearchIndexes {

    private static final String COLLECTION = "users";
    static final String SEARCH_TOKENS_INDEX = "ix_users_search_tokens";

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION)
                .createIndex(new Index().on("searchTokens", Sort.Direction.ASC)
                        .named(SEARCH_TOKENS_INDEX));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(COLLECTION).dropIndex(SEARCH_TOKENS_INDEX);
    }
}
