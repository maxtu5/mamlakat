package com.tuiken.mamlakat;

import com.tuiken.mamlakat.utils.TokenManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenManagerTest {

    @Autowired
    private TokenManager tokenManager;

    @Test
    void refresh() throws URISyntaxException {
        tokenManager.refresh();
    }

    @Test
    void getToken() throws URISyntaxException, IOException {
        String token = tokenManager.getToken();
        System.out.println(token);
    }
}