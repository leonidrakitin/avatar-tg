package dev.avatar.middle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChatGptTelegramBot {

    public static void main(String[] args) {
        SpringApplication.run(ChatGptTelegramBot.class, args);
    }
}
