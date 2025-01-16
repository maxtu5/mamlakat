package com.tuiken.mamlakat.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.tuiken.mamlakat.dto.graphcsv.ProvenenceDto;
import com.tuiken.mamlakat.dto.graphcsv.MonarchDto;
import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Provenence;
import com.tuiken.mamlakat.model.Throne;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String TG_MONARCHS_FILE = "src\\main\\resources\\koningen.csv";
    private static final String TG_PROVS_FILE = "src\\main\\resources\\familien";

    private final MonarchService monarchService;
    private final ThroneRoom throneRoom;
    private final ProvenanceService provenanceService;

    @Transactional
    public void exportSample() throws Exception {
        Country country = Country.POLAND;
        Throne throne = throneRoom.loadThroneByCountry(country);
        if (throne != null) {
            Set<Monarch> allMonarchs = new HashSet<>();
            Set<Provenence> provenences = new HashSet<>();
            for (int i = 0; i < 30; i++) {
                Monarch monarch = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(i)));
                List<Monarch> kids = provenanceService.findAllByParent(monarch.getId(), monarch.getGender()).stream()
                        .peek(provenences::add)
                        .map(p -> monarchService.loadMonarch(p.getId()))
                        .collect(Collectors.toList());
                allMonarchs.add(monarch);
                allMonarchs.addAll(kids);
                Provenence provenence = provenanceService.findById(monarch.getId());
                provenences.add(provenence);
                if (provenence != null && provenence.getMother() != null) {
                    allMonarchs.add(monarchService.loadMonarch(provenence.getMother()));
                }
                if (provenence != null && provenence.getFather() != null) {
                    allMonarchs.add(monarchService.loadMonarch(provenence.getFather()));
                }
            }

            List<MonarchDto> peopleDtos = allMonarchs.stream()
                    .filter(Objects::nonNull)
                    .map(monarchService::toMonarchDto).collect(Collectors.toList());
            provenences = provenences.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            List<ProvenenceDto> fatherDtos = provenences.stream()
                    .map(p -> p.getFather() != null ?
                            ProvenenceDto.builder()
                                    .id(p.getId().toString())
                                    .parent(p.getFather().toString())
                                    .build()
                            : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<ProvenenceDto> motherDtos = provenences.stream()
                    .map(p -> p.getMother() != null ?
                            ProvenenceDto.builder()
                                    .id(p.getId().toString())
                                    .parent(p.getMother().toString())
                                    .build()
                            : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            beanToCSVWithDefault(peopleDtos, fatherDtos, motherDtos);

        }

    }

    private void beanToCSVWithDefault(List<MonarchDto> applications, List<ProvenenceDto> fatherDtos, List<ProvenenceDto> motherDtos) throws Exception {
        try (FileWriter writer = new FileWriter(TG_MONARCHS_FILE)) {
            HeaderColumnNameMappingStrategy<MonarchDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(MonarchDto.class);
            StatefulBeanToCsv<MonarchDto> builder = new StatefulBeanToCsvBuilder<MonarchDto>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withMappingStrategy(strategy)
                    .withSeparator(',')
                    .build();
            builder.write(applications);
        }
        try (FileWriter writer = new FileWriter(TG_PROVS_FILE + "_fathers.csv")) {
            HeaderColumnNameMappingStrategy<ProvenenceDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(ProvenenceDto.class);
            StatefulBeanToCsv<ProvenenceDto> builder = new StatefulBeanToCsvBuilder<ProvenenceDto>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withSeparator(',')
                    .withMappingStrategy(strategy)
                    .build();
            builder.write(fatherDtos);
        }
        try (FileWriter writer = new FileWriter(TG_PROVS_FILE + "_mothers.csv")) {
            HeaderColumnNameMappingStrategy<ProvenenceDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(ProvenenceDto.class);
            StatefulBeanToCsv<ProvenenceDto> builder = new StatefulBeanToCsvBuilder<ProvenenceDto>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .withSeparator(',')
                    .withMappingStrategy(strategy)
                    .build();
            builder.write(motherDtos);
        }
    }
}
