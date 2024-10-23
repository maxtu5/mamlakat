package com.tuiken.mamlakat.model.dtos;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.Monarch;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
public class Throne {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(columnDefinition = "varchar(255)")
    private UUID id;
    private String name;
    @Enumerated(EnumType.STRING)
    private Country country;
    @ElementCollection
    @CollectionTable
    @OrderColumn
    private List<String> monarchsIds = new ArrayList<>();
}
