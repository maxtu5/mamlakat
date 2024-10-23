package com.tuiken.mamlakat.builders;

import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.*;
import com.tuiken.mamlakat.service.MonarchRetriever;
import com.tuiken.mamlakat.service.MonarchService;
import com.tuiken.mamlakat.service.PersonRetriever;
import com.tuiken.mamlakat.service.WikiService;
import com.tuiken.mamlakat.utils.JsonUtils;
import com.tuiken.mamlakat.utils.RedirectResolver;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonBuilder {

    private final WikiService wikiService;
    private final PersonRetriever personRetriever;
    private final MonarchService monarchService;
    private final MonarchRetriever monarchRetriever;

    public Monarch buildPerson(String url, Gender gender) {
        JSONArray jsonArray = null;
        try {
            jsonArray = wikiService.read(url);
        } catch (WikiApiException e) {
            return null;
        }
        if (jsonArray==null || JsonUtils.readInfoboxes(jsonArray).size()==0) return null;
        String realUrl=null;
        realUrl = realUrl == null ? url : realUrl;

        Monarch person = new Monarch(realUrl);
        person.setName(personRetriever.retrieveName(jsonArray));
        person.setGender(gender==null ? Gender.fromTitle(person.getName()) : gender);
        person.setBirth(personRetriever.retrieveOneDate(jsonArray, "Born"));
        person.setDeath(personRetriever.retrieveOneDate(jsonArray, "Died"));
        person.setHouse(personRetriever.retrieveHouses(jsonArray));
        person.setStatus(PersonStatus.NEW_URL);
        System.out.println("== Created person " + person.getName());
        return person;
    }

    public Monarch findOrCreateOptionalSave(String url, Country country, boolean save) throws WikiApiException {
        RedirectResolver resolver = new RedirectResolver();
        String resolvedUrl = resolver.resolve(url);
        System.out.println("Reading from source: " + resolvedUrl);
        Monarch monarch = monarchService.findByUrl(resolvedUrl);
        JSONArray jsonArray = wikiService.read(resolvedUrl);

        if (monarch != null) {
            System.out.println("Exists");
            if (country!=null) {
                List<Reign> reigns = monarchRetriever.retrieveReigns(jsonArray, country);
                List<Reign> clearedReigns = monarch.getReigns().stream()
                        .filter(r -> !r.getCountry().equals(country))
                        .collect(Collectors.toList());
                clearedReigns.addAll(reigns);
                monarch.setReigns(clearedReigns);
                monarchService.saveMonarch(monarch);
            }
        } else {
            System.out.print("Attempting to create...");
            monarch = buildPerson(resolvedUrl, null);
            if (monarch!=null) {
                monarch.setGender(Gender.fromTitle(monarch.getName()));
                if (country != null) {
                    List<Reign> reigns = monarchRetriever.retrieveReigns(jsonArray, country);
                    Gender gender = reigns.stream()
                            .map(Reign::getTitle)
                            .filter(Objects::nonNull)
                            .map(Gender::fromTitle)
                            .filter(Objects::nonNull)
                            .findFirst().orElse(null);
                    if (gender != null) monarch.setGender(gender);
                    monarch.setReigns(reigns);
                }
                if (save) monarchService.saveMonarch(monarch);
            } else {
                System.out.println("fail");
            }
        }
//        printMonarch(monarch, country, false);
        return monarch;
    }

}
