package com.product.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BCryptPasswordEncoder{

    public String encode(String password){

        return BCrypt.hashpw(password,
                             BCrypt.gensalt());

    }

    public boolean matches(String password,
                           String hash){

        return BCrypt.checkpw(password,hash);

    }

}