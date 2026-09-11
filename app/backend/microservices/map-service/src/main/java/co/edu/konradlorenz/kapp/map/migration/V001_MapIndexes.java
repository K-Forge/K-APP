package co.edu.konradlorenz.kapp.map.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition.TextIndexDefinitionBuilder;

/**
 * Baseline indexes for the campus map.
 *
 * <p>Spaces live in their own flat collection rather than nested inside buildings. The
 * primary access pattern is "find a room by name anywhere on campus", and a nested model
 * would return whole building documents for the client to sift through. At roughly 500
 * spaces, a flat collection with a text index is both simpler and faster.
 *
 * <p>Change units are append-only. Never edit one that has run; add a new one.
 */
@ChangeUnit(id = "map-indexes-v001", order = "001", author = "kapp")
public class V001_MapIndexes {

    @Execution
    public void execute(MongoTemplate mongo) {
        mongo.indexOps("buildings").createIndex(
                new Index().on("code", Sort.Direction.ASC).unique().named("uk_buildings_code"));

        mongo.indexOps("buildings").createIndex(
                new Index().on("campus", Sort.Direction.ASC).named("ix_buildings_campus"));

        // Room codes are unique per building, not globally: two buildings can each have
        // a room 302.
        mongo.indexOps("spaces").createIndex(
                new Index().on("buildingId", Sort.Direction.ASC)
                        .on("code", Sort.Direction.ASC)
                        .unique().named("uk_spaces_building_code"));

        // Lets the schedule screen jump straight from a class to its classroom.
        mongo.indexOps("spaces").createIndex(
                new Index().on("code", Sort.Direction.ASC).named("ix_spaces_code"));

        // MongoDB permits exactly one text index per collection, so every searchable
        // field goes in this one. Weights put an exact room code above a name match,
        // and a name above an alias.
        //
        // Spanish as the default language gives stemming, and MongoDB text search is
        // diacritic-insensitive by default - so "sala de sistemas" matches an alias
        // written "Sala de Sistemas", and "matematicas" matches "Matematicas".
        mongo.indexOps("spaces").createIndex(
                new TextIndexDefinitionBuilder()
                        .named("tx_spaces_search")
                        .withDefaultLanguage("spanish")
                        .onField("code", 5F)
                        .onField("name", 3F)
                        .onField("aliases", 2F)
                        .build());
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongo) {
        mongo.indexOps("buildings").dropIndex("uk_buildings_code");
        mongo.indexOps("buildings").dropIndex("ix_buildings_campus");
        mongo.indexOps("spaces").dropIndex("uk_spaces_building_code");
        mongo.indexOps("spaces").dropIndex("ix_spaces_code");
        mongo.indexOps("spaces").dropIndex("tx_spaces_search");
    }
}
