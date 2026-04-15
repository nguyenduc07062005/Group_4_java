package com.group4.javagrader.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderPageMetadataResolverTest {

    private final HeaderPageMetadataResolver resolver = new HeaderPageMetadataResolver();

    @Test
    void resolvesAssignmentWorkspaceMetadata() {
        HeaderPageMetadataResolver.HeaderPageMetadata metadata = resolver.resolve("/assignments/42/reports");

        assertThat(metadata.pageTitle()).isEqualTo("Reports");
        assertThat(metadata.pageTrail()).isEqualTo("SEMESTERS / ASSIGNMENTS / REPORTS");
        assertThat(metadata.primarySection()).isEqualTo("semesters");
        assertThat(metadata.pageDescription()).contains("exportable grade reports");
        assertThat(metadata.pageRobots()).isEqualTo("noindex,nofollow");
    }

    @Test
    void resolvesLoginMetadata() {
        HeaderPageMetadataResolver.HeaderPageMetadata metadata = resolver.resolve("/login");

        assertThat(metadata.pageTitle()).isEqualTo("Sign In");
        assertThat(metadata.pageTrail()).isEqualTo("WORKBOARD / PAGE");
        assertThat(metadata.pageDescription()).contains("Secure teacher portal");
        assertThat(metadata.pageRobots()).isEqualTo("index,follow");
    }
}
