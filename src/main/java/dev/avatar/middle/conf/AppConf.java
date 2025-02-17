package dev.avatar.middle.conf;

import dev.avatar.middle.client.OpenAiAssistantClient;
import dev.avatar.middle.client.OpenAiAudioClient;
import dev.avatar.middle.client.OpenAiFileClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class AppConf {


    @Value("${openai.token}")
    private String openAiToken;
//    @Bean
//    @ConfigurationProperties("openai")
//    public GPTConfig gptConfig() {
//        return new GPTConfig();
//    }

    @Bean
    public OpenAiAssistantClient getAssistantApi() {
        return new OpenAiAssistantClient(openAiToken);
    }

    @Bean
    public OpenAiAudioClient getAudioApi() {
        return new OpenAiAudioClient(openAiToken);
    }

    @Bean
    public OpenAiFileClient getFileApi() {
        return new OpenAiFileClient(openAiToken);
    }
}
