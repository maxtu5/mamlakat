package com.tuiken.mamlakat.model;

public class WikiPage {
    String url;

    public WikiPage(String url) {
        this.url = url;
    }

    public String getName() {
        String[] tokens = url.split("/");
        return tokens[tokens.length-1];
    }
}
