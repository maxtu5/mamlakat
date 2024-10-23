package com.tuiken.mamlakat.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Embeddable
public class Reign {
    String title;
    private Instant start;
    private Instant end;
    private Instant coronation;
    @Enumerated(EnumType.STRING)
    private Country country;
}
