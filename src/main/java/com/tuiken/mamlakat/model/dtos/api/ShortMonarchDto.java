package com.tuiken.mamlakat.model.dtos.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ShortMonarchDto {
    private String name;
    private String url;
}