package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.ProvenenceRepository;
import com.tuiken.mamlakat.model.Gender;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Provenence;
import com.tuiken.mamlakat.model.dtos.api.FamilyDto;
import com.tuiken.mamlakat.model.dtos.api.MonarchApiDto;
import com.tuiken.mamlakat.model.dtos.api.ShortMonarchDto;
import com.tuiken.mamlakat.model.workflows.SaveFamilyConfiguration;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvenanceService {

    private final ProvenenceRepository provenenceRepository;
    private final MonarchService monarchService;

    @Transactional
    public void saveFamily(SaveFamilyConfiguration saveConfig) {
        for (Provenence source : saveConfig.getToCreate()) {
            Provenence existing = provenenceRepository.findById(source.getId()).orElse(source);
            if (existing != source) {
                if (existing.getFather() == null && source.getFather() != null) existing.setFather(source.getFather());
                if (existing.getMother() == null && source.getMother() != null) existing.setMother(source.getMother());
            }
            provenenceRepository.save(existing);
        }
    }

    public Provenence findById(UUID id) {
        return provenenceRepository.findById(id).orElse(null);
    }

    @Transactional
    public void save(Provenence provenence) {
        provenenceRepository.save(provenence);
    }

    public List<Provenence> findAllProvenances() {
        List<Provenence> retval = new ArrayList<>();
        provenenceRepository.findAll().forEach(retval::add);
        return retval;
    }

    public void delete(Provenence provenence) {
        provenenceRepository.delete(provenence);
    }

    public List<Provenence> findAllByParent(UUID id, Gender gender) {
        return gender == Gender.MALE ? provenenceRepository.findByFather(id) : provenenceRepository.findByMother(id);
    }

    public Monarch findFather(Monarch child) {
        Provenence provenence = provenenceRepository.findById(child.getId()).orElse(null);
        if (provenence != null && provenence.getFather() != null) {
            return monarchService.loadMonarch(provenence.getFather());
        }
        return null;
    }

    public Monarch findMother(Monarch child) {
        Provenence provenence = provenenceRepository.findById(child.getId()).orElse(null);
        if (provenence != null && provenence.getMother() != null) {
            return monarchService.loadMonarch(provenence.getMother());
        }
        return null;
    }

    public Set<Monarch> findChildren(Monarch parent) {
        List<Provenence> children = parent.getGender() == Gender.MALE ?
                provenenceRepository.findByFather(parent.getId()) :
                provenenceRepository.findByMother(parent.getId());
        return children.stream()
                .map(Provenence::getId)
                .map(monarchService::loadMonarch)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void deleteAll(Set<Provenence> toDelete) {
        provenenceRepository.deleteAll(toDelete);
    }

    public MonarchApiDto toApiDtoByUrl(String url) {
        Monarch monarch = monarchService.loadMonarchByUrl(url).orElse(null);
        MonarchApiDto retval = monarchService.toApiDto(monarch);

        Provenence provenence = provenenceRepository.findById(monarch.getId()).orElse(null);
        FamilyDto family = new FamilyDto();

        if (provenence!=null) {
            if (provenence.getFather() != null) {
                Monarch father = monarchService.loadMonarch(provenence.getFather());
                ShortMonarchDto dad = new ShortMonarchDto(father.getName(), father.getUrl());
                family.setFather(dad);
            }
            if (provenence.getMother() != null) {
                Monarch mother = monarchService.loadMonarch(provenence.getMother());
                ShortMonarchDto mum = new ShortMonarchDto(mother.getName(), mother.getUrl());
                family.setMother(mum);
            }
        }
        Set<Monarch> children = findChildren(monarch);
        family.setChildren(children.stream()
                .map(m->new ShortMonarchDto(m.getName(),m.getUrl())).collect(Collectors.toList()));
        retval.setFamily(family);
        return retval;
    }
}
