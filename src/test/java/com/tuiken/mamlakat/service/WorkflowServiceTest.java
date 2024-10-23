package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.model.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private MonarchService monarchService;

    @Autowired
    private ThroneRoom throneRoom;

    @Autowired
    private UnhandledRecordService unhandledRecordService;

    // CREATE NEW

    @Test
    void createThrone() throws IOException, URISyntaxException {
        UUID uuid = workflowService.createThrone(
                Country.PRUSSIA,
                "https://en.wikipedia.org/wiki/Wilhelm_II",
                "Prussian monarchy");
    }

    // ===== ADD CONTENT ======

    @Test
    public void addToThroneByUrl() throws IOException, URISyntaxException {
        String url = "https://en.wikipedia.org/wiki/Leopold_II,_Holy_Roman_Emperor";
        Country country = Country.TUSCANY;
        workflowService.addToThroneByUrl(url, country);
    }

    @Test
    public void addToThroneNext() throws IOException, URISyntaxException {
        Country country = Country.DENMARK;
        workflowService.addToThroneNext(country);
    }

    @Test
    void addToThroneLoop() throws IOException, URISyntaxException {
        Country country = Country.PRUSSIA;
        workflowService.addToThroneLoop(country);
    }

    @Test
    void resolveFamilyNext() throws URISyntaxException, IOException {
        Country country = Country.DENMARK;
        workflowService.resolveFamilyNext(country);
    }

    @Test
    void repairUnhandledDiacritics() {
        unhandledRecordService.repairUnhandledDiacritics();
    }

    @Test
    void scanUnhandled2() throws IOException, URISyntaxException {
        workflowService.scanUnhandled2();
//        workflowService.killUnhandled();
    }
    // ====== PRINT DATA ========

    @Test
    public void printThrones() {
        throneRoom.printThrones();
    }

    @Test
    public void printMonarchs() {
        Country country = Country.DENMARK;
        workflowService.printAllMonarchs(country);
    }

    @Test
    public void printConcreteMonarch() {
        workflowService.printConcreteMonarch(Country.ENGLAND, 22, true);
    }

    // ====== REPAIR =========

    @Test
    public void repairTitles() throws IOException, URISyntaxException {
        workflowService.repairTitles();
    }

    @Test
    public void occasional() {
        String url = "https://en.wikipedia.org/wiki/Afonso_I_of_Portugal";
        workflowService.occasional(url);
    }

    // ========= CLEANUP ======================

    @Test
    void deleteThrone () {
        Country country = Country.TUSCANY;
        workflowService.deleteEmptyThroneBycountry(country);
    }

    @Test
    void delete() {
        monarchService.deleteById("59084e79-54a7-4a66-b102-b596d67fabab");
    }

    @Test
    void cleanWrongParents() throws IOException, URISyntaxException {
        workflowService.cleanWrongParents();
    }

    @Test
    void cleanCacheFromDoubles() {
        workflowService.cleanCache();
    }


}