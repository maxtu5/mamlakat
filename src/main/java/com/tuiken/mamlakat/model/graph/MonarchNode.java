package com.tuiken.mamlakat.model.graph;

import com.tuiken.mamlakat.model.Gender;
import com.tuiken.mamlakat.model.House;
import com.tuiken.mamlakat.model.PersonStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.Set;

@Node("Monarch")
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MonarchNode {
    @Id
    @EqualsAndHashCode.Include
    private String id;
    private String url;
    private String name;
    private Gender gender;
//    @Relationship(type = "IS_MEMBEROF", direction = Relationship.Direction.OUTGOING)
//    private Set<House> house;
    private LocalDate birth;
    private LocalDate death;
    private PersonStatus status;
    @Relationship(type = "RULED_IN", direction = Relationship.Direction.OUTGOING)
    private Set<ReignNode> reigns;
    @Relationship(type = "FATHER_OF", direction = Relationship.Direction.INCOMING)
    private MonarchNode father;
    @Relationship(type = "MOTHER_OF", direction = Relationship.Direction.INCOMING)
    private MonarchNode mother;

}
