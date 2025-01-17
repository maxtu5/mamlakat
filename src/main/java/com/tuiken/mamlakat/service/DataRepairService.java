package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.builders.PersonBuilder;
import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.*;
import com.tuiken.mamlakat.model.Throne;
import com.tuiken.mamlakat.utils.JsonUtils;
import com.tuiken.mamlakat.utils.RedirectResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataRepairService {

    private final MonarchService monarchService;
    private final MonarchRetriever monarchRetriever;
    private final WikiService wikiService;
    private final ProvenanceService provenanceService;
    private final ThroneRoom throneRoom;
    private final PersonBuilder personBuilder;
    private final FamilyRetriever familyRetriever;
    private final AiResolverService aiResolverService;

    @Transactional
    public boolean reloadReigns(String url, Country country) {
        Monarch monarch = monarchService.findByUrl(url);
        if (monarch != null) {
            JSONArray jsonArray = null;
            try {
                jsonArray = wikiService.read(url);
            } catch (WikiApiException e) {
                return false;
            }
            List<Reign> reigns = monarchRetriever.retrieveReigns(jsonArray, country);
            if (!reigns.isEmpty()) {
                List<Reign> clearedReigns = monarch.getReigns().stream()
                        .filter(r -> !r.getCountry().equals(country))
                        .collect(Collectors.toList());
                clearedReigns.addAll(reigns);
                monarch.setReigns(clearedReigns);
                monarchService.saveMonarch(monarch);
            }
        }
        return false;
    }

    @Transactional
    public void reloadIssue(String url, Country countryJustForAi) throws IOException, URISyntaxException {
        Monarch monarch = monarchService.findByUrl(url);
        if (monarch != null && monarch.getGender()!=null) {
            List<Provenence> provenences = provenanceService.findAllByParent(monarch.getId(), monarch.getGender());
            provenences.forEach(id->{
                Monarch child = monarchService.loadMonarch(id.getId());
                if (child!=null) {
                    System.out.println(child.getName());
                } else {
                    System.out.println("missing child");
                }
            });
            System.out.println();
            JSONArray jsonArray = null;
            List<Monarch> issue = null;
            try {
                jsonArray = wikiService.read(monarch.getUrl());
                issue = familyRetriever.extractIssueFromWikiValidatedWithCreate(jsonArray, monarch, countryJustForAi);
            } catch (WikiApiException e) {
                throw new RuntimeException(e);
            }
            issue.forEach(m-> {
                if (!provenences.stream().map(Provenence::getId).anyMatch(id->id.equals(m.getId()))) {
                    Provenence newProvenence = new Provenence(m.getId());
                    if (monarch.getGender().equals(Gender.MALE))
                        newProvenence.setFather(monarch.getId());
                    else
                        newProvenence.setMother(monarch.getId());
                    provenanceService.save(newProvenence);
                    System.out.println("Adding.." + m.getName());
                }
            });
        }
    }

    @Transactional
    public void printMissingHouses() throws WikiApiException {
        List<Monarch> allPeople = monarchService.loadAllMonarchs();
        Set<String> allHouses = new HashSet<>();
        for (Monarch monarch : allPeople) {
            if (monarch.getHouse().isEmpty()) {
                JSONArray jsonArray = wikiService.read(monarch.getUrl());
                List<JSONObject> list = JsonUtils.arrayTolist(jsonArray);
                List<JSONObject> houseObjects = JsonUtils.drillForName(list, "House", "Dynasty");
                Set<String> houseStrings = JsonUtils.readFromLinks(houseObjects, "text").stream()
                        .map(s -> s.contains("House of") ? s.replace("House of", "").trim() : s)
                        .filter(s -> !s.equalsIgnoreCase("House"))
                        .collect(Collectors.toSet());
                allHouses.addAll(houseStrings);
//                Set<House> houses = new HashSet<>();
//                for (String s : houseStrings) {
//                    House house = House.HouseFromBeginningOfString(s);
//                    if (house != null) houses.add(house);
//                }
//                monarch.setHouse(houses);
//                monarchService.saveMonarch(monarch);
//                System.out.print(i + ". ");
//                houseStrings.forEach(s -> System.out.print(s + " "));
//                System.out.println();
//                i++;
            }
        }
//        System.out.println("of total " + allPeople.size());
        allHouses.forEach(s -> {
            System.out.println(s.toUpperCase() + "(\"" + s + "\'),");
        });
    }

    @Transactional
    public void repairHouses() throws WikiApiException {
        List<Monarch> allPeople = monarchService.loadAllMonarchs();
        for (Monarch monarch : allPeople) {
            if (monarch.getHouse().isEmpty()) {
                JSONArray jsonArray = wikiService.read(monarch.getUrl());
                List<JSONObject> list = JsonUtils.arrayTolist(jsonArray);
                List<JSONObject> houseObjects = JsonUtils.drillForName(list, "House", "Dynasty");
                Set<String> houseStrings = JsonUtils.readFromLinks(houseObjects, "text").stream()
                        .map(s -> s.contains("House of") ? s.replace("House of", "").trim() : s)
                        .filter(s -> !s.equalsIgnoreCase("House"))
                        .collect(Collectors.toSet());
                Set<House> houses = new HashSet<>();
                for (String s : houseStrings) {
                    House house = House.HouseFromBeginningOfString(s);
                    if (house != null) houses.add(house);
                }
                monarch.setHouse(houses);
                monarchService.saveMonarch(monarch);
                houseStrings.forEach(s -> System.out.print(s + " "));
                System.out.println();
            }
        }
    }

    @Transactional
    public void removeDoubles() {
        List<Throne> allThrones = throneRoom.loadAllThrones();
        List<Monarch> allMonarchs = monarchService.loadAllMonarchs();
        List<Provenence> allProvenences = provenanceService.findAllProvenances();
        RedirectResolver resolver = new RedirectResolver();
        Map<String, List<Monarch>> map = allMonarchs.stream()
                .filter(m -> m.getUrl() != null)
//                .filter(m->{
//                    try {
//                        String resolved = resolver.resolve(m.getUrl());
//                        if (!resolved.equals(m.getUrl())) {
//                            System.out.println(m.getUrl() + " " + resolved);
//                            if (allMonarchs.stream().anyMatch(monny->monny.getUrl().equals(resolved))) {
//                                System.out.println("present resolved..."+m.getUrl() + " " + resolved);
//                            } else {
////                                m.setUrl(resolved);
//                                System.out.println("to fix..."+m.getUrl() + " " + resolved);
////                                monarchService.saveMonarch(m);
//                            }
//                            return true;
//                        } else {
//                            return false;
//                        }
//                    } catch (IOException e) {
//                        return  false;
//                    }
//                })
                .collect(Collectors.groupingBy(Monarch::getUrl));
        System.out.println("Total doubles: " + map.entrySet().stream()
                .filter(es -> es.getValue().size() > 1)
                .count());
        map.entrySet().stream()
                .filter(es -> es.getValue().size() > 1)
                .forEach(es -> {
                    System.out.println(es.getKey());
                    for (Monarch monarch : es.getValue()) {
                        System.out.println(monarch.getName() + " " + monarch.getUrl() + monarch.getId().toString());
                        long inProvenences = countProvenences(allProvenences, monarch);
                        long inThrones = countInThrones(allThrones, monarch);
                        System.out.println("found in provenances " + inProvenences + " found in thrones " + inThrones);
                    }

                    System.out.println();
                });

    }

    private long countInThrones(List<Throne> allThrones, Monarch monarch) {
        int sumAll = 0;
        for (Throne throne : allThrones) {
            List<String> monarchsIds = throne.getMonarchsIds();
            int sumCurrent = 0;
            for (int i = 0; i < monarchsIds.size(); i++) {
                String id = monarchsIds.get(i);
                if (id.equals(monarch.getId().toString())) {
                    System.out.println(i + " in " + throne.getName());
                    sumCurrent++;
                }
            }
            sumAll += sumCurrent;
        }
        return (long) sumAll;
    }

    private long countProvenences(List<Provenence> allProvenences, Monarch monarch) {
        return allProvenences.stream()
                .map(provenence -> {
                    if (provenence.getId().equals(monarch.getId())) {
                        Monarch mum = provenence.getMother() != null ? monarchService.loadMonarch(provenence.getMother()) : null;
                        Monarch dad = provenence.getFather() != null ? monarchService.loadMonarch(provenence.getFather()) : null;
                        String mumText = mum != null ? mum.getName() : "null";
                        System.out.println("self");
                        return true;
                    }
                    if (provenence.getMother() != null && provenence.getMother().equals(monarch.getId())) {
                        Monarch child = monarchService.loadMonarch(provenence.getId());
                        System.out.println("mum of " + child.getName());
                        return true;
                    }
                    if (provenence.getFather() != null && provenence.getFather().equals(monarch.getId())) {
                        Monarch child = monarchService.loadMonarch(provenence.getId());
                        System.out.println("dad of " + child.getName());
                        return true;
                    }
                    return false;
                })
                .filter(found -> found)
                .count();
    }

    @Transactional
    public void reportOrderInThrone(Country country) throws IOException, URISyntaxException {
        Throne throne = throneRoom.loadThroneByCountry(country);
        Monarch monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(0)));
        Monarch successor = monarch;
        int lastReignStartYear = monarch.getReigns().stream()
                .filter(r -> r.getCountry().equals(throne.getCountry()))
                .findFirst().orElse(null).getStart()
                .atZone(ZoneId.systemDefault()).getYear();
        for (int i = 1; i < throne.getMonarchsIds().size(); i++) {
            monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(i)));
            if (monarch==null) {
                throne.getMonarchsIds().remove(i);
                throneRoom.saveThrone(throne);
                return;
            }
            if (!monarch.getReigns().stream().anyMatch(r -> r.getCountry().equals(throne.getCountry()))) {
                System.out.println("No rule for country for monarch " + i + " in " + throne.getName() + " " + monarch.getUrl());
            } else {
//                Reign reign = monarch.getReigns().stream()
//                        .filter(r -> r.getCountry().equals(throne.getCountry()))
//                        .findFirst().orElse(null);
                List<Reign> reigns = monarch.getReigns().stream()
                        .filter(r -> r.getCountry().equals(throne.getCountry()))
                        .collect(Collectors.toList());
                Reign rightReign = null;
                for (Reign reign: reigns) {
                    int reignStart = reign.getStart()==null ? 0 : reign.getStart().atZone(ZoneId.systemDefault()).getYear();
                    int reignEnd = reign.getEnd()==null ? 0 : reign.getEnd().atZone(ZoneId.systemDefault()).getYear();

                    if (reignEnd>0 && Math.abs(lastReignStartYear - reignEnd)<3 ||
                            reignEnd>lastReignStartYear && reignStart>0 && reignStart<lastReignStartYear) {
                        rightReign = reign;
                    }
                }

                if (rightReign!=null) {
                    System.out.println("Monarch " + i + " precedes in " + lastReignStartYear + " " +monarch.ruleString(throne.getCountry()));
                    System.out.println(monarch.getUrl());
                    lastReignStartYear = rightReign.getStart().atZone(ZoneId.systemDefault()).getYear();
                } else {
                        System.out.println("Oops.. must be " + lastReignStartYear +
                                " but is " + monarch.ruleString(country) + " for " + monarch.getUrl());
                        lastReignStartYear = monarch.getReigns().stream()
                                .filter(r -> r.getCountry().equals(throne.getCountry()))
                                .findFirst().orElse(null).getStart()
                                .atZone(ZoneId.systemDefault()).getYear();
                        System.out.println("Restarting from " + lastReignStartYear);
                    }
            }
            successor=monarch;
        }
    }

    @Transactional
    public void insertMonarchInThrone(Country country, String urlToInsert, String urlInsertAfter) throws WikiApiException {
        Throne throne = throneRoom.loadThroneByCountry(country);
        Monarch monarchToInsert = monarchService.findByUrl(urlToInsert);
        if (monarchToInsert==null) {
            monarchToInsert = personBuilder.buildPerson(urlToInsert, null);
            if (monarchToInsert==null) return;
            JSONArray jsonArray = wikiService.read(monarchToInsert.getUrl());
            monarchToInsert.setReigns(monarchRetriever.retrieveReigns(jsonArray, country));
            monarchService.saveMonarch(monarchToInsert);
        }
        for (int i=0; i<throne.getMonarchsIds().size(); i++) {
            Monarch monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(i)));
            if (monarch.getUrl().equals(urlInsertAfter)) {
                throne.getMonarchsIds().add(i+1, monarchToInsert.getId().toString());
                throneRoom.saveThrone(throne);
            }
        }
    }

    @Transactional
    public void deleteMonarchInThrone(Country country, String urlToDelete) {
        Throne throne = throneRoom.loadThroneByCountry(country);
        Monarch monarchToInsert = monarchService.findByUrl(urlToDelete);
        if (monarchToInsert==null) {
            System.out.println("not found");
            return;
        }
        for (int i=0; i<throne.getMonarchsIds().size(); i++) {
            Monarch monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(i)));
            if (monarch.getUrl().equals(urlToDelete)) {
                throne.getMonarchsIds().remove(i);
                throneRoom.saveThrone(throne);
            }
        }
    }

    @Transactional
    public int provenanceRemoveNulls() {
        List<Provenence> provenences = new ArrayList<>();
        provenanceService.findAllProvenances().forEach(provenences::add);
        Set<Provenence> toDelete = provenences.stream()
                .filter(p -> {
                    return monarchService.loadMonarch(p.getId()) == null ||
                            p.getFather() != null && monarchService.loadMonarch(p.getFather()) == null ||
                            p.getMother() != null && monarchService.loadMonarch(p.getMother()) == null;
                })
                .collect(Collectors.toSet());
        System.out.println("To remove: " + toDelete.size());
        provenanceService.deleteAll(toDelete);
        return toDelete.size();
    }

    public void provenanceCheckParents() throws WikiApiException {
        List<Provenence> provenences = new ArrayList<>();
        provenanceService.findAllProvenances().forEach(provenences::add);
        List<Provenence> toDelete = new ArrayList<>();
        for (Provenence provenence: provenences) {
            Monarch monarch = monarchService.loadMonarch(provenence.getId());
            if (monarch==null) {
                System.out.println("Monarch null");
                continue;
            }
            JSONArray jsonArray = wikiService.read(monarch.getUrl());
            if (jsonArray==null) {
                System.out.println("JsonArray null");
                continue;
            }
            List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);
            if (infoboxes==null) {
                System.out.println("Infobox null");
                continue;
            }
//            if (provenence.getFather()!=null) {
//                List<JSONObject> father = JsonUtils.drillForName(infoboxes, "Father");
//                if (father == null) {
//                    System.out.println("JsonObject null");
//                    continue;
//                }
//                String fatherUrl = JsonUtils.readFromLinks(father, "url").stream()
//                        .map(familyRetriever::convertChildLink)
//                        .filter(Objects::nonNull)
//                        .findFirst().orElse(null);
//                if (Strings.isBlank(fatherUrl)) {
//                    System.out.println("URL null " + monarch.getUrl());
//                    Monarch daddy = monarchService.loadMonarch(provenence.getFather());
//                    System.out.println("Prov: " + daddy.getUrl());
//                    continue;
//                }
//                RedirectResolver resolver = new RedirectResolver();
//                fatherUrl = resolver.resolve(fatherUrl);
//                Monarch daddy = monarchService.loadMonarch(provenence.getFather());
//                if (!daddy.getUrl().equals(fatherUrl)) {
//                    System.out.println("Wrong father");
//                    System.out.println("Prov: " + daddy.getUrl());
//                    System.out.println("Wiki: " + fatherUrl);
//                    Monarch byUrl = monarchService.findByUrl(fatherUrl);
//                    if (byUrl!=null) {
//                        List<Provenence> allByParent = provenanceService.findAllByParent(byUrl.getId(), Gender.MALE);
//                        if (allByParent.stream().noneMatch(p->p.getId().equals(monarch.getId()))) {
//                            provenence.setFather(byUrl.getId());
//                            provenanceService.save(provenence);
//                            System.out.println("switched");
//                        }
//                    } else {
//                        System.out.println("to delete");
//                        toDelete.add(provenence);
//                    }
//                }
//            }
            if (provenence.getMother()!=null) {
                List<JSONObject> mother = JsonUtils.drillForName(infoboxes, "mother");
                if (mother == null) {
                    System.out.println("JsonObject null");
                    continue;
                }
                String motherUrl = JsonUtils.readFromLinks(mother, "url").stream()
                        .map(familyRetriever::convertChildLink)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
                if (Strings.isBlank(motherUrl)) {
                    System.out.println("URL null " + monarch.getUrl());
                    Monarch mummy = monarchService.loadMonarch(provenence.getMother());
                    System.out.println("Prov: " + mummy.getUrl());
                    continue;
                }
                RedirectResolver resolver = new RedirectResolver();
                motherUrl = resolver.resolve(motherUrl);
                Monarch mummy = monarchService.loadMonarch(provenence.getMother());
                if (!mummy.getUrl().equals(motherUrl)) {
                    System.out.println("Wrong mother");
                    System.out.println("Prov: " + mummy.getUrl());
                    System.out.println("Wiki: " + motherUrl);
                    Monarch byUrl = monarchService.findByUrl(motherUrl);
                    if (byUrl!=null) {
                        List<Provenence> allByParent = provenanceService.findAllByParent(byUrl.getId(), Gender.FEMALE);
                        if (allByParent.stream().noneMatch(p->p.getId().equals(monarch.getId()))) {
                            provenence.setMother(byUrl.getId());
                            provenanceService.save(provenence);
                            System.out.println("switched");
                        }
                    } else {
                        System.out.println("to delete");
                        toDelete.add(provenence);
                    }
                }
            }
            System.out.println();
        }
        provenanceService.deleteAll(new HashSet<>(toDelete));
    }

    public int monarchsReload(String[] urls) {
        for (int i=0; i<urls.length; i+=2) {
            monarchReload(urls[i], urls[i+1]);
        }
        return urls.length/2;
    }

    @Transactional
    private void monarchReload(String tgt, String src) {
        Monarch monarch = monarchService.findByUrl(tgt);
        Monarch right = personBuilder.buildPerson(src, null);
        monarch.setUrl(src);
        monarch.setName(right.getName());
        monarch.setGender(right.getGender());
        monarch.setStatus(PersonStatus.NEW_URL);
        monarch.setBirth(right.getBirth());
        monarch.setDeath(right.getDeath());
        monarch.setHouse(right.getHouse());
        monarchService.saveMonarch(monarch);
    }

    @Transactional
    public void reportGender() {
        List<Monarch> all = monarchService.loadAllMonarchs().stream()
                .filter(m->m.getGender()==null)
                .collect(Collectors.toList());
        Map<String, Long> collect = all.stream()
                .peek(m-> {
                    if (!Strings.isBlank(m.getName())) {
                        System.out.println(m.getName());
                        m.setGender(Gender.fromTitle(m.getName()));
                        if (m.getGender()==null && !m.getReigns().isEmpty() && m.getReigns().get(0).getTitle()!=null) {
                            m.setGender(Gender.fromTitle(m.getReigns().get(0).getTitle()));
                        }
                        if (m.getGender()==null) {
                            String aiResolved = aiResolverService.findGender(m.getName());
                            try {
                                m.setGender(Gender.valueOf(aiResolved));
                            } catch (IllegalArgumentException e) {
                                System.out.println("UNKNOWN???");
                            }
                            System.out.println(m.getName());
                            System.out.println(m.getGender()+"\n");
                        }
                        monarchService.saveMonarch(m);
                    }
                })
                .collect(Collectors.groupingBy(t-> t.getGender()==null? "NULL" : t.getGender().toString(), Collectors.counting()));
        for (Map.Entry<String, Long> es : collect.entrySet()) {
            System.out.println(es.getKey() + " " + es.getValue());
        }
    }
}