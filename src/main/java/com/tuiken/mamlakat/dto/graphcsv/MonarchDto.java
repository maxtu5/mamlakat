package com.tuiken.mamlakat.dto.graphcsv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.tuiken.mamlakat.model.PersonStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonarchDto {
    @CsvBindByPosition(position = 0)
    String id;
    @CsvBindByPosition(position = 1)
    String name;
    @CsvBindByPosition(position = 2)
    String gender;
    @CsvBindByPosition(position = 3)
    LocalDate birth;
    @CsvBindByPosition(position = 4)
    LocalDate death;
    @CsvBindByPosition(position = 5)
    PersonStatus status;
    @CsvBindByPosition(position = 6)
    String url;

}
