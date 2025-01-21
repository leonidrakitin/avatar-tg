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

    private List<String> adminTelegramIds;
    private String telegramId;
    private String version;
    private String token;
    private String username;

}
