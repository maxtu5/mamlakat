package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.Country;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataRepairServiceTest {

    @Autowired
    private DataRepairService dataRepairService;

    @Test
    public void reportOrderInThrones() throws IOException, URISyntaxException {
        Country country = Country.NORWAY;
        dataRepairService.reportOrderInThrone(country);
    }

    @Test
    public void reloadReigns() {
        Country country = Country.SPAIN;
        String url = "https://en.wikipedia.org/wiki/Christian_II_of_Denmark";
        dataRepairService.reloadReigns(url, country);
    }

    @Test
    public void reloadIssue() throws IOException, URISyntaxException {
        String url = "https://en.wikipedia.org/wiki/Haakon_IV";
        Country country = Country.NORWAY;
        dataRepairService.reloadIssue(url, country);
    }

    @Test
    public void repairHouses1() throws WikiApiException {
        dataRepairService.printMissingHouses();
    }

    @Test
    public void repairHouses2() throws WikiApiException {
        dataRepairService.repairHouses();
    }

    @Test
    public void removeDoubles() {
        dataRepairService.removeDoubles();
    }

    @Test
    public void insertMonarchInThrone() throws WikiApiException {
        Country country = Country.SPAIN;
        String urlToInsert = "https://en.wikipedia.org/wiki/Philip_V_of_Spain";
        String urlInsertAfter = "https://en.wikipedia.org/wiki/Louis_I_of_Spain";
        dataRepairService.insertMonarchInThrone(country, urlToInsert, urlInsertAfter);
    }

    @Test
    public void deleteMonarchInThrone() {
        Country country = Country.SWEDEN;
        String urlToDelete = "https://en.wikipedia.org/wiki/Margaret_I_of_Denmark";
        dataRepairService.deleteMonarchInThrone(country, urlToDelete);
    }

    @Test
    public void reportGender() {
        dataRepairService.reportGender();
    }

}