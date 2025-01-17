package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.builders.PersonBuilder;
import com.tuiken.mamlakat.dao.ProvenenceRepository;
import com.tuiken.mamlakat.dao.ThroneRepository;
import com.tuiken.mamlakat.dao.WikiCacheRecordRepository;
import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.*;
import com.tuiken.mamlakat.model.Throne;
import com.tuiken.mamlakat.model.dtos.api.ThroneDto;
import com.tuiken.mamlakat.model.workflows.LoadFamilyConfiguration;
import com.tuiken.mamlakat.model.workflows.SaveFamilyConfiguration;
import com.tuiken.mamlakat.utils.JsonUtils;
import com.tuiken.mamlakat.utils.RedirectResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final MonarchRetriever monarchRetriever;
    private final FamilyRetriever familyRetriever;
    private final MonarchService monarchService;
    private final ProvenenceRepository provenenceRepository;
    private final ProvenanceService provenanceService;
    private static final boolean SAVE_FLAG = true;
    private final WikiService wikiService;
    private final WikiCacheRecordRepository wikiCacheRecordRepository;
    private final ThroneRepository throneRepository;
    private final ThroneRoom throneRoom;
    private final UnhandledRecordService unhandledRecordService;
    private final PersonBuilder personBuilder;

    // ============== ADD DATA ========================

    @Transactional
    public void addToThroneLoop(Country country) throws WikiApiException {
        Throne throne = throneRoom.loadThroneByCountry(country);
        if (throne != null && throne.getMonarchsIds().size() > 0) {
            Monarch lastMonarch = monarchService.loadMonarch(
                    UUID.fromString(throne.getMonarchsIds().get(throne.getMonarchsIds().size() - 1)));
            JSONArray jsonArray = wikiService.read(lastMonarch.getUrl());
            String predecessorUrl = monarchRetriever.retrievePredecessor(jsonArray, country);

            while (Strings.isNotBlank(predecessorUrl)) {
                try {
                    Monarch king = personBuilder.findOrCreateOptionalSave(predecessorUrl, country, true);
                    throne.getMonarchsIds().add(king.getId().toString());
                    throneRoom.saveThrone(throne);
                    jsonArray = wikiService.read(king.getUrl());
                    predecessorUrl = monarchRetriever.retrievePredecessor(jsonArray, country);
                } catch (Exception e) {
                    predecessorUrl = "";
                    System.out.println("End");
                }
            }
            throneRoom.saveThrone(throne);
        }
    }

    @Transactional
    public UUID addToThroneNext(Country country) throws WikiApiException {
        Throne throne = throneRoom.loadThroneByCountry(country);
        if (throne != null && throne.getMonarchsIds().size() > 0) {
            Monarch lastMonarch = monarchService.loadMonarch(
                    UUID.fromString(throne.getMonarchsIds().get(throne.getMonarchsIds().size() - 1)));
            System.out.println("Latest ruler is " + lastMonarch.getName());
            JSONArray jsonArray = null;
            try {
                jsonArray = wikiService.read(lastMonarch.getUrl());
            } catch (WikiApiException e) {
                return null;
            }
            String predecessorUrl = monarchRetriever.retrievePredecessor(jsonArray, country);

            if (Strings.isNotBlank(predecessorUrl)) {
                System.out.println("Predecessor found " + predecessorUrl);
                Monarch king = personBuilder.findOrCreateOptionalSave(predecessorUrl, country, true);
                throne.getMonarchsIds().add(king.getId().toString());
                throneRoom.saveThrone(throne);
                return king.getId();
            }
        }
        return null;
    }

    @Transactional
    public ThroneDto addToThroneByUrl(String url, Country country) {
        Throne throne = throneRoom.loadThroneByCountry(country);
        if (throne != null) {
            Monarch monarch = personBuilder.findOrCreateOptionalSave(url, country, true);
            throne.getMonarchsIds().add(monarch.getId().toString());
            throneRoom.saveThrone(throne);
        }
        return new ThroneDto(throne.getId().toString(), throne.getName());
    }

    @Transactional
    public void resolveFamilyNext(Country country, int depth) throws WikiApiException {
        Throne throne = throneRoom.loadThroneByCountry(country);
        if (throne != null && throne.getMonarchsIds().size()>0) {
            List<Monarch> toResolve = new ArrayList<>();
            Monarch m = findFirstToResolve(throne.getMonarchsIds().get(0), depth);
            if (m!= null && m.getStatus().equals(PersonStatus.NEW_URL)) {
                toResolve.add(m);
            }
            int checked =0;
            while (checked < throne.getMonarchsIds().size() - 1 && toResolve.size()==0 && checked<throne.getMonarchsIds().size()) {
                checked++;
                m = findFirstToResolve(throne.getMonarchsIds().get(checked), depth);
                if (m!=null && m.getStatus().equals(PersonStatus.NEW_URL)) {
                    toResolve.add(m);
                }
            }
            if (toResolve.size()>0) {
                Monarch latest = toResolve.get(0);
                System.out.println(String.format("\n+++ Loading family for %s +++", latest.getName()));

                Monarch simplified = new Monarch(latest.getUrl());
                simplified.setId(latest.getId());
                simplified.setName(latest.getName());
                simplified.setGender(latest.getGender());
                simplified.setBirth(latest.getBirth());
                simplified.setDeath(latest.getDeath());

                LoadFamilyConfiguration configuration = familyRetriever.createLoadFamilyConfiguration(
                        simplified, country);
                configuration.print();

                try {
                    SaveFamilyConfiguration saveConfig = familyRetriever.retrieveFamily(configuration);
                    saveConfig.print();

                    if (SAVE_FLAG) {
                        provenanceService.saveFamily(saveConfig);
                        monarchService.saveStatus(latest.getId(), PersonStatus.RESOLVED);
                    }
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Crashed");
                }
            }
        }
    }

    private Monarch findFirstToResolve(String id, int depth) {
        Monarch monarch = monarchService.loadMonarch(UUID.fromString(id));
        if (monarch.getStatus().equals(PersonStatus.NEW_URL)) {
            return monarch;
        }
        int level = 1;
        List<Monarch> previousLevel = new ArrayList<>();
        previousLevel.add(monarch);
        while (level<=depth) {
            List<Monarch> newPreviousLevel = new ArrayList<>();
            for (Monarch current: previousLevel) {
                Monarch father = provenanceService.findFather(current);
                if (father!=null) {
                    if (father.getStatus().equals(PersonStatus.NEW_URL)) {
                        return father;
                    }
                    newPreviousLevel.add(father);
                }
                Monarch mother = provenanceService.findMother(current);
                if (mother!=null) {
                    if (mother.getStatus().equals(PersonStatus.NEW_URL)) {
                        return mother;
                    }
                    newPreviousLevel.add(mother);
                }
                Set<Monarch> childeren = provenanceService.findChildren(monarch);
                for (Monarch child: childeren) {
                    if (child.getStatus().equals(PersonStatus.NEW_URL)) {
                        return child;
                    }
                    newPreviousLevel.add(child);
                }
            }
            previousLevel = newPreviousLevel;
            level++;
        }
        return null;
    }


