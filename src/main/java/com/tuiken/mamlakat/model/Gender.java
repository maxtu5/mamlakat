package com.tuiken.mamlakat.model;

public enum Gender {
    MALE(new String[]{"King ", "Emperor", "Prince ", "Duke", "Earl", "Lord", "Count "}),
    FEMALE(new String[]{"Queen", "Empress", "Princess", "Duchess", "Lady", "Infanta","Viscountess", "Baroness", "Diana", "Landgravine", "Elizabeth", "Anne", "Countess"});

    Gender(String[] keywords) {
        this.keywords = keywords;
    }

    private final String[] keywords;

    public static Gender fromTitle(String title) {
        String titleUpper = title.toUpperCase();
        for (Gender g : Gender.values()) {
            for (String key : g.keywords) {
                if (titleUpper.contains(key.toUpperCase())) {
                    return g;
                }
            }
        }
        return null;
    }
}