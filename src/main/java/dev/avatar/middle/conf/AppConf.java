package dev.avatar.middle.conf;

import com.logaritex.ai.api.AssistantApi;
import com.logaritex.ai.api.AudioApi;
import com.logaritex.ai.api.FileApi;
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
    public AssistantApi getAssistantApi() {
        return new AssistantApi(openAiToken);
    }

    @Bean
    public AudioApi getAudioApi() {
        return new AudioApi(openAiToken);
    }

    @Bean
    public FileApi getFileApi() {
        return new FileApi(openAiToken);
    }
}
