package com.tuiken.mamlakat.dto.graphcsv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencsv.bean.CsvBindByPosition;
import com.tuiken.mamlakat.model.Country;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReignCsvDto {

    @CsvBindByPosition(position = 0)
    private String country;
    @CsvBindByPosition(position = 1)
    private String monarchId;
    @CsvBindByPosition(position = 2)
    private String title;
    @CsvBindByPosition(position = 3)
    private LocalDate start;
    @CsvBindByPosition(position = 4)
    private LocalDate end;
    @CsvBindByPosition(position = 5)
    private LocalDate coronation;
    @CsvBindByPosition(position = 6)
    private String predecessorId;

}