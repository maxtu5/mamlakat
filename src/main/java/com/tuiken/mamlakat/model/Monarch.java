package com.tuiken.mamlakat.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@ToString
@Entity
public class Monarch extends Person {

    @ElementCollection
    private List<Reign> reigns = new ArrayList<>();

    public Monarch(String url) {
        super(url);
    }

    public Monarch() {
        super();
    }


    public String toString(Country country) {
        return String.format(
                "===%s %s\n" +
                        "=Life: %s\n" +
                        "=Rule: %s\n" +
                        "=Houses: %s\n" +
                "=Status: %s",
                getName(), getUrl(),
                lifeString(),
                ruleString(country),
                String.join(", ", getHouse().stream().map(h->h.toString()).collect(Collectors.toList())),
                status.toString());
    }

    public String ruleString(Country country) {
        List<String> strings = reigns.stream()
                .filter(r -> r.getCountry()!=null && r.getCountry().equals(country))
                .map(r -> String.format("%s (%s - %s)",
                        r.title,
                        dateToYear(r.getStart()),
                        dateToYear(r.getEnd())))
                .collect(Collectors.toList());
        return String.join(", ", strings);
    }
}
