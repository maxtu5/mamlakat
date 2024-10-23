package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.House;
import com.tuiken.mamlakat.model.Reign;
import com.tuiken.mamlakat.utils.DatesParser;
import com.tuiken.mamlakat.utils.JsonUtils;
import lombok.AllArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PersonRetriever {

    public String retrieveName(JSONArray jsonArray) {
        Set<String> list = new HashSet<>();
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (object.has("name")) list.add((String) object.get("name"));
        }
//        System.out.println(list.stream().findFirst().get());
        return list.size()>0 ? list.stream().findFirst().get() : null;
    }

    public Instant retrieveOneDate(JSONArray jsonArray, String key) {
        List<JSONObject> list = JsonUtils.arrayTolist(jsonArray);
        List<JSONObject> dates = JsonUtils.drillForName(list, key);

        return dates.stream()
                .map(o -> JsonUtils.readValue(o))
                .filter(Objects::nonNull)
                .map(s -> DatesParser.findDate(s))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public Set<House> retrieveHouses(JSONArray jsonArray) {
        List<JSONObject> list = JsonUtils.arrayTolist(jsonArray);
        List<JSONObject> houseObjects = JsonUtils.drillForName(list, "House", "Dynasty");
        Set<String> houseStrings = JsonUtils.readFromLinks(houseObjects, "text").stream()
                .map(s -> s.contains("House of") ? s.replace("House of", "").trim() : s)
                .filter(s->!s.equalsIgnoreCase("House"))
                .collect(Collectors.toSet());

        Set<House> houses = new HashSet<>();
        for (String s: houseStrings) {
            House house = House.HouseFromBeginningOfString(s);
            if (house!=null) houses.add(house);
        }
        return houses;
    }

    public Reign retrieveReign(JSONArray jsonArray, Country country) {
        List<JSONObject> list = JsonUtils.arrayTolist(jsonArray);
        List<JSONObject> reign = JsonUtils.drillForName(list, "Reign");

        Instant[] datesReign = reign.stream()
                .map(o -> JsonUtils.readValue(o))
                .filter(Objects::nonNull)
                .map(s -> DatesParser.findTwoDates(s))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if (datesReign.length==2) {
            Reign retval = new Reign();
            retval.setStart(datesReign[0]);
            retval.setEnd(datesReign[1]);
            List<JSONObject> coronation = JsonUtils.drillForName(list, "Coronation");

            Instant coronationDate = reign.stream()
                    .map(o -> JsonUtils.readValue(o))
                    .filter(Objects::nonNull)
                    .map(s -> DatesParser.findDate(s))
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            retval.setCoronation(coronationDate);
            retval.setCountry(country);
            return retval;
        }
        return null;
    }
}
