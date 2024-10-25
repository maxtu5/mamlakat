package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.WikiCacheRecordRepository;
import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.WikiCacheRecord;
import com.tuiken.mamlakat.utils.RedirectResolver;
import com.tuiken.mamlakat.utils.TokenManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class WikiService {

    private final WikiCacheRecordRepository wikiCacheRecordRepository;
    private final TokenManager tokenManager;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String ENT_WIKI_STRUCTURED_URL = "https://api.enterprise.wikimedia.com/v2/structured-contents/%s";
    private static final String NORMAL_URL_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final String REQUEST_URL_PREFIX = "https://api.enterprise.wikimedia.com/v2/structured-contents/";

    private static final String WIKI_API_REQUEST = """
            {
              "filters": [
                {"field":"is_part_of.identifier","value":"enwiki"}
              ]
            }
            """;

    //    @Cacheable("wikies")
    @Transactional
    public JSONArray read(String url) throws WikiApiException {

        String[] tokens = url.split("/");
        String requestUrl = String.format(ENT_WIKI_STRUCTURED_URL, tokens[tokens.length - 1]);
        RedirectResolver resolver = new RedirectResolver();
        String resolvedUrl = resolver.resolve(requestUrl);

        // try to find in cache
        WikiCacheRecord cacheRecord = wikiCacheRecordRepository
                .findByUrl(resolvedUrl.replace(REQUEST_URL_PREFIX, NORMAL_URL_PREFIX))
                .orElse(new WikiCacheRecord(resolvedUrl.replace(REQUEST_URL_PREFIX, NORMAL_URL_PREFIX)));
        if (cacheRecord.getCacheId() != null) {
            return new JSONArray(cacheRecord.getBody());
        }
        // not found in cache, retrieve from wiki API
        String rawResponse = loadFromWikiApi(resolvedUrl);
        if (rawResponse==null) return null;
        JSONArray retval = new JSONArray(rawResponse);
        // save in cache
        cacheRecord.setBody(rawResponse);
        wikiCacheRecordRepository.save(cacheRecord);
        return retval;
    }

    private String loadFromWikiApi(String url) throws WikiApiException {
        HttpHeaders headers = new HttpHeaders();
        String token = tokenManager.getToken();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(WIKI_API_REQUEST, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);
            return response.getBody();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                tokenManager.refresh();
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                            url, HttpMethod.POST, requestEntity, String.class);
                    return response.getBody();
                } catch (RestClientResponseException e1) {
                    throw new WikiApiException("Error reading from wiki API", e1);
                }
            } else {
                throw new WikiApiException("Error reading from wiki API", e);
            }
        }
    }

}