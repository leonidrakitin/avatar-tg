package dev.avatar.middle.client;

import dev.avatar.middle.conf.AppProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Slf4j
@Service
public class HeyGenClient {

    private final WebClient webClient;
    private final AppProperty appProperty;

    public HeyGenClient(WebClient.Builder webClientBuilder, AppProperty appProperty) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.heygen.com") //todo to config
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.appProperty = appProperty;
        log.info("HeyGenService initialized with base URL: https://api.heygen.com"); //todo to config
    }

    public String generateVideo(String text) {
        //todo start logic service
        log.info("Start generating video with text: {}", text);

        if (text.length() > 1500) {
            log.error("Text length exceeds 1500 characters. Length: {}", text.length());
            throw new IllegalArgumentException("Text input exceeds the 1500 character limit.");
        }

        GenerateVideoRequest request = new GenerateVideoRequest(
                new VideoInput[]{
                        new VideoInput(
                                new Character("avatar", "Gala_sitting_sofa_front_close", "normal"),
                                new Voice("text", text, "35b75145af9041b298c720f23375f578", 1.1)
                        )
                },
                new Dimension(600, 600)
        );

        log.debug("Request payload: {}", request);
        //todo end service
        try {
            // Преобразование в объект
            GenerateVideoResponse response = webClient.post()
                    .uri("/v2/video/generate")
                    .header("X-Api-Key", appProperty.getHeyGenApiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GenerateVideoResponse.class)
                    .block();

            log.debug("Parsed API response: {}", response);

            if (response == null || response.data() == null || response.data().video_id == null) {
                log.error("API response is null or missing required fields. Response: {}", response);
                throw new RuntimeException("Failed to generate video: response is null or missing videoId");
            }

            log.info("Video generation successful. Received videoId: {}", response.data().video_id);
            return response.data().video_id;

        } catch (Exception e) {
            log.error("Error occurred while generating video", e);
            throw new RuntimeException("Error generating video: " + e.getMessage(), e);
        }
    }

    public Optional<String> checkVideoStatus(String videoId) {
        log.info("Checking status for videoId: {}", videoId);

        try {
            Optional<String> videoUrl = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/video_status.get")
                            .queryParam("video_id", videoId)
                            .build())
                    .header("X-Api-Key", appProperty.getHeyGenApiKey())
                    .retrieve()
                    .bodyToMono(VideoStatusResponse.class)
                    .map(response -> {
                        log.debug("Received response for video status check: {}", response);
                        if ("completed".equals(response.data().status())) {
                            log.info("Video generation completed. Video URL: {}", response.data().video_url());
                            return Optional.of(response.data().video_url());
                        }
                        else {
                            log.debug("Video generation status: {}", response.data().status());
                            return Optional.<String>empty();
                        }
                    })
                    .block();

            return videoUrl;

        }
        catch (Exception e) {
            log.error("Unexpected error while checking video status for videoId: {}", videoId, e);
            return Optional.empty();
        }
    }

    public byte[] downloadVideo(String videoUrl) {
        log.info("Downloading video from URL: {}", videoUrl);

        try {
            byte[] videoBytes = webClient.get()
                    .uri(videoUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            if (videoBytes == null) {
                log.error("Downloaded video is null");
                throw new RuntimeException("Failed to download video: response is null");
            }

            log.info("Video downloaded successfully. Size: {} bytes", videoBytes.length);
            return videoBytes;

        }
        catch (Exception e) {
            log.error("Error occurred while downloading video from URL: {}", videoUrl, e);
            throw new RuntimeException("Error downloading video: " + e.getMessage(), e);
        }
    }

    private record GenerateVideoRequest(VideoInput[] video_inputs, Dimension dimension) {

    }

    private record VideoInput(Character character, Voice voice) {

    }

    private record Character(String type, String avatar_id, String avatar_style) {

    }

    private record Voice(String type, String input_text, String voice_id, double speed) {

    }

    private record Dimension(int width, int height) {

    }

    private record GenerateVideoResponse(Data data) {

        @Override
        public String toString() {
            return "GenerateVideoResponse{data=" + data + "}";
        }

        record Data(String video_id) {

            @Override
            public String toString() {
                return "Data{video_id='" + video_id + "'}";
            }
        }
    }

    private record VideoStatusResponse(Data data) {

        @Override
        public String toString() {
            return "VideoStatusResponse{data=" + data + "}";
        }

        record Data(String status, String video_url) {

            @Override
            public String toString() {
                return "Data{status='" + status + "', video_url='" + video_url + "'}";
            }
        }
    }
}
