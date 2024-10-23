package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.builders.PersonBuilder;
import com.tuiken.mamlakat.dao.UnhandledRecordRepository;
import com.tuiken.mamlakat.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnhandledRecordService {

    private final UnhandledRecordRepository unhandledRecordRepository;
    private final MonarchService monarchService;
    private final PersonBuilder personBuilder;
    private final ProvenanceService provenanceService;

    @Transactional
    public UnhandledRecord save(UnhandledRecord monarch) {
        return unhandledRecordRepository.save(monarch);
    }

    @Transactional
    public void resolve() {
        List<UnhandledRecord> unhandledRecords = new ArrayList<>();
        unhandledRecordRepository.findAll().forEach(unhandledRecords::add);
        List<UUID> toKill = unhandledRecords.stream()
                .filter(r -> r.getSolution() != null)
                .map(r->{
                    if (!r.getSolution().equals("kill")) {
                        Monarch monarch = monarchService.findByUrl(URLDecoder.decode(r.getSolution()));
                        if (monarch==null) {
                            monarch = personBuilder.buildPerson(r.getSolution(), null);
                            if (monarch==null) return r;
                            monarch.setGender(Gender.fromTitle(monarch.getName()));
                            monarchService.saveMonarch(monarch);
                        }
                        Provenence provenence = provenanceService.findById(monarch.getId());
                        if (provenence==null) {
                            provenence = new Provenence(monarch.getId());
                            Monarch parent = monarchService.findByUrl(r.getParentUrl());
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
                    return r;
                })
                .filter(r->r.getSolution().equals("kill"))
                .map(r -> r.getId())
                .collect(Collectors.toList());
toKill.forEach(System.out::println);
    }

    public int deleteKilled() {
        List<UnhandledRecord> unhandledRecords = new ArrayList<>();
        unhandledRecordRepository.findAll().forEach(unhandledRecords::add);
        List<UnhandledRecord> kill = unhandledRecords.stream().filter(r -> r.getSolution() != null && r.getSolution().equals("kill"))
                .collect(Collectors.toList());
        unhandledRecordRepository.deleteAll(kill);
        return kill.size();
    }

    public List<UnhandledRecord> loadAll() {
        List<UnhandledRecord> retval = new ArrayList<>();
        unhandledRecordRepository.findAll().forEach(retval::add);
        return retval;
    }

    @Transactional
    public void repairUnhandledDiacritics() {
        List<UnhandledRecord> retval = new ArrayList<>();
        unhandledRecordRepository.findAll().forEach(retval::add);
        retval.stream()
                .filter(s->s.getChildUrl().contains("%"))
                .forEach(r->{
                    r.setSolution(URLDecoder.decode(r.getChildUrl()));
                    unhandledRecordRepository.save(r);
                });
    }
}
