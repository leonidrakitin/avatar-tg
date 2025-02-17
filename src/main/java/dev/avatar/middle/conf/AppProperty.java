package dev.avatar.middle.conf;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties("app")
public class AppProperty {

    private String defaultOpenAiUrl; //todo set this param
    private List<String> adminTelegramIds;
    private String telegramId;
    private String version;
    private String token;
    private String username;
    private String heyGenApiKey;
    private String heyGenBaseUrl;
    private String heyGenAvatarId;
    private String heyGenVoiceId;
    private String elevenLabsBaseUrl;
    private String elevenLabsApiKey;
}
