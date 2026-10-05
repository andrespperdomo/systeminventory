package com.product.infrastructure.security;


import com.product.infrastructure.security.dto.AuthRequest;
import com.product.infrastructure.security.dto.AuthResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mindrot.jbcrypt.BCrypt;

import com.product.infrastructure.persistence.UserEntity;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    @Inject
    PanacheRepository<UserEntity> userRepository;

   // @ConfigProperty(name = "jwt.issuer")
    String issuer;

   public AuthResponse login(AuthRequest request) {

        Optional<UserEntity> user = userRepository.find("username", request.username())
                .firstResultOptional();

        if (!user.isPresent()) {
            throw new NotAuthorizedException("Invalid username or password");
        }

         UserEntity userEntity=user.get();

       if (!BCrypt.checkpw(request.password(), userEntity.getPassword())) {
    throw new NotAuthorizedException("Invalid username or password");
}

       

        String token = generateToken(userEntity);

        return new AuthResponse(
                token,
                "Bearer",
                userEntity.getUsername(),
                userEntity.getRole()
        );
    }

    private String generateToken(UserEntity user) {

        return Jwt.issuer(issuer)
                .subject(user.getUsername())
                .groups(Set.of(user.getRole()))
                .claim("userId", user.id)
                .claim("email", user.getEmail())
                .expiresIn(Duration.ofHours(2))
                .sign();
    }

}