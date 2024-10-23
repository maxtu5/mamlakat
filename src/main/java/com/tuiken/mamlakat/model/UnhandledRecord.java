package com.tuiken.mamlakat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UnhandledRecord {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(columnDefinition = "varchar(255)")
    UUID id;
    String child;
    String parentUrl;
    String childUrl;
    String solution;
}
