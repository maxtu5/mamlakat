package com.tuiken.mamlakat.service;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.tuiken.mamlakat.dto.graphcsv.MonarchCsvDto;
import com.tuiken.mamlakat.dto.graphcsv.ProvenenceCsvDto;
import com.tuiken.mamlakat.dto.graphcsv.ReignCsvDto;
import com.tuiken.mamlakat.dto.graphcsv.ThroneCsvDto;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Provenence;
import com.tuiken.mamlakat.model.Reign;
import com.tuiken.mamlakat.model.Throne;
import com.tuiken.mamlakat.utils.Converters;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String TARGET_PATH = "src\\main\\resources\\csv\\";
    private static final String THRONES_FILE = "thrones.csv";
    private static final String MONARCHS_FILE = "monarchs.csv";
    private static final String REIGNS_FILE = "reigns.csv";
    private static final String FATHERS_FILE = "fathers.csv";
    private static final String MOTHERS_FILE = "mothers.csv";


    private final MonarchService monarchService;
    private final ThroneRoom throneRoom;
    private final ProvenanceService provenanceService;

    @Transactional
    public void exportAll() {
        List<Throne> thrones = throneRoom.loadAllThrones();
        List<ThroneCsvDto> throneDtos = thrones.stream()
                .map(this::toThroneCsvDto)
                .collect(Collectors.toList());
        saveToCSV(throneDtos, TARGET_PATH + THRONES_FILE);

        List<Monarch> monarchs = monarchService.loadAllMonarchs();
        List<MonarchCsvDto> monarchDtos = monarchs.stream()
                .map(this::toMonarchDto).toList();
        saveToCSV(monarchDtos, TARGET_PATH + MONARCHS_FILE);


        List<ReignCsvDto> reignDtos = new ArrayList<>();
        for (Throne throne : thrones) {
            reignDtos.addAll(buildReignDtos(throne, monarchs));
        }
        saveToCSV(reignDtos, TARGET_PATH + REIGNS_FILE);

        List<Provenence> provenences = provenanceService.findAllProvenances();
        List<ProvenenceCsvDto> fatherDtos = provenences.stream()
                .map(p -> p.getFather() != null ?
                        ProvenenceCsvDto.builder()
                                .id(p.getId().toString())
                                .parent(p.getFather().toString())
                                .build()
                        : null)
                .filter(Objects::nonNull).toList();
        List<ProvenenceCsvDto> motherDtos = provenences.stream()
                .map(p -> p.getMother() != null ?
                        ProvenenceCsvDto.builder()
                                .id(p.getId().toString())
                                .parent(p.getMother().toString())
                                .build()
                        : null)
                .filter(Objects::nonNull).toList();

        saveToCSV(fatherDtos, TARGET_PATH + FATHERS_FILE);

        saveToCSV(motherDtos, TARGET_PATH + MOTHERS_FILE);

    }

    private List<ReignCsvDto> buildReignDtos(Throne throne, List<Monarch> monarchs) {
        List<ReignCsvDto> reignDtos = new ArrayList<>();
        Map<String, Integer> doubleRulers = new HashMap<>();
        for (int i = 0; i<throne.getMonarchsIds().size(); i++) {
            int j = i;
            Monarch monarch = monarchs.stream()
                    .filter(m -> m.getId().toString().equals(throne.getMonarchsIds().get(j)))
                    .findFirst()
                    .get();
            List<Reign> reigns = monarch.getReigns().stream()
                    .filter(r -> r.getCountry().equals(throne.getCountry())).collect(Collectors.toList());
            if (reigns.size() == 0) {
                System.out.println(monarch.getName());
                continue;
            }
            if (reigns.size() == 1) {
                reignDtos.add(
                        toReignCsvDto(
                                reigns.get(0),
                                monarch.getId().toString(),
                                i == 0 ? null : throne.getMonarchsIds().get(i - 1))
                );
            } else {
                printCompare(
                        reigns,
                        throne,
                        monarch
                );
                if (doubleRulers.containsKey(monarch.getId().toString())) {
                    int reignsWere = doubleRulers.get(monarch.getId().toString());
                    reignDtos.add(
                            toReignCsvDto(
                                    reigns.get(reignsWere),
                                    monarch.getId().toString(),
                                    i == 0 ? null : throne.getMonarchsIds().get(i - 1))
                    );
                    doubleRulers.put(monarch.getId().toString(), reignsWere + 1);
                } else {
                    reigns.sort((r1, r2) -> (int) Duration.between(r1.getStart(), r2.getStart()).toMinutes());
                    reignDtos.add(
                            toReignCsvDto(
                                    reigns.get(0),
                                    monarch.getId().toString(),
                                    i == 0 ? null : throne.getMonarchsIds().get(i - 1))
                    );
                    doubleRulers.put(monarch.getId().toString(), 1);
                }
            }
        }
        return reignDtos;
    }

    private void printCompare(List<Reign> reigns, Throne throne, Monarch monarch) {
        int inThrone = 0;
        int inThronePos = 0;
        for (int i=0; i<throne.getMonarchsIds().size(); i++) {
            if (throne.getMonarchsIds().get(i).equals(monarch.getId().toString())) {
                inThrone++;
                inThronePos=i;
            }
        }
        if (inThrone!=reigns.size()) {
            System.out.println(inThrone + " " + reigns.size());
            Monarch m0 = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(inThronePos-1)));
            Monarch m1 = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(inThronePos)));
            Monarch m2 = monarchService.loadMonarch(UUID.fromString(throne.getMonarchsIds().get(inThronePos+1)));
            System.out.println(inThronePos-1 + " " + m0.getUrl() + " " + m0.getReigns().get(0).getStart().toString() + " " +
                    m0.getReigns().get(0).getEnd().toString());
            System.out.println(inThronePos + " " + m1.getUrl());
            System.out.println(inThronePos+1 + " " + m2.getUrl() + " " + m2.getReigns().get(0).getStart().toString() + " " +
                    m2.getReigns().get(0).getEnd().toString());
            System.out.println(monarch.getUrl()+" "+monarch.getReigns().get(0).getStart().toString() + "-" + monarch.getReigns().get(0).getEnd().toString() +
                    monarch.getReigns().get(1).getStart() + "-" + monarch.getReigns().get(1).getEnd() +'\n');
        } else {
            System.out.println(monarch.getName()+'\n');
        }
    }

    private <T> void saveToCSV(List<T> dtos, String fileName) {
        Class targetClass = dtos.size() > 0 ? dtos.get(0).getClass() : null;
        try (FileWriter writer = new FileWriter(fileName)) {
            CustomColumnPositionStrategy<T> strategy = new CustomColumnPositionStrategy<>();
            strategy.setType(targetClass);
            StatefulBeanToCsv<T> builder = new StatefulBeanToCsvBuilder<T>(writer)
                    .withApplyQuotesToAll(true)
                    .withMappingStrategy(strategy)
                    .withSeparator(',')
                    .build();
            builder.write(dtos);
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ThroneCsvDto toThroneCsvDto(Throne throne) {
        return ThroneCsvDto.builder()
                .id(throne.getId().toString())
                .name(throne.getName())
                .country(throne.getCountry().toString())
                .build();
    }

    private MonarchCsvDto toMonarchDto(Monarch monarch) {
        return MonarchCsvDto.builder()
                .id(monarch.getId().toString())
                .name(monarch.getName().replace(',', '|'))
                .gender(monarch.getGender() != null ? monarch.getGender().toString() : null)
                .birth(Converters.toLocalDate(monarch.getBirth()))
                .death(Converters.toLocalDate(monarch.getDeath()))
                .url(monarch.getUrl())
                .status(monarch.getStatus())
                .build();
    }

    private ReignCsvDto toReignCsvDto(Reign reign, String monarchId, String predecessorId) {
        return ReignCsvDto.builder()
                .monarchId(monarchId)
                .title(reign.getTitle())
                .start(Converters.toLocalDate(reign.getStart()))
                .end(Converters.toLocalDate(reign.getEnd()))
                .coronation(Converters.toLocalDate(reign.getCoronation()))
                .country(reign.getCountry().toString())
                .predecessorId(predecessorId)
                .build();
    }

    public static class CustomColumnPositionStrategy<T> extends ColumnPositionMappingStrategy<T> {

        @Override
        public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
            super.generateHeader(bean);
            return super.getColumnMapping();
        }
    }

}
