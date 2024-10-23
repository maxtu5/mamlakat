package com.tuiken.mamlakat.dto.apimonarch.login;

import lombok.Builder;

@Builder
public class TokenRequestDto {

    public String username;
    public String password;

}
