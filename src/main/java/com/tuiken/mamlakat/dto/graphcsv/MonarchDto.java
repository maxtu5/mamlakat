package com.tuiken.mamlakat.dto.graphcsv;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonarchDto {
    @CsvBindByPosition(position = 0)
    @CsvBindByName(column = "id")
    String id;
    @CsvBindByPosition(position = 1)
    @CsvBindByName(column = "name")
    String name;
    @CsvBindByPosition(position = 2)
    @CsvBindByName(column = "birth")
    LocalDate birth;
    @CsvBindByPosition(position = 3)
    @CsvBindByName(column = "death")
    LocalDate death;
    @CsvBindByPosition(position = 4)
    @CsvBindByName(column = "url")
    String url;
    @CsvBindByPosition(position = 5)
    @CsvBindByName(column = "gender")
    String gender;
}
