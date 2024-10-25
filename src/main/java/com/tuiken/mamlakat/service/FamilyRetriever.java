package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.builders.PersonBuilder;
import com.tuiken.mamlakat.dao.ProvenenceRepository;
import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.*;
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
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyRetriever {

    private static final String SIMPLE_URL_PREFIX = "https://simple.wikipedia.org/wiki/";
    private static final String NORMAL_URL_PREFIX = "https://en.wikipedia.org/wiki/";
    private static final boolean SAVE_FLAG = true;

    private final MonarchService monarchService;
    private final ProvenenceRepository provenenceRepository;
    private final WikiService wikiService;
    private final AiResolverService aiResolver;
    private final UnhandledRecordService unhandledRecordService;
    private final PersonBuilder personBuilder;

    public LoadFamilyConfiguration createLoadFamilyConfiguration(Monarch root, Country rootCountry)
            throws WikiApiException {

        LoadFamilyConfiguration configuration = LoadFamilyConfiguration.builder()
                .rootId(root.getId())
                .rootUrl(root.getUrl())
                .rootGender(root.getGender())
                .build();

        JSONArray jsonArray = wikiService.read(root.getUrl());
        List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);

        // parents
        Provenence provenence = provenenceRepository.findById(root.getId()).orElse(null);
        if (provenence!=null) {
            if (provenence.getFather()!=null) configuration.setFatherId(provenence.getFather());
            if (provenence.getMother()!=null) configuration.setMotherId(provenence.getMother());
        }

        List<JSONObject> father = JsonUtils.drillForName(infoboxes, "Father");
        String fatherUrl = JsonUtils.readFromLinks(father, "url").stream()
                .map(this::convertChildLink)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        RedirectResolver resolver = new RedirectResolver();
        if (fatherUrl!=null) {
            fatherUrl = resolver.resolve(fatherUrl);
            configuration.setFatherUrl(fatherUrl);
        }

        List<JSONObject> mother = JsonUtils.drillForName(infoboxes, "Mother");
        String motherUrl = JsonUtils.readFromLinks(mother, "url").stream()
                .map(this::convertChildLink)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        if (motherUrl!=null) {
            motherUrl = resolver.resolve(motherUrl);
            configuration.setMotherUrl(motherUrl);
        }
        // children
        List<Provenence> issueP = root.getGender() ==Gender.MALE ?
                provenenceRepository.findByFather(root.getId()) :
                provenenceRepository.findByMother(root.getId());
        configuration.setIssueIds(issueP.stream()
                .map(p->p.getId())
                .collect(Collectors.toList()));

        List<Monarch> childrenSaved = extractIssueFromWikiValidatedWithCreate(jsonArray, root, rootCountry);
        configuration.setIssueUrls(childrenSaved.isEmpty() ? null : childrenSaved.stream().map(Monarch::getUrl).collect(Collectors.toList()));
        return configuration;
    }

    public List<Monarch> extractIssueFromWikiValidatedWithCreate(JSONArray jsonArray, Monarch root, Country rootCountry) throws WikiApiException {
        List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);
        List<JSONObject> issue = JsonUtils.drillForName(infoboxes, "Issue detail", "Issue", "Issue more...", "Illegitimate children Detail", "Issue among others...", "Illegitimate children more...");

        List<String> issueUrls = JsonUtils.readFromLinks(issue, "url").stream()
                .map(this::convertChildLink)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!issueUrls.isEmpty()) {
            System.out.println("Wow, found it in simple way...");
            List<Monarch> retval = issueUrls.stream()
                    .map(u -> personBuilder.findOrCreateOptionalSave(u, null, true))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            System.out.println("* simple " + retval.size() + "/" + issueUrls.size());
            return retval;
        } else {
            return smartExtractWithCreate(jsonArray, root, rootCountry);
        }
    }

    private List<Monarch> smartExtractWithCreate(JSONArray jsonArray, Monarch root, Country rootCountry) throws WikiApiException {
        List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);
        List<JSONObject> issue = JsonUtils.drillForName(infoboxes, "Issue detail", "Issue", "Issue more...", "Illegitimate children Detail", "Issue among others...", "Illegitimate children more...");
        Set<String> allLinks = JsonUtils.readAllLinks(jsonArray).stream()
                .map(this::convertChildLink)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> names = JsonUtils.readFromValues(issue).stream()
                .filter(s->!s.equals("Illegitimate:"))
                .collect(Collectors.toList());

        int fuzzy = 0;
        int ai = 0;

        List<Monarch> retval = new ArrayList<>();
        for (String name : names) {
            String foundUrl = fuzzyFind(name, allLinks);
            if (!Strings.isBlank(foundUrl)) {
                Monarch newPerson = personBuilder.findOrCreateOptionalSave(foundUrl, null, true);
                if (newPerson!=null) {
                    fuzzy++;
                    retval.add(newPerson);
                }
            } else {
                foundUrl = aiResolver.findChild(name, root.getName(), rootCountry);
                if (!Strings.isBlank(foundUrl)) {
                    Monarch newPerson = personBuilder.findOrCreateOptionalSave(foundUrl, null, false);
                    if (newPerson != null && checkParent(newPerson, root)) {
                        ai++;
                        retval.add(newPerson);
                    } else {
                        saveUnhandledRecord(name, foundUrl, root.getUrl());
                    }
                } else {
                    saveUnhandledRecord(name, "", root.getUrl());
                }
            }
        }
        System.out.println("* fuzzy " + fuzzy + "/" + names.size() + ", ai " + ai + "/" + names.size());
        return retval;
    }

    private boolean checkParent(Monarch monarch, Monarch parent) throws WikiApiException {
        JSONArray jsonArray = wikiService.read(monarch.getUrl());
        List<JSONObject> infoboxes = JsonUtils.readInfoboxes(jsonArray);
        if (parent.getGender().equals(Gender.MALE)) {
            List<JSONObject> father = JsonUtils.drillForName(infoboxes, "Father");
            String fatherUrl = JsonUtils.readFromLinks(father, "url").stream()
                    .map(this::convertChildLink)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            RedirectResolver resolver = new RedirectResolver();
            fatherUrl = resolver.resolve(fatherUrl);
            return parent.getUrl().equals(fatherUrl);
        } else {
            List<JSONObject> mother = JsonUtils.drillForName(infoboxes, "Mother");
            String motherUrl = JsonUtils.readFromLinks(mother, "url").stream()
                    .map(this::convertChildLink)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            RedirectResolver resolver = new RedirectResolver();
            motherUrl = resolver.resolve(motherUrl);
            return parent.getUrl().equals(motherUrl);
        }
    }

    private void saveUnhandledRecord(String name, String childUrl, String parentUrl) {
        UnhandledRecord unhandledRecord = new UnhandledRecord();
        unhandledRecord.setChild(name);
        unhandledRecord.setParentUrl(parentUrl);
        unhandledRecord.setChildUrl(childUrl);
        unhandledRecordService.save(unhandledRecord);
    }

    private String fuzzyFind(String name, Set<String> allNames) {

        String[] tokens_sample = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll(",", "")
                .split(" ");

        for (String path : allNames) {
            String[] searched = Normalizer.normalize(path, Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replace(NORMAL_URL_PREFIX, "")
                    .replaceAll(",", "")
                    .split("_");

            int notFound = 0;
            for (int i=0; i<tokens_sample.length; i++) {
                String tofind = tokens_sample[i];
                if (Arrays.stream(searched).noneMatch(ss->ss.equalsIgnoreCase(tofind))) {
                    notFound++;
                }
            }
            if (notFound<1) return path;
        }
        return null;
    }

    public SaveFamilyConfiguration retrieveFamily(LoadFamilyConfiguration configuration) throws IOException, URISyntaxException {
        System.out.println("=== LOADING ===");
        SaveFamilyConfiguration updates = new SaveFamilyConfiguration();

        if (configuration.getRootId()== null || configuration.getRootUrl()==null ||
                configuration.getIssueUrls()!=null && configuration.getRootGender()==null)
            return updates;

        // parents
        Provenence provenence = new Provenence(configuration.getRootId());
        if (configuration.getFatherId() == null && configuration.getFatherUrl() != null) {
            UUID father = extractRelative(configuration.getFatherUrl(), Gender.MALE, configuration.getRootId());
            if (father!=null) {
                provenence.setFather(father);
            }
        }
        if (configuration.getMotherId() == null && configuration.getMotherUrl() != null) {
            UUID mother = extractRelative(configuration.getMotherUrl(), Gender.FEMALE, configuration.getRootId());
            if (mother!=null) {
                provenence.setMother(mother);
            }
        }
        if (provenence.getFather()!=null || provenence.getMother()!=null) {
            updates.getToCreate().add(provenence);
        }

        // children
        if (configuration.getIssueUrls()==null || configuration.getIssueUrls().isEmpty() ||
                configuration.getIssueIds()!=null && configuration.getIssueIds().size() >= configuration.getIssueUrls().size())
            return updates;

        List<UUID> issue = new ArrayList<>();
        for (String url : configuration.getIssueUrls()) {
            UUID uuid = extractRelative(url, null, configuration.getRootId());
            if (uuid!=null) issue.add(uuid);
        }

        for (UUID uuid: issue) {
            if (!configuration.getIssueIds().contains(uuid)) {
                updates.getToCreate().add(configuration.getRootGender()==Gender.MALE ?
                                Provenence.builder().id(uuid).father(configuration.getRootId()).build() :
                                Provenence.builder().id(uuid).mother(configuration.getRootId()).build());
            }
        }

        return updates;
    }

    @Transactional
    UUID extractRelative(String url, Gender gender, UUID relationId) {
        Monarch monarch = null;
        try {
            monarch = monarchService.findByUrl(url);
            if (monarch==null) {
                monarch = personBuilder.buildPerson(url, gender);
            }
        } catch (Exception e) {
            System.out.println("!!! Failure: " + url);
            return null;
        }
        if (monarch!=null) {
            if (monarch.getId()==null) {
                System.out.println("== Saving: " + monarch.getUrl());
                if (SAVE_FLAG) {
                    save(monarch, relationId);
                }
            } else {
                System.out.println("== Exists: " + monarch.getUrl());
            }
            monarch.print();
            return monarch.getId();
        }
        return null;
    }

    private void save(Monarch existParent, UUID relationId) {
        if (existParent != null) {
            monarchService.save(existParent);
        }
    }


    public String convertChildLink(String src) {
        if (src.contains("#") || src.contains("(genealogy)") || !src.startsWith(SIMPLE_URL_PREFIX) && !src.startsWith(NORMAL_URL_PREFIX)) {
            return null;
        }
        String retval = src.contains("?") ? src.substring(0, src.indexOf("?")) : src;
//        retval = Normalizer.normalize(retval, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        if (retval.startsWith(SIMPLE_URL_PREFIX)) {
                String[] tokens = retval.split("/");
                retval = NORMAL_URL_PREFIX + tokens[tokens.length-1];
        }
        return retval;
    }

}