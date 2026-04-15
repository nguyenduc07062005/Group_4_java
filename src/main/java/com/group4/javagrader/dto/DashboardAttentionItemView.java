package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardAttentionItemView {

    String title;
    String summary;
    String icon;
    String tone;
    String actionLabel;
    String href;
}
