package com.tuiken.mamlakat.model;

import lombok.Getter;

public enum Country {
    ENGLAND(new String[]{"United Kingdom", "England", "Great Britain", "English"}),
    BELGIUM(new String[]{"Belgians"}),
    NORWAY(new String[]{"Norway"}),
    DENMARK(new String[]{"Denmark"}),
    NETHERLANDS(new String[]{"Netherlands"}),
    SPAIN(new String[]{"Spain", "Spanish"}),
    SWEDEN(new String[]{"Sweden"}),
    HOLY_ROMAN_EMPIRE(new String[]{"Holy Roman Emperor"}),
    TUSCANY(new String[]{"Tuscany", "Etruria"}),
    PORTUGAL(new String[]{"Portugal"}),
    PRUSSIA(new String[]{"Prussia"});

    @Getter
    private final String[] keywords;

    Country(String[] keywords) {
        this.keywords = keywords;
    }

    public boolean belongs(String title) {
        String titleUpper = title.toUpperCase();
        for(String key : this.keywords) {
            if (titleUpper.contains(key.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
