package com.tuiken.mamlakat.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@MappedSuperclass
@Setter
@Getter
@NoArgsConstructor
public abstract class Person {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "varchar(255)")
    private UUID id;
    @NonNull
    @Column(unique=true)
    private String url;
    @NonNull
    private String name;
//    @NonNull
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        return id.equals(person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @ElementCollection
    @NonNull
    @Enumerated(EnumType.STRING)
    private Set<Country> country = new HashSet<>();
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<House> house = new HashSet<>();
    private Instant birth;
    private Instant death;
    @NonNull
    @Enumerated(EnumType.STRING)
    PersonStatus status;

    public Person(@NonNull String url) {
        this.url = url;
    }

    public void print() {
        System.out.println("= " + name + " " + gender);
//        if (country!=null && !country.isEmpty()) {
//            country.forEach(c-> System.out.print(c));
//            System.out.println();
//        }
//        System.out.println(String.join(", ", getHouse().stream().map(h->h.toString()).collect(Collectors.toList())));
        System.out.println("= " + lifeString());
//        System.out.println(status);
    }

    String lifeString() {
        return String.format("%s - %s", dateToYear(getBirth()), dateToYear(getDeath()));
    }

    String dateToYear(Instant date) {
        return date==null ? "NA" : String.valueOf(date.atZone(ZoneId.systemDefault()).getYear());
    }
}
