package com.tuiken.mamlakat.builders;

import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.*;
import com.tuiken.mamlakat.service.*;
import com.tuiken.mamlakat.utils.JsonUtils;
import com.tuiken.mamlakat.utils.RedirectResolver;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

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
    private final AiResolverService aiResolverService;

    public Monarch buildPerson(String url, Gender gender) {
        JSONArray jsonArray = null;
        try {
            jsonArray = wikiService.read(url);
        } catch (WikiApiException e) {
            return null;
        }
        if (jsonArray == null || JsonUtils.readInfoboxes(jsonArray).size() == 0) return null;
        String realUrl = null;
        realUrl = realUrl == null ? url : realUrl;

        Monarch person = new Monarch(realUrl);
        person.setName(personRetriever.retrieveName(jsonArray));
        person.setGender(gender == null ? Gender.fromTitle(person.getName()) : gender);
        person.setBirth(personRetriever.retrieveOneDate(jsonArray, "Born"));
        person.setDeath(personRetriever.retrieveOneDate(jsonArray, "Died"));
        person.setHouse(personRetriever.retrieveHouses(jsonArray));
        person.setStatus(PersonStatus.NEW_URL);
        System.out.println("== Created person " + person.getName());
        return person;
    }

    public Monarch findOrCreateOptionalSave(String url, Country country, boolean save) {
        RedirectResolver resolver = new RedirectResolver();
        String resolvedUrl = resolver.resolve(url);
        System.out.println("Reading from source: " + resolvedUrl);
        Monarch monarch = monarchService.findByUrl(resolvedUrl);
        JSONArray jsonArray = null;
        try {
            jsonArray = wikiService.read(resolvedUrl);
        } catch (WikiApiException e) {
            return null;
        }
        if (monarch != null) {
            System.out.println("Exists");
            if (country != null) {
                // refresh reigns for country
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
            if (monarch != null) {
                if (country != null) {
                    List<Reign> reigns = monarchRetriever.retrieveReigns(jsonArray, country);
                    monarch.setReigns(reigns);
                }
                monarch.setGender(detectGender(monarch));
                if (save) monarchService.saveMonarch(monarch);
            } else {
                System.out.println("Create attempt failed");
            }
        }
//        printMonarch(monarch, country, false);
        return monarch;
    }

    private Gender detectGender(Monarch monarch) {
        Gender retval = Gender.fromTitle(monarch.getName());
        if (retval == null) {
            retval = monarch.getReigns().stream()
                    .map(Reign::getTitle)
                    .filter(Objects::nonNull)
                    .map(Gender::fromTitle)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }
        if (retval==null) {
            String aiResolved = aiResolverService.findGender(monarch.getName());
            try {
                retval= Gender.valueOf(aiResolved);
            } catch (IllegalArgumentException e) {
                System.out.println("UNKNOWN???");
            }
            System.out.println(monarch.getName() + " defined by AI as " + retval);
        }
        return retval;
    }

}
