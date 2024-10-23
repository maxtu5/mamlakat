package com.tuiken.mamlakat.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Provenence {
    @Id
    @Column(columnDefinition = "varchar(255)")
    UUID id;
    @Column(columnDefinition = "varchar(255)")
    UUID mother;
    @Column(columnDefinition = "varchar(255)")
    UUID father;
    public Provenence(UUID id) {
        this.id = id;
    }
}
