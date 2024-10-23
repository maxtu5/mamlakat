package com.tuiken.mamlakat.model.graph;

import com.tuiken.mamlakat.model.Country;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node("Reign")
@AllArgsConstructor
@Setter
@Getter
public class ReignNode {
    @Id
    private String id;
    String title;
    private LocalDate start;
    private LocalDate end;
    private LocalDate coronation;
    private Country country;
    @Relationship(type = "WAS_MONARCH", direction = OUTGOING)
    private MonarchNode monarch;
    @Relationship(type = "PREDECESSOR", direction = OUTGOING)
    private ReignNode predecessor;
    @Relationship(type = "SUCCESSOR", direction = OUTGOING)
    private ReignNode successor;
}