//    @Transactional
//    public void loadHousesForMonarchy(Country country) throws IOException, URISyntaxException {
//        Monarchy monarchy = monarchyService.loadMonarchyByCountry(country);
//        if (monarchy==null) return;
//        List<Monarch> monarchs = monarchy.getMonarchs();
//        System.out.println(String.format("=== Loading houses for monarchs in %s of %s ===", monarchy.getName(), country.toString()));
//        for (int i = 0; i<monarchs.size(); i++) {
//            Monarch monarch = monarchs.get(i);
//            if (monarch.getHouse().isEmpty()) {
//                System.out.println(String.format("Monarch %s, %s\n%s", i, monarch.toString(country), monarch.getUrl()));
//                monarchRetriever.readMonarchOnlyHouse(monarch, country);
//                monarchService.saveMonarch(monarch);
//            }
//        }
//    }



    // ============ PRINT =============

    @Transactional
    public void printAllMonarchs(Country country) {
        Throne throne = throneRepository.findByCountry(country).stream().findFirst().orElse(null);
        if (throne != null) {
            List<String> monarchs = throne.getMonarchsIds();
            System.out.println(String.format("=== Listing all %s monarchs in %s of %s ===", monarchs.size(), throne.getName(), country.toString()));
            for (int i = 0; i < monarchs.size(); i++) {
                Monarch monarch = monarchService.loadMonarch(UUID.fromString(monarchs.get(i)));
                if (monarch==null) {
                    System.out.println("Oops..the monarch " + i + " is missing");
                } else {
                    printMonarch(monarch, country, true);
                }
            }
        }
    }

    @Transactional
    public void printConcreteMonarch(Country country, int index, boolean family) {
        Throne monarchy = throneRoom.loadThroneByCountry(country);
        if (monarchy == null || monarchy.getMonarchsIds() == null || monarchy.getMonarchsIds().size() < index + 1)
            return;
        Monarch monarch = monarchService.loadMonarch(UUID.fromString(monarchy.getMonarchsIds().get(index)));
        printMonarch(monarch, country, family);
    }

    private void printMonarch(Monarch monarch, Country country, boolean family) {
        System.out.println(monarch.toString(country));

        Provenence provenence = provenenceRepository.findById(monarch.getId()).orElse(null);
        if (provenence != null) {
            if (provenence.getFather() != null) {
                Monarch father = monarchService.loadMonarch(provenence.getFather());
                if (father==null) System.out.println(provenence.getFather());
                System.out.println(String.format("= Father: %s, (%s)",
                        father.getName(),
                        String.join(", ", father.getHouse().stream().map(h -> h.toString()).collect(Collectors.toList()))));
            }
            if (provenence.getMother() != null) {
                Monarch mother = monarchService.loadMonarch(provenence.getMother());
                System.out.println(String.format("= Mother: %s, (%s)",
                        mother.getName(),
                        String.join(", ", mother.getHouse().stream().map(h -> h.toString()).collect(Collectors.toList()))));
            }
        }

        List<Provenence> children = monarch.getGender() == Gender.MALE ?
                provenenceRepository.findByFather(monarch.getId()) :
                provenenceRepository.findByMother(monarch.getId());
        int i = 1;
        for (Provenence childProvenance : children) {
            Monarch child = monarchService.loadMonarch(childProvenance.getId());
            if (child!=null) {
                System.out.println(String.format("= Child %s: %s, (%s)",
                        i,
                        child.getName(),
                        String.join(", ", child.getHouse().stream().map(h -> h.toString()).collect(Collectors.toList()))));
                i++;
            } else {
                System.out.println(String.format("= Child %s: null",i));
            }
        }

    }

    //    @Transactional
