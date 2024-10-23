package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.model.*;
import com.tuiken.mamlakat.utils.DatesParser;
import com.tuiken.mamlakat.utils.JsonUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonarchRetriever {

    DatesParser datesParser = new DatesParser();

    private final WikiService wikiService;
    private final PersonRetriever personRetriever;
    private final MonarchService monarchService;

    public String retrievePredecessor(JSONArray jsonArray, Country country) throws IOException, URISyntaxException {
        List<JSONObject> list = JsonUtils.extendParts(JsonUtils.readInfoboxes(jsonArray));
        list = list.stream().filter(o -> o.has("name") && country.belongs((String) o.get("name"))).collect(Collectors.toList());
        if (list.size() > 1) {
            list = list.stream()
                    .filter(o -> {
                        List<JSONObject> lissi = new ArrayList<>();
                        lissi.add(o);
                        return JsonUtils.drillForName(lissi, "Reign").size() > 0;
                    })
                    .collect(Collectors.toList());
        }

        List<JSONObject> predecessor = JsonUtils.drillForName(list, "Predecessor");

        if (predecessor.size() == 1) {
            return JsonUtils.readFromLinks(predecessor, "url").get(0);
        }
        return null;
    }

    public List<Reign> retrieveReigns(JSONArray jsonArray, Country country) throws IOException, URISyntaxException {

        List<JSONObject> list = JsonUtils.extendParts(JsonUtils.readInfoboxes(jsonArray));
        List<Reign> retval = new ArrayList<>();

        List<JSONObject> reign = findByNameAndDrill(list, country, "Reign");
        if (reign.isEmpty()) {
            reign = findByNameAndDrill(list, country, "1st reign");
            reign.addAll(findByNameAndDrill(list, country, "2nd reign"));
        }
        for (int i = 0; i < reign.size(); i++) {
            String reignline = JsonUtils.readValue(reign.get(i));
            Instant[] reignDates = datesParser.findTwoDates(reignline);
            Reign r = new Reign();
            r.setCountry(country);
            r.setStart(reignDates[0]);
            r.setEnd(reignDates[1]);
            List<JSONObject> corona = JsonUtils.drillForName(list, "Coronation");
            if (corona.size() == 1) {
                String coronationLine = JsonUtils.readValue(corona.get(0));
                r.setCoronation(datesParser.findDate(coronationLine));
            }
            r.setTitle(retrieveTitle(jsonArray, country));
            retval.add(r);
        }
        return retval;
    }

    public List<JSONObject> findByNameAndDrill(List<JSONObject> list, Country country, String keyword) {
        list = list.stream().filter(o -> o.has("name") && country.belongs((String) o.get("name"))).collect(Collectors.toList());
        if (list.size() > 1) {
            list = list.stream()
                    .filter(o -> {
                        List<JSONObject> lissi = new ArrayList<>();
                        lissi.add(o);
                        return JsonUtils.drillForName(lissi, keyword).size() > 0;
                    })
                    .collect(Collectors.toList());
        }
        return JsonUtils.drillForName(list, keyword);
    }

    public String retrieveTitle(JSONArray jsonArray, Country country) throws IOException, URISyntaxException {
        List<JSONObject> list = JsonUtils.extendParts(JsonUtils.readInfoboxes(jsonArray));
        list = list.stream().filter(o -> o.has("name") && country.belongs((String) o.get("name"))).collect(Collectors.toList());
        if (list.size() > 1) {
            list = list.stream()
                    .filter(o -> {
                        List<JSONObject> lissi = new ArrayList<>();
                        lissi.add(o);
                        return JsonUtils.drillForName(lissi, "Reign").size() > 0;
                    })
                    .collect(Collectors.toList());
        }

        System.out.println("==found Title " + list.size());
        if (list.size() == 1) {
            return (String) list.get(0).get("name");
        }
        return null;
    }

    @Transactional
    public void repairMonarch(String monarchId, Country country) throws IOException, URISyntaxException {

        Monarch person = monarchService.loadMonarch(UUID.fromString(monarchId));
        if (person != null) {
            JSONArray jsonArray = wikiService.read(person.getUrl());
            List<JSONObject> infoboxes = JsonUtils.arrayTolist(JsonUtils.readInfoboxes(jsonArray).get(0).getJSONArray("has_parts"));
//            if (infoboxes.size()>1) {
//                person.setTitle(infoboxes.get(1).getString("name"));
//            }
            person.setName(personRetriever.retrieveName(jsonArray));
            person.setGender(Gender.fromTitle(person.getName()));
            person.setBirth(personRetriever.retrieveOneDate(jsonArray, "Born"));
            person.setDeath(personRetriever.retrieveOneDate(jsonArray, "Died"));
            person.setHouse(personRetriever.retrieveHouses(jsonArray));
            person.setStatus(PersonStatus.NEW_URL);

            Reign reign = personRetriever.retrieveReign(jsonArray, country);
            List<Reign> reigns = new ArrayList<>();
            reigns.add(reign);
            person.setReigns(reigns);
            Set<Country> countries = new HashSet<>();
            countries.add(country);
            person.setCountry(countries);
            monarchService.saveMonarch(person);
        }
    }
}