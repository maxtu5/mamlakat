package com.tuiken.mamlakat.dao.graph;

import com.tuiken.mamlakat.model.graph.MonarchNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface MonarchNodeRepository extends Neo4jRepository<MonarchNode, String> {
    @Query("MATCH(t:Monarch) WHERE t.id = $id RETURN t")
    Optional<MonarchNode> findById(String id);
}