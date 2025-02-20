package dev.avatar.middle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.entity.TelegramBotEntity;
import dev.avatar.middle.model.Bot;
import dev.avatar.middle.model.ClientBot;
import dev.avatar.middle.model.GodfatherBot;
import dev.avatar.middle.model.TelegramBotType;
import dev.avatar.middle.repository.TelegramBotRepository;
import dev.avatar.middle.service.ai.AssistantService;
import dev.avatar.middle.service.request.AbstractBotRequestService;
import dev.avatar.middle.service.request.ClientBotRequestService;
import dev.avatar.middle.service.request.GodfatherBotRequestService;
import dev.avatar.middle.service.telegram.command.TelegramCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BotServiceFactory {

    private final AssistantService assistantService;
    private final TelegramBotRepository telegramBotRepository;
    private final ClientBotRequestService clientBotRequestService;
    private final GodfatherBotRequestService godfatherBotRequestService;
    private final List<TelegramCommand> commands;
    private final Cache<String, Bot> botCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    public BotServiceFactory(
            AssistantService assistantService,
            TelegramBotRepository telegramBotRepository,
            ClientBotRequestService clientBotRequestService,
            GodfatherBotRequestService godfatherBotRequestService,
            List<TelegramCommand> commands
    ) {
        this.assistantService = assistantService;
        this.telegramBotRepository = telegramBotRepository;
        this.clientBotRequestService = clientBotRequestService;
        this.godfatherBotRequestService = godfatherBotRequestService;
        this.commands = commands;
        //todo check subscription/active flag -> findActive()
        this.telegramBotRepository.findAll().forEach(this::initializeBot);
    }

    public Optional<Bot> get(String botTokenId) {
        try {
            return Optional.of(this.botCache.get(
                    botTokenId,
                    () -> startTelegramBot(this.telegramBotRepository.findByBotTokenId(botTokenId).orElseThrow())
            ));
        } catch (ExecutionException e) {
            log.error("Catch error: {}, description: {}",
                    e.getCause().getMessage(), e.getCause().getMessage(), e);
        }
        return Optional.empty();
    }

    public void createTelegramBot(String botTokenId, String name, String description, String instructions) {
        String vectorStoreId = this.assistantService.createVectorStore();
        Data.Assistant assistant = this.assistantService.createAssistant(
                vectorStoreId, name, description, instructions
        );
        TelegramBotEntity telegramBotEntity = TelegramBotEntity.builder()
                .botTokenId(botTokenId)
                .assistantId(assistant.id())
                .vectorStoreId(vectorStoreId)
                .name(name)
                .botType(TelegramBotType.CLIENT_BOT)
                .build();
        this.initializeBot(telegramBotEntity);
    }

    public void removeTelegramBot(String botTokenId) {
        Optional.ofNullable(this.botCache.getIfPresent(botTokenId))
                .ifPresent(bot -> bot.getExecutableBot().shutdown());
        this.botCache.invalidate(botTokenId);
    }

    private void initializeBot(TelegramBotEntity bot) {
        this.botCache.put(bot.getBotTokenId(), startTelegramBot(bot));
    }

    private Bot startTelegramBot(TelegramBotEntity botEntity) {
        Bot botModel = switch (botEntity.getBotType()) {
            case GODFATHER_BOT -> new GodfatherBot(
                    botEntity.getBotTokenId(),
                    botEntity.getAssistantId(),
                    new TelegramBot(botEntity.getBotTokenId()),
                    TelegramBotType.GODFATHER_BOT,
                    prepareCommands(TelegramBotType.GODFATHER_BOT)
            );
            case CLIENT_BOT -> new ClientBot(
                    botEntity.getBotTokenId(),
                    botEntity.getAssistantId(),
                    new TelegramBot(botEntity.getBotTokenId()),
                    TelegramBotType.CLIENT_BOT,
                    prepareCommands(TelegramBotType.CLIENT_BOT)
            );
        };

        AbstractBotRequestService requestService = switch (botEntity.getBotType()) {
            case GODFATHER_BOT -> godfatherBotRequestService;
            case CLIENT_BOT -> clientBotRequestService;
        };

        botModel.getExecutableBot().setUpdatesListener(
                (List<Update> updates) -> {
                    try {
                        requestService.processRequest(botModel, updates);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                //todo create handler and move it there
                e -> {
                    if (e.response() != null) {
                        log.error("Catch error: {}, description: {}", e.response().errorCode(), e.response().description());
                    }
                    else {
                        log.error("Unexpected error: {}", e.getMessage());
                    }
                }
        );

        return botModel;
    }

    private List<TelegramCommand> prepareCommands(TelegramBotType botType) {
        return commands.stream()
                .filter(command -> command.getBotType() == botType)
                .toList();
    }
}
