package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class DashboardActivityItemView {

    String title;
    String summary;
    String icon;
    String timestampLabel;
    String href;
}
