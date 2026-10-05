package com.product.infrastructure.web.response;

public record LoginResponse(

        String accessToken,

        Long expiresIn

){}