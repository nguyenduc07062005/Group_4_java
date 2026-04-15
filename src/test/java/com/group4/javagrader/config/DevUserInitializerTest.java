package com.group4.javagrader.config;

import com.group4.javagrader.entity.User;
import com.group4.javagrader.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevUserInitializerTest {

    private final DevUserInitializer initializer = new DevUserInitializer();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApplicationArguments applicationArguments = mock(ApplicationArguments.class);

    @Test
    void createsAdminWhenMissing() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");

        ApplicationRunner runner = initializer.devTeacherAccountInitializer(userRepository, passwordEncoder);
        runner.run(applicationArguments);

        verify(userRepository).save(argThat(user ->
                user.getId() == null
                        && "admin".equals(user.getUsername())
                        && "encoded-admin".equals(user.getPasswordHash())
                        && "Local Teacher".equals(user.getFullName())
                        && "TEACHER".equals(user.getRole())
                        && user.isEnabled()
        ));
    }

    @Test
    void refreshesExistingAdminCredentials() throws Exception {
        User existingUser = new User();
        existingUser.setId(7L);
        existingUser.setUsername("admin");
        existingUser.setPasswordHash("old-password");
        existingUser.setFullName("Old Name");
        existingUser.setRole("STUDENT");
        existingUser.setEnabled(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");

        ApplicationRunner runner = initializer.devTeacherAccountInitializer(userRepository, passwordEncoder);
        runner.run(applicationArguments);

        assertThat(existingUser.getId()).isEqualTo(7L);
        assertThat(existingUser.getUsername()).isEqualTo("admin");
        assertThat(existingUser.getPasswordHash()).isEqualTo("encoded-admin");
        assertThat(existingUser.getFullName()).isEqualTo("Local Teacher");
        assertThat(existingUser.getRole()).isEqualTo("TEACHER");
        assertThat(existingUser.isEnabled()).isTrue();
        verify(userRepository).save(existingUser);
    }
}
