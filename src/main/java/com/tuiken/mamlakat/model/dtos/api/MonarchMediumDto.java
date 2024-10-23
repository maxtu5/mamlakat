package com.tuiken.mamlakat.model.dtos.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonarchMediumDto {
    String id;
    String name;
    String url;
    String allReigns;
}
