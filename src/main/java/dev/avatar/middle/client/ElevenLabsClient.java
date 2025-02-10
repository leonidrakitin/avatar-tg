package dev.avatar.middle.client;

import dev.avatar.middle.conf.AppProperty;
import dev.avatar.middle.exceptions.ElevenLabsException;
import dev.avatar.middle.exceptions.enums.ElevenLabsErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class ElevenLabsClient {

    private final WebClient webClient;
    private final AppProperty appProperty;

    public ElevenLabsClient(WebClient.Builder webClientBuilder, AppProperty appProperty) {
        this.webClient = webClientBuilder
                .baseUrl(appProperty.getElevenLabsBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)
                )
//                .defaultHeader() //todo auth header
                .build();
        this.appProperty = appProperty;
        log.info("ElevenLabsClient initialized with base URL: {}", appProperty.getElevenLabsBaseUrl());
    }

    public Mono<byte[]> generateAudio(String voiceId, String modelId, String text) {
        String apiKey = appProperty.getElevenLabsApiKey(); //todo should be default

        Map<String, Object> requestBody = Map.of(
                "text", text,
                "model_id", modelId,
                "voice_settings", Map.of(
                        "style", 0.3,
                        "stability", 0.5,
                        "similarity_boost", 0.75
                )
        );

        log.debug("Sending text-to-speech request: {}", requestBody);

        try {
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/text-to-speech/" + voiceId)
                            .queryParam("output_format", "mp3_44100_128")
                            .build())
                    .header("xi-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class);
        }
        catch (WebClientResponseException e) {
            log.error("ElevenLabs API error: {}", e.getResponseBodyAsString(), e);
            throw new ElevenLabsException(ElevenLabsErrorCode.API_ERROR,
                    "Error during audio generation: " + e.getResponseBodyAsString(), e);
        }
        catch (Exception e) {
            log.error("Unexpected error while generating audio", e);
            throw new ElevenLabsException(ElevenLabsErrorCode.API_ERROR,
                    "Unexpected error while generating audio", e);
        }
    }
}
