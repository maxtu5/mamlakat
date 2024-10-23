package com.tuiken.mamlakat.model.dtos.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuiken.mamlakat.model.Country;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ReignDto {
    private String title;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate start;
    private LocalDate end;
    private LocalDate coronation;
    private Country country;
}
