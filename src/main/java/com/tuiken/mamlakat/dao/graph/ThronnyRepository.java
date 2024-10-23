package com.tuiken.mamlakat.dao.graph;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.graph.ThroneNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface ThronnyRepository extends Neo4jRepository<ThroneNode, String> {

    @Query("MATCH(t:Throne) WHERE t.country = $country RETURN t")
    Optional<ThroneNode> findByCountry(Country country);
}