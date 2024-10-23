package com.tuiken.mamlakat.controller;

import com.tuiken.mamlakat.service.DataRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/repair")
@CrossOrigin
@RequiredArgsConstructor
public class RepairController {

    private final DataRepairService dataRepairService;

    @GetMapping(path = "/prov/removenull")
    public int provenanceRemoveNulls() {
        return dataRepairService.provenanceRemoveNulls();
    }

    @GetMapping(path = "/prov/checkparents")
    public void provenanceCheckParents() throws IOException, URISyntaxException {
        dataRepairService.provenanceCheckParents();
    }

    @PostMapping(path = "/monarch/reload")
    public int monarchReload(@RequestBody String[] urls) {
        if (urls.length % 2 != 0 || urls.length==0) return 0;
        return dataRepairService.monarchsReload(urls);
    }


}
