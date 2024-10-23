package com.tuiken.mamlakat.model.graph;

import com.tuiken.mamlakat.model.Country;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;
import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("Throne")
@AllArgsConstructor
@Getter
@Setter
public class ThroneNode {
    @Id
    private String id;
    private String name;
    private Country country;
    @Relationship(type = "WAS_RULER", direction = OUTGOING)
//    @Relationship(type = "HAD_REIGN", direction = INCOMING)
    private Set<ReignNode> rulers;
    @Relationship(type = "LATEST_REIGN", direction = OUTGOING)
    private ReignNode latest;
}