//    public void printAllPeople() {
//        log.info("=========== People stats ============");
//        List<Monarchy> monarchies = monarchyService.loadAllMonarchies();
//        Set<UUID> monarchsInMonarchies = monarchies.stream()
//                .filter(m->m.getMonarchs()!=null)
//                .flatMap(m->m.getMonarchs().stream())
//                .filter(Objects::nonNull)
//                .filter(m->m.getId()!=null)
//                .map(Monarch::getId)
//                .collect(Collectors.toSet());
//        List<Monarch> monarchs = monarchService.loadAllMonarchs();
//        List<Monarch> nonRoyals = monarchs.stream()
//                .filter(m->!monarchsInMonarchies.contains(m.getId()))
//                .collect(Collectors.toList());
//        log.info(String.format("=== Royals: %s", monarchs.size()-nonRoyals.size()));
//        log.info(String.format("=== Non-royals: %s", nonRoyals.size()));
//        log.info(String.format("=== Total: %s", monarchs.size()));
//    }
    // ============ Cleanup ==============
    @Transactional
    public void deleteEmptyThroneBycountry(Country country) {
        Throne throne = throneRepository.findByCountry(country).stream()
                .filter(t -> t.getMonarchsIds() == null || t.getMonarchsIds().isEmpty())
                .findFirst().orElse(null);
        if (throne != null) {
            throneRepository.delete(throne);
        }
    }

    public void cleanWrongParents() {
        List<Provenence> provenences = new ArrayList<>();
        provenenceRepository.findAll().forEach(provenences::add);

        for (Provenence provenence : provenences) {
            Monarch monarch = monarchService.loadMonarch(provenence.getId());
            JSONArray jsonArray = null;
            try {
                jsonArray = wikiService.read(monarch.getUrl());
            } catch (WikiApiException e) {
                return;
            }
            List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);

            if (provenence.getFather() != null) {
                Monarch fatherO = monarchService.loadMonarch(provenence.getFather());
                List<JSONObject> father = JsonUtils.drillForName(infoboxes, "Father");
                String fatherUrl = JsonUtils.readFromLinks(father, "url").stream()
                        .map(familyRetriever::convertChildLink)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
                if (fatherUrl == null) {
                    System.out.println("Wiki father not found at " + monarch.getName());
                } else {
                    RedirectResolver resolver = new RedirectResolver();
                    fatherUrl = resolver.resolve(fatherUrl);
                    if (!fatherUrl.equals(fatherO.getUrl())) {
                        System.out.println("Wrong father at " + monarch.getName());
                        System.out.println("Wiki " + fatherUrl + " Data " + fatherO.getUrl());
                        System.out.println();
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteFamily(UUID id, Set<UUID> royals) {

        Monarch monarch = monarchService.loadMonarch(id);

        Set<UUID> family = new HashSet<>();

        Provenence provenence = provenenceRepository.findById(id).orElse(null);
        if (provenence != null) {
            if (provenence.getFather() != null) family.add(provenence.getFather());
            if (provenence.getMother() != null) family.add(provenence.getMother());
        }

        if (monarch.getGender().equals(Gender.MALE)) {
            family.addAll(provenenceRepository.findByFather(id).stream().map(Provenence::getId).collect(Collectors.toList()));
        } else if (monarch.getGender().equals(Gender.FEMALE)) {
            family.addAll(provenenceRepository.findByMother(id).stream().map(Provenence::getId).collect(Collectors.toList()));
        }

        List<UUID> uuidList = family.stream()
                .filter(i -> !royals.contains(i))
                .collect(Collectors.toList());

        uuidList.stream()
                .map(UUID::toString)
                .forEach(monarchService::deleteById);

        monarch.setStatus(PersonStatus.NEW_URL);
        monarchService.saveMonarch(monarch);
    }

    // ==== issue issue ====

    public void cleanCache() {
        List<WikiCacheRecord> all = new ArrayList<>();
        wikiCacheRecordRepository.findAll().forEach(all::add);
        List<WikiCacheRecord> allS = new ArrayList<>();
        List<UUID> uuids = all.stream()
                .map(rec -> {
                    String found = allS.stream()
                            .map(r -> r.getUrl())
                            .filter(u -> u.equals(rec.getUrl()))
                            .findAny().orElse(null);
                    if (found == null) {
                        allS.add(rec);
                        return null;
                    } else {
                        return rec.getCacheId();
                    }
                })
                .filter(Objects::nonNull).toList();
        uuids.forEach(u -> System.out.println(u.toString()));
        wikiCacheRecordRepository.deleteAllById(uuids);
    }

    private void printMon(UUID m) {
        Monarch monarch = monarchService.loadMonarch(m);
        System.out.println(monarch.getId().toString() + " " + monarch.getUrl());
    }

    @Transactional
    public void repairTitles() throws IOException, URISyntaxException {
        List<Monarch> allPeople = monarchService.loadAllMonarchs();
        allPeople = allPeople.stream()
                .filter(m -> !m.getReigns().isEmpty())
                .collect(Collectors.toList());

        for (int j = 0; j < allPeople.size(); j++) {
            Monarch m = allPeople.get(j);
            if (!m.getReigns().isEmpty()) {
                for (int i = 0; i < m.getReigns().size(); i++) {
                    if (Strings.isBlank(m.getReigns().get(i).getTitle())) {
                        try {
                            JSONArray jsonArray = wikiService.read(m.getUrl());
                            String title = monarchRetriever.retrieveTitle(jsonArray, m.getReigns().get(i).getCountry());
                            System.out.println(title);
                            if (title != null) {
                                System.out.println("Repaired");
                                m.getReigns().get(i).setTitle(title);
                            }
                        } catch (Exception e) {
                            System.out.println("Oops");
                        }
                    }
                }
                monarchService.saveMonarch(m);
            }
        }
    }

    @Transactional
    public void occasional(String url) {
        Monarch monarch = monarchService.findByUrl(url);
        monarch.getReigns().remove(1);
        monarchService.saveMonarch(monarch);
    }

    public int killUnhandled() {
        return unhandledRecordService.deleteKilled();
    }

    public void scanUnhandled2() throws IOException, URISyntaxException {
        List<UnhandledRecord> unhandledRecords = unhandledRecordService.loadAll();
        List<String> strings = unhandledRecords.stream()
                .filter(r -> r.getChildUrl().contains("%C3%B6") || r.getChildUrl().contains("%C3%A9"))
                .map(r -> {
                    r.setChildUrl(r.getChildUrl()
                            .replaceAll("%C3%B6", "ö")
                            .replaceAll("%C3%A9", "é")
                            .replaceAll("%C3%AD", "í"));
                    return r;
                })
                .map(r -> {
                    Monarch parent = monarchService.findByUrl(r.getParentUrl());
                    if (parent != null) {
                        Monarch child = personBuilder.buildPerson(r.getChildUrl(), null);
                        monarchService.saveMonarch(child);
                        Provenence provenence = provenanceService.findById(child.getId());
                        if (provenence == null) {
                            provenence = new Provenence(child.getId());
                            if (parent.getGender().equals(Gender.MALE)) {
                                provenence.setFather(parent.getId());
                            }
                            if (parent.getGender().equals(Gender.FEMALE)) {
                                provenence.setMother(parent.getId());
                            }
                            provenanceService.save(provenence);
                        }
                        r.setSolution("kill");
                    }
                    return r.getChildUrl();
                })
//                .map(UnhandledRecord::getChildUrl)
                .collect(Collectors.toList());
        strings.forEach(System.out::println);
    }
}
