package com.springboot.springsecurity.controller.dto;

public record LoginResponse(String accessToken, Long expiresIn) {

}
