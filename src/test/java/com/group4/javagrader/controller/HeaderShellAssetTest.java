package com.group4.javagrader.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderShellAssetTest {

    @Test
    void classpathDoesNotShipLegacyHeaderScript() {
        assertThat(new ClassPathResource("static/js/header.js").exists()).isFalse();
    }
}
