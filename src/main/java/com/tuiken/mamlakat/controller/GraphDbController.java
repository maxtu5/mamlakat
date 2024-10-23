package com.tuiken.mamlakat.controller;

import com.tuiken.mamlakat.model.dtos.api.CopyToGraphTaskDto;
import com.tuiken.mamlakat.service.GraphFirstService;
import com.tuiken.mamlakat.service.ThroneRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/graph")
@CrossOrigin
@RequiredArgsConstructor
public class GraphDbController {

    private final GraphFirstService graphFirstService;
    private final ThroneRoom throneRoom;

    @GetMapping(path = "/e")
    public boolean experimental(@RequestBody CopyToGraphTaskDto copyToGraphTaskDto) throws InterruptedException {
        graphFirstService.copyToGraph(copyToGraphTaskDto);
        return true;
    }
}
