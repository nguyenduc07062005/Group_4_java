package com.group4.javagrader.dto;

import lombok.Value;

@Value
public class HeaderNotificationItem {

    String title;
    String message;
    String tone;
    String destination;
}
