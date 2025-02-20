package dev.avatar.middle.service.telegram.command.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotActionDto(
        String action,
        String botToken
) {
}
