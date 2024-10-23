package com.tuiken.mamlakat.utils;

import com.tuiken.mamlakat.dto.apimonarch.login.TokenRequestDto;
import com.tuiken.mamlakat.dto.apimonarch.login.TokenResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

@Component
public class TokenManager {

    private static final String TOKEN_FILE = "src\\main\\resources\\token.txt";
    private final URI ENT_WIKI_LOGIN_URL = new URI("https://auth.enterprise.wikimedia.com/v1/login");

    @Value("${MMLKT_WIKI_USER}")
    private String wikiUser;
    @Value("${MMLKT_WIKI_PASSWORD}")
    private String wikiPassword;

    public TokenManager() throws URISyntaxException {
    }

    public void refresh() {
        RestTemplate restTemplate = new RestTemplate();
        TokenRequestDto request = TokenRequestDto.builder()
                .username(wikiUser)
                .password(wikiPassword)
                .build();
        ResponseEntity<TokenResponseDto> responseEntity = restTemplate.postForEntity(ENT_WIKI_LOGIN_URL, request, TokenResponseDto.class);
        TokenResponseDto response = responseEntity.getBody();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TOKEN_FILE))) {
             writer.write(response.access_token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getToken() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(TOKEN_FILE));
        String token = reader.readLine();
        reader.close();
        return token;
    }
}
