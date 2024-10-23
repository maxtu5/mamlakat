package com.tuiken.mamlakat.model.dtos.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ThroneDto {
    String id;
    String name;
}
