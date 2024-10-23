package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.WikiCacheRecordRepository;
import com.tuiken.mamlakat.model.WikiCacheRecord;
import com.tuiken.mamlakat.utils.RedirectResolver;
import com.tuiken.mamlakat.utils.TokenManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class WikiService {

    private final WikiCacheRecordRepository wikiCacheRecordRepository;
    private final TokenManager tokenManager;

    private static final String ENT_WIKI_STRUCTURED_URL = "https://api.enterprise.wikimedia.com/v2/structured-contents/%s" ;
    private static final String NORMAL_URL_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final String REQUEST_URL_PREFIX = "https://api.enterprise.wikimedia.com/v2/structured-contents/";

    private static final String BODY_FILE = "src\\main\\resources\\request_body.json" ;

//    @Cacheable("wikies")
    @Transactional
    public JSONArray read(String url) throws IOException, URISyntaxException {

        String[] tokens = url.split("/");
        String requestUrl = String.format(ENT_WIKI_STRUCTURED_URL, tokens[tokens.length - 1]);

        HttpHeaders headers = new HttpHeaders();
        String token = tokenManager.getToken();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = new String(Files.readAllBytes(Paths.get(BODY_FILE)));
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = null;
        String realUrl = null;
        RedirectResolver resolver = new RedirectResolver();

        RestTemplate restTemplate = new RestTemplate();

        String resolvedUrl = resolver.resolve(requestUrl);

        WikiCacheRecord cacheRecord = wikiCacheRecordRepository
                .findByUrl(resolvedUrl.replace(REQUEST_URL_PREFIX, NORMAL_URL_PREFIX))
                .orElse(new WikiCacheRecord(resolvedUrl.replace(REQUEST_URL_PREFIX, NORMAL_URL_PREFIX)));

        if (cacheRecord.getCacheId()!=null) {
            return new JSONArray(cacheRecord.getBody());
        }

        try {
            response = restTemplate.exchange(
                    resolvedUrl, HttpMethod.POST, requestEntity, String.class);
        } catch (HttpClientErrorException e) {
            return null;
        }

        String jsonString = response.getBody();

        JSONArray retval = new JSONArray(jsonString);
        if (!requestUrl.equals(resolvedUrl)) {
            resolvedUrl = resolvedUrl.replace(REQUEST_URL_PREFIX, NORMAL_URL_PREFIX);
            JSONObject object = new JSONObject();
            object.put("realUrl", resolvedUrl);
            retval.put(object);
            cacheRecord.setUrl(resolvedUrl);
        }
        cacheRecord.setBody(jsonString);
        wikiCacheRecordRepository.save(cacheRecord);

        return retval;
    }

}