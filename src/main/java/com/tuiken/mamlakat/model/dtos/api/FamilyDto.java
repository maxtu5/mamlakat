package com.tuiken.mamlakat.model.dtos.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuiken.mamlakat.model.Country;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class FamilyDto {
    private ShortMonarchDto mother;
    private ShortMonarchDto father;
    private List<ShortMonarchDto> children;
}