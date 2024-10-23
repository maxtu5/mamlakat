package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.MonarchRepository;
import com.tuiken.mamlakat.dto.graphcsv.MonarchDto;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.PersonStatus;
import com.tuiken.mamlakat.model.Provenence;
import com.tuiken.mamlakat.model.Reign;
import com.tuiken.mamlakat.model.dtos.api.MonarchApiDto;
import com.tuiken.mamlakat.model.dtos.api.FamilyDto;
import com.tuiken.mamlakat.model.dtos.api.ReignDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonarchService {

    private final MonarchRepository monarchRepository;

    public MonarchApiDto toApiDtoByUuid(String uuid) {
        Monarch monarch = monarchRepository.findByUrl(String.valueOf(uuid)).orElse(null);
        return toApiDto(monarch);
    }

    public MonarchApiDto toApiDto(Monarch monarch) {
        if (monarch==null) {
            return null;
        } else {
            List<ReignDto> reignDtos = new ArrayList<>();
            monarch.getReigns().forEach(r-> {
                ReignDto reignDto = ReignDto.builder()
                        .title(r.getTitle())
                        .country(r.getCountry())
                        .start(r.getStart()==null ? null : r.getStart().atZone(ZoneId.systemDefault()).toLocalDate())
                        .end(r.getEnd()==null ? null : r.getEnd().atZone(ZoneId.systemDefault()).toLocalDate())
                        .coronation(r.getCoronation()==null ? null : r.getCoronation().atZone(ZoneId.systemDefault()).toLocalDate())
                        .build();
                reignDtos.add(reignDto);
            });
            return MonarchApiDto.builder()
                    .id(monarch.getId())
                    .name(monarch.getName())
                    .url(monarch.getUrl())
                    .gender(monarch.getGender())
                    .house(monarch.getHouse())
                    .birth(monarch.getBirth()==null  ?null : monarch.getBirth().atZone(ZoneId.systemDefault()).toLocalDate())
                    .death(monarch.getDeath()==null  ?null : monarch.getDeath().atZone(ZoneId.systemDefault()).toLocalDate())
                    .status(monarch.getStatus())
                    .reigns(reignDtos)
                    .build();
        }
    }

    public MonarchApiDto updateMonarch(MonarchApiDto updatedMonarch) {
        // only name, url, reigns
        Monarch monarch = monarchRepository.findById(updatedMonarch.getId()).orElse(null);
        if (monarch!=null) {
            monarch.setName(updatedMonarch.getName()==null ? monarch.getName() : updatedMonarch.getName());
            monarch.setUrl(updatedMonarch.getUrl()==null ? monarch.getUrl() : updatedMonarch.getUrl());
            List<Reign> reigns = updatedMonarch.getReigns()==null ? null :
                    updatedMonarch.getReigns().stream()
                    .map(r -> {
                        Reign reign = new Reign();
                        reign.setTitle(r.getTitle());
                        reign.setCountry(r.getCountry());
                        reign.setStart(r.getStart() == null ? null : r.getStart().atStartOfDay().toInstant(ZoneOffset.UTC));
                        reign.setEnd(r.getEnd() == null ? null : r.getEnd().atStartOfDay().toInstant(ZoneOffset.UTC));
                        reign.setCoronation(r.getCoronation() == null ? null : r.getCoronation().atStartOfDay().toInstant(ZoneOffset.UTC));
                        return reign;
                    })
                    .collect(Collectors.toList());

            monarch.setReigns(reigns==null ? monarch.getReigns() : reigns);
            monarchRepository.save(monarch);
            return toApiDto(monarch);
        } else {
            return null;
        }
    }

    @Transactional
    public Monarch loadMonarch(UUID id) {
        return monarchRepository.findById(id).orElse(null);
    }

    @Transactional
    public Monarch saveMonarch(Monarch monarch) {
        log.info(String.format("Saving monarch %s", monarch.getName()));
        return monarchRepository.save(monarch);
    }

    public MonarchApiDto deleteById(String id) {
        MonarchApiDto retval = null;
        Monarch monarch = monarchRepository.findById(UUID.fromString(id)).orElse(null);
        if (monarch!=null) {
            retval = toApiDto(monarch);
            log.info(String.format("Deleting monarch %s, %s", monarch.getName(), monarch.getId()));
            monarchRepository.deleteById(UUID.fromString(id));
        }
        return retval;
    }

    public Optional<Monarch> loadMonarchByUrl(String latestMonarchUrl) {
        return monarchRepository.findByUrl(latestMonarchUrl);
    }

    public MonarchDto toTg(Monarch monarch) {
        return MonarchDto.builder()
                .id(monarch.getId().toString())
                .name(monarch.getName().replace(',', '|'))
                .gender(monarch.getGender()!=null ? monarch.getGender().toString() : null)
                .birth(monarch.getBirth() != null ? monarch.getBirth().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .death(monarch.getDeath() != null ? monarch.getDeath().atZone(ZoneId.systemDefault()).toLocalDate() : null)
                .url(monarch.getUrl())
                .build();
    }

    public List<Monarch> loadAllMonarchs() {
        List<Monarch> retval = new ArrayList<>();
        monarchRepository.findAll().forEach(retval::add);
        return retval;
    }

    public Monarch findByUrl(String url) {
        Monarch monarch = monarchRepository.findByUrl(url).orElse(null);
        return monarch;
    }

    public Monarch save(Monarch existParent) {
        return monarchRepository.save(existParent);
    }

    @Transactional
    public void saveStatus(UUID id, PersonStatus status) {
        Monarch monarch = monarchRepository.findById(id).orElse(null);
        if (monarch!=null) {
            monarch.setStatus(status);
            monarchRepository.save(monarch);
        }
    }

    @Transactional
    public List<MonarchApiDto> deleteMonarchs(List<String> collect) {
        List<MonarchApiDto> retval = new ArrayList<>();
        for (String id : collect) {
            retval.add(deleteById(id));
        }
        return retval;
    }
}
