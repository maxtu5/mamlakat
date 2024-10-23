package com.tuiken.mamlakat.controller;

import com.tuiken.mamlakat.exceptions.WikiApiException;
import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.dtos.api.MonarchApiDto;
import com.tuiken.mamlakat.model.dtos.api.UrlDto;
import com.tuiken.mamlakat.service.MonarchService;
import com.tuiken.mamlakat.service.ProvenanceService;
import com.tuiken.mamlakat.service.UnhandledRecordService;
import com.tuiken.mamlakat.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/data/monarchs")
@Slf4j
@CrossOrigin
@RequiredArgsConstructor
public class MonarchController {

    private final MonarchService monarchService;
    private final ProvenanceService provenanceService;
    private final WorkflowService workflowService;
    private final UnhandledRecordService unhandledRecordService;

    @PostMapping(path = "/byurl")
    public MonarchApiDto findMonarchByUrl(@RequestBody UrlDto url) {
        return provenanceService.toApiDtoByUrl(url.getUrl());
    }

    @PostMapping(path = "/update")
    public MonarchApiDto updateMonarch(@RequestBody MonarchApiDto updatedMonarch) {
        return monarchService.updateMonarch(updatedMonarch);
    }

    @DeleteMapping(path = "/delete")
    public List<MonarchApiDto> deleteMonarchs(@RequestBody List<String> toDelete) {
        return monarchService.deleteMonarchs(toDelete);
    }

    @PostMapping(path = "/loadf/{country}/{quantity}")
    public boolean loadFamilyNext(@PathVariable String country, @PathVariable int quantity) throws WikiApiException {
        Country countryObject = Country.valueOf(country);
        for (int i=0; i<quantity; i++) {
            workflowService.resolveFamilyNext(countryObject);
        }
        return true;
    }

    @PostMapping(path = "/loadp/{country}/{quantity}")
    public boolean loadRulerNext(@PathVariable String country, @PathVariable int quantity) throws WikiApiException {
        Country countryObject = Country.valueOf(country);
        for (int i=0; i<quantity; i++) {
            workflowService.addToThroneNext(countryObject);
        }
        return true;
    }

    @GetMapping(path = "/unhandled")
    public int resolveUnhandled() {
        unhandledRecordService.resolve();
        return workflowService.killUnhandled();
    }

}
