package com.tuiken.mamlakat.utils;

import com.tuiken.mamlakat.dto.resolver.RedirectsResponse;
import lombok.NoArgsConstructor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@NoArgsConstructor
public class RedirectResolver {

    private static final String REDIRECT_URL = "https://en.wikipedia.org/w/api.php?action=query&format=json&titles=%s&redirects=1&formatversion=2";
    private static final String PUBLIC_URL_TEMPLATE = "https://en.wikipedia.org/wiki/ANY";

    public String resolve(String url) {
        RestTemplate restTemplate = new RestTemplate();
        String title = extractTitle(url);
        String redirectUrl = String.format(REDIRECT_URL, title);
        RedirectsResponse response = restTemplate.getForObject(redirectUrl, RedirectsResponse.class);
        String newTitle = normalize(response.query.pages.get(0).title);
        if (newTitle.equals(title)) {
            return url;
        }
        return replaceTitle(url, newTitle);
    }
    
    public String getPublicUrl(String src) {
        String[] tokens = src.split("/");
        return replaceTitle(PUBLIC_URL_TEMPLATE, tokens[tokens.length-1]);
    }

    private String replaceTitle(String url, String newTitle) {
        String[] tokens = url.split("/");
        tokens[tokens.length-1] = newTitle;
        return String.join("/", tokens);
    }

    private String normalize(String title) {
        return title.replace(' ', '_');
    }

    private String extractTitle(String url) {
        String[] tokens = url.split("/");
        return tokens[tokens.length-1];
    }
}
