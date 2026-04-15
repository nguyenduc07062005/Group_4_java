package com.group4.javagrader.config;

import com.group4.javagrader.entity.User;
import com.group4.javagrader.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"dev", "local"})
public class DevUserInitializer {

    @Bean
    public ApplicationRunner devTeacherAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            User user = userRepository.findByUsername("admin")
                    .orElseGet(User::new);

            user.setUsername("admin");
            user.setPasswordHash(passwordEncoder.encode("admin"));
            user.setFullName("Local Teacher");
            user.setRole("TEACHER");
            user.setEnabled(true);
            userRepository.save(user);
        };
    }
}
