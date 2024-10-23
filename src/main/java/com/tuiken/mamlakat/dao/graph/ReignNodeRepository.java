package com.tuiken.mamlakat.dao.graph;

import com.tuiken.mamlakat.model.graph.MonarchNode;
import com.tuiken.mamlakat.model.graph.ReignNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface ReignNodeRepository extends Neo4jRepository<ReignNode, String> {

    @Query("MATCH(t:Throne)-[:LATEST_REIGN]->(m:Reign) WHERE t.id = $id RETURN m")
    ReignNode findLatestReignForThrone(String id);

    @Query("MATCH(m:Reign)-[:PREDECESSOR]->(p:Reign) WHERE m.id = $id RETURN p")
    ReignNode findPredecessor(String id);
}