package com.tuiken.mamlakat.model.dtos.api;

import com.tuiken.mamlakat.model.Country;
import lombok.Data;

@Data
public class CopyToGraphTaskDto {
    Country country;
    int from;
    int to;
}
