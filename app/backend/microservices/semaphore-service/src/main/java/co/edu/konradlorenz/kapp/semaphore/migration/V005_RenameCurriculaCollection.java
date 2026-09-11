package co.edu.konradlorenz.kapp.semaphore.migration;

import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Renames the {@code curricula} collection to {@code pensums}.
 *
 * <p>"Curriculum" was the word all the way down - the URL, the classes, the collection - and
 * nobody in the building says it. The code now says pensum everywhere, which means
 * {@code @Document(collection = "pensums")}, which means a database that still holds the data
 * under the old name would come up empty and look like the catalogue had been lost.
 *
 * <p>Both directions are handled, and neither is destructive:
 * <ul>
 *   <li><strong>A database that already ran V001–V004</strong> has {@code curricula} with data
 *       in it. This renames it, indexes included - {@code renameCollection} carries them.</li>
 *   <li><strong>A fresh database</strong> ran those change units against {@code pensums}
 *       already, because their source says so now. There is no {@code curricula} to rename and
 *       this does nothing.</li>
 * </ul>
 *
 * <p>Refuses to act if both exist. That means somebody has been writing to each, and picking a
 * winner automatically would throw away whichever it did not pick.
 */
@ChangeUnit(id = "semaphore-rename-curricula-to-pensums-v005", order = "005", author = "kapp")
public class V005_RenameCurriculaCollection {

    private static final Logger log = LoggerFactory.getLogger(V005_RenameCurriculaCollection.class);

    private static final String OLD_NAME = "curricula";
    private static final String NEW_NAME = "pensums";

    @Execution
    public void rename(MongoTemplate mongo) {
        MongoDatabase db = mongo.getDb();
        List<String> names = new ArrayList<>();
        db.listCollectionNames().into(names);

        if (!names.contains(OLD_NAME)) {
            log.info("No '{}' collection: nothing to rename.", OLD_NAME);
            return;
        }
        if (names.contains(NEW_NAME)) {
            throw new IllegalStateException(
                    "Both '" + OLD_NAME + "' and '" + NEW_NAME + "' exist. Something has been "
                            + "written to each of them; merge them by hand rather than letting "
                            + "this discard one.");
        }

        mongo.execute(OLD_NAME, collection -> {
            collection.renameCollection(
                    new com.mongodb.MongoNamespace(db.getName(), NEW_NAME));
            return null;
        });
        log.info("Renamed collection '{}' to '{}'", OLD_NAME, NEW_NAME);
    }

    @RollbackExecution
    public void back(MongoTemplate mongo) {
        MongoDatabase db = mongo.getDb();
        List<String> names = new ArrayList<>();
        db.listCollectionNames().into(names);
        if (!names.contains(NEW_NAME) || names.contains(OLD_NAME)) {
            return;
        }
        mongo.execute(NEW_NAME, collection -> {
            collection.renameCollection(new com.mongodb.MongoNamespace(db.getName(), OLD_NAME));
            return null;
        });
    }
}
