package com.tuiken.mamlakat.model.dtos.api;

import com.tuiken.mamlakat.model.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ThroneOperationDto {
    Country country;
    String name;
    String latestMonarchUrl;
}
