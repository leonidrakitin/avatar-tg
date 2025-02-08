package dev.avatar.middle.client;

import dev.avatar.middle.conf.AppProperty;
import dev.avatar.middle.exceptions.HeyGenException;
import dev.avatar.middle.exceptions.enums.HeyGenErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class HeyGenClient {

    private final WebClient webClient;
    private final AppProperty appProperty;

    public HeyGenClient(WebClient.Builder webClientBuilder, AppProperty appProperty) {
        this.webClient = webClientBuilder
                .baseUrl(appProperty.getHeyGenBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.appProperty = appProperty;
        log.info("HeyGenClient initialized with base URL: {}", appProperty.getHeyGenBaseUrl());
    }

    public Mono<String> generateVideo(String text) {
        //todo start logic service

        HeyGenClient.GenerateVideoRequest request = new HeyGenClient.GenerateVideoRequest(
                new HeyGenClient.VideoInput[]{
                        new HeyGenClient.VideoInput(
                                new HeyGenClient.Character("avatar", appProperty.getHeyGenAvatarId(), "normal"),
                                new HeyGenClient.Voice("text", text, appProperty.getHeyGenVoiceId(), 1.1)
                        )
                },
                new HeyGenClient.Dimension(600, 600)
        );

        log.debug("Sending video generation request: {}", request);
        try {
            return webClient.post()
                    .uri("/v2/video/generate")
                    .header("X-Api-Key", appProperty.getHeyGenApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GenerateVideoResponse.class)
                    .map(GenerateVideoResponse::data)
                    .map(GenerateVideoResponse.Data::video_id);
        }
        catch (Exception e) {
            throw new HeyGenException(HeyGenErrorCode.API_ERROR, "Error during video generation", e);
        }
    }

    public Mono<Optional<String>> getVideoStatus(String videoId) {
        log.debug("Checking status for videoId: {}", videoId);

        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/video_status.get")
                            .queryParam("video_id", videoId)
                            .build())
                    .header("X-Api-Key", appProperty.getHeyGenApiKey())
                    .retrieve()
                    .bodyToMono(VideoStatusResponse.class)
                    .map(response -> {
                        log.debug("Received video status response: {}", response);
                        if ("completed".equals(response.data().status())) {
                            return Optional.of(response.data().video_url());
                        }
                        return Optional.empty();
                    });
        }
        catch (Exception e) {
            throw new HeyGenException(HeyGenErrorCode.VIDEO_STATUS_ERROR,
                    "Error checking video status for videoId: " + videoId, e);
        }
    }

    public Mono<byte[]> downloadVideo(String videoUrl) {
        log.info("Downloading video from URL: {}", videoUrl);

        try {
            return webClient.get()
                    .uri(videoUrl)
                    .retrieve()
                    .bodyToMono(byte[].class);
        }
        catch (Exception e) {
            throw new HeyGenException(
                    HeyGenErrorCode.VIDEO_DOWNLOAD_ERROR,
                    "Error downloading video from URL: " + videoUrl,
                    e
            );
        }
    }

    public record GenerateVideoRequest(VideoInput[] video_inputs, Dimension dimension) {

        @Override
        public String toString() {
            return "GenerateVideoRequest{video_inputs=" + video_inputs + ", dimension=" + dimension + "}";
        }
    }

    public record VideoInput(Character character, Voice voice) {

        @Override
        public String toString() {
            return "VideoInput{character=" + character + ", voice=" + voice + "}";
        }
    }

    public record Character(String type, String avatar_id, String avatar_style) {

        @Override
        public String toString() {
            return "Character{type='" + type + "', avatar_id='" + avatar_id + "', avatar_style='" + avatar_style + "'}";
        }
    }

    public record Voice(String type, String input_text, String voice_id, double speed) {

        @Override
        public String toString() {
            return "Voice{type='" + type + "', input_text='" + input_text + "', voice_id='" + voice_id + "', speed=" + speed + "}";
        }
    }

    public record Dimension(int width, int height) {

        @Override
        public String toString() {
            return "Dimension{width=" + width + ", height=" + height + "}";
        }
    }

    public record GenerateVideoResponse(Data data) {

        @Override
        public String toString() {
            return "GenerateVideoResponse{data=" + data + "}";
        }

        public record Data(String video_id) {

            @Override
            public String toString() {
                return "Data{video_id='" + video_id + "'}";
            }
        }
    }

    public record VideoStatusResponse(Data data) {

        @Override
        public String toString() {
            return "VideoStatusResponse{data=" + data + "}";
        }

        public record Data(String status, String video_url) {

            @Override
            public String toString() {
                return "Data{status='" + status + "', video_url='" + video_url + "'}";
            }
        }
    }
}
