package com.group4.javagrader.grading.oop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.group4.javagrader.entity.Assignment;
import com.group4.javagrader.entity.AssignmentAttachmentType;
import com.group4.javagrader.service.AssignmentService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OopRuleCheckerFactory {

    private final AssignmentService assignmentService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public OopRuleCheckerFactory(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    public List<OopRuleChecker> create(Assignment assignment) {
        byte[] configData = assignmentService.findAttachmentData(
                assignment.getId(),
                AssignmentAttachmentType.OOP_RULE_CONFIG).orElse(null);
        if (configData == null || configData.length == 0) {
            return defaultRules();
        }

        try {
            JsonNode root = objectMapper.readTree(new String(configData, StandardCharsets.UTF_8));
            JsonNode rulesNode = root.path("rules");
            if (!rulesNode.isArray() || rulesNode.isEmpty()) {
                return defaultRules();
            }

            List<OopRuleChecker> checkers = new ArrayList<>();
            for (JsonNode ruleNode : rulesNode) {
                String type = ruleNode.path("type").asText("");
                String label = ruleNode.path("label").asText(type);
                switch (type) {
                    case "minimum_class_count" -> checkers.add(
                            new MinimumClassCountRuleChecker(ruleNode.path("value").asInt(2), label));
                    case "required_keyword" -> checkers.add(
                            new RequiredKeywordRuleChecker(ruleNode.path("value").asText("private"), label));
                    default -> {
                        // Ignore unsupported rule types so uploads remain forward-compatible.
                    }
                }
            }

            return checkers.isEmpty() ? defaultRules() : checkers;
        } catch (IOException ex) {
            return defaultRules();
        }
    }

    private List<OopRuleChecker> defaultRules() {
        return List.of(
                new MinimumClassCountRuleChecker(2, "At least two classes"),
                new RequiredKeywordRuleChecker("private", "Uses encapsulation"),
                new RequiredKeywordRuleChecker("class", "Contains class declarations"));
    }
}
