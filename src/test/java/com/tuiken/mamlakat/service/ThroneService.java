package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.ThroneRepository;
import com.tuiken.mamlakat.model.Throne;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThroneService {

    private final ThroneRepository throneRepository;

    public List<Throne> findAll() {
        List<Throne> thrones = new ArrayList<>();
        throneRepository.findAll().forEach(thrones::add);
        return thrones;
    }
}
