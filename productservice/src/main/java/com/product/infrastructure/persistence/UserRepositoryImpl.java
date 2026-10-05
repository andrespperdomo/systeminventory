package com.product.infrastructure.persistence;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepositoryImpl
        implements PanacheRepository<UserEntity>{

    public Optional<UserEntity> findByUsername(String username){

        return find("username",username)
                .firstResultOptional();

    }

}