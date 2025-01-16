package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.builders.PersonBuilder;
import com.tuiken.mamlakat.dao.ThroneRepository;
import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.PersonStatus;
import com.tuiken.mamlakat.model.Throne;
import com.tuiken.mamlakat.model.dtos.api.MonarchMediumDto;
import com.tuiken.mamlakat.model.dtos.api.ThroneDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThroneRoom {
    private final ThroneRepository throneRepository;
    private final MonarchService monarchService;
    private final PersonBuilder personBuilder;

    @Transactional
    public ThroneDto createThrone(Country country, String latestMonarchUrl, String name) {
        Throne throne = new Throne();
        throne.setCountry(country);
        throne.setName(name);

        Monarch monarch = personBuilder.findOrCreateOptionalSave(latestMonarchUrl, country, true);
        if (monarch != null && monarch.getId() != null) {
            throne.getMonarchsIds().add(monarch.getId().toString());
            saveThrone(throne);
            return new ThroneDto(throne.getId().toString(), throne.getName());
        }
        return null;
    }

    public Throne saveThrone(Throne throne) {
        return throneRepository.save(throne);
    }

    public Throne loadThroneByCountry(Country country) {
        List<Throne> list = throneRepository.findByCountry(country);
        return list!=null && list.size()==1 ? list.get(0) : null;

    }

    @Transactional
    public void printThrones() {
        throneRepository.findAll().forEach(throne -> {
            List<PersonStatus> statuses = throne.getMonarchsIds().stream()
                    .map(id -> monarchService.loadMonarch(UUID.fromString(id)))
                    .filter(Objects::nonNull)
                    .map(Monarch::getStatus)
                    .collect(Collectors.toList());
            System.out.println("===" + throne.getName() + " of " + throne.getCountry() +
                    "\n=Monarchs: " + throne.getMonarchsIds().size() + ", of them " +
                    statuses.stream().filter(s->s.equals(PersonStatus.RESOLVED)).count() + " resolved");
        });
    }

    public List<Throne> loadAllThrones() {
        List<Throne> retval = new ArrayList<>();
        throneRepository.findAll().forEach(retval::add);
        return retval;
    }

    public List<MonarchMediumDto> historyReport(Country country) {
        List<Throne> list = throneRepository.findByCountry(country);
        if (list==null || list.size()!=1) return null;
        return list.get(0).getMonarchsIds().stream()
                .map(id->{
                    Monarch monarch = monarchService.loadMonarch(UUID.fromString(id));
                    return MonarchMediumDto.builder()
                            .id(monarch.getId().toString())
                            .url(monarch.getUrl())
                            .allReigns(monarch.ruleString(country))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<ThroneDto> findThrones() {
        List<ThroneDto> retval = new ArrayList<>();
        throneRepository.findAll()
                .forEach(t->retval.add(new ThroneDto(t.getId().toString(), t.getName())));
        return retval;
    }

}
