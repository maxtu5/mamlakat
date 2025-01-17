package com.tuiken.mamlakat.controller;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.dtos.api.ThroneOperationDto;
import com.tuiken.mamlakat.model.dtos.api.MonarchMediumDto;
import com.tuiken.mamlakat.model.dtos.api.ThroneDto;
import com.tuiken.mamlakat.service.MonarchService;
import com.tuiken.mamlakat.service.ThroneRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/data/thrones")
@CrossOrigin
@RequiredArgsConstructor
public class ThroneWaitroom {

    private final MonarchService monarchService;
    private final ThroneRoom throneRoom;

    @PostMapping(path = "/")
    public List<ThroneDto> findThrones() {
        return throneRoom.findThrones();
    }

    @GetMapping(path = "/{country}")
    public List<MonarchMediumDto> findMonarchsByCountry(@PathVariable Country country) {
        return throneRoom.historyReport(country);
    }

    @PutMapping(path = "/create")
    public ThroneDto createThrone(@RequestBody ThroneOperationDto throne) {
        return throneRoom.createThrone(throne.getCountry(), throne.getLatestMonarchUrl(), throne.getName());
    }

}
