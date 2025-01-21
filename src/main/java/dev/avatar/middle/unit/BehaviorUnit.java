//package dev.avatar.middle.unit;
//
//import dev.avatar.middle.service.PersonalChatService;
//import dev.avatar.middle.unit.util.Cmd;
//import dev.avatar.middle.unit.util.UnitName;
//import dev.struchkov.godfather.main.domain.annotation.Unit;
//import dev.struchkov.godfather.main.domain.content.Attachment;
//import dev.struchkov.godfather.main.domain.content.Mail;
//import dev.struchkov.godfather.simple.domain.BoxAnswer;
//import dev.struchkov.godfather.simple.domain.unit.AnswerText;
//import dev.struchkov.godfather.telegram.domain.attachment.CommandAttachment;
//import dev.struchkov.godfather.telegram.main.core.util.Attachments;
//import dev.struchkov.godfather.telegram.starter.PersonUnitConfiguration;
//import dev.struchkov.openai.domain.chat.ChatInfo;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Optional;
//
//import static dev.avatar.middle.unit.util.UnitName.CURRENT_BEHAVIOR;
//import static dev.struchkov.godfather.simple.domain.BoxAnswer.boxAnswer;
//
//@Component
//@RequiredArgsConstructor
//public class BehaviorUnit implements PersonUnitConfiguration {
//
//    public static final String TEST = """
//             Or choose one of the available options:
//
//                                                1. Гопник
//                                                {0}
//                                                👉 /behavior_ru_1
//
//                                                2. Copilot
//                                                {1}
//                                                👉 /behavior_ru_2
//
//                                                3. Linux
//                                                {2}
//                                                👉 /behavior_ru_3
//
//                                                4. Тренера по взаимоотношениям
//                                                {3}
//                                                👉 /behavior_ru_4
//
//                                                5. Наставник
//                                                {4}
//                                                👉 /behavior_ru_5
//
//                                                6. В двух словах
//                                                {5}
//                                                👉 /behavior_ru_6
//            """;
//
//    public static final String BEHAVIOUR_RU_1 = "Представь, что ты славянский гопник и общаешься со своими собутыльниками по подъезду. Используй побольше мата и блатного жаргона. Используй как можно больше «бля» и «ёпта». Отвечай в таком стиле всегда, какой бы вопрос не задали пользователи в этом чате.";
//    public static final String BEHAVIOUR_RU_2 = "Ты - помощник для программистов. На любой вопрос отвечай с примерами кода, если нужно и присылай советы по улучшению кода";
//    public static final String BEHAVIOUR_RU_3 = "Я хочу, чтобы вы выступали в роли терминала Linux. Я буду вводить команды, а вы будете отвечать тем, что должен показать терминал. Я хочу, чтобы вы ответили выводом терминала только внутри одного уникального блока кода, и ничего больше. не пишите пояснений. не вводите команды, если я не поручу вам это сделать. Когда мне нужно будет сказать вам что-то на русском языке, я буду заключать текст в фигурные скобки {вот так}.";
//    public static final String BEHAVIOUR_RU_4 = "Я хочу, чтобы вы выступили в роли тренера по взаимоотношениям. Я предоставлю некоторые подробности о двух людях, вовлеченных в конфликт, а ваша задача - предложить, как они могут решить проблемы, которые их разделяют. Это могут быть советы по технике общения или различные стратегии для улучшения понимания ими точек зрения друг друга. Первый запрос: \"Мне нужна помощь в разрешении конфликтов между мной и моим парнем\".";
//    public static final String BEHAVIOUR_RU_5 = "Вы наставник, который всегда отвечает в сократовском стиле. Вы *никогда* не даете ученику ответа, но всегда стараетесь задать правильный вопрос, чтобы помочь ему научиться думать самостоятельно. Вы всегда должны согласовывать свой вопрос с интересами и знаниями учащегося, разбивая проблему на более простые части, пока она не достигнет нужного для них уровня.";
//    public static final String BEHAVIOUR_RU_6 = "Отвечай максимально коротко, даже если тебя просят ответить развернуто. Весь ответ должен уложиться в пару предложений.";
//
//    private final PersonalChatService chatService;
//
//    @Unit(value = UnitName.BEHAVIOR, global = true)
//    public AnswerText<Mail> behavior() {
//        return AnswerText.<Mail>builder()
//                .triggerCheck(
//                        mail -> {
//                            final List<Attachment> attachments = mail.getAttachments();
//                            final Optional<CommandAttachment> optCommand = Attachments.findFirstCommand(attachments);
//                            if (optCommand.isPresent()) {
//                                final CommandAttachment command = optCommand.get();
//                                return Cmd.BEHAVIOR.equals(command.getCommandType());
//                            }
//                            return false;
//                        }
//                )
//                .answer(mail -> {
//                    final CommandAttachment command = Attachments.findFirstCommand(mail.getAttachments()).orElseThrow();
//                    final Optional<String> optArg = command.getArg();
//                    if (optArg.isEmpty()) {
//                        return BoxAnswer.builder()
//                                .message(
//                                        """
//                                                Allows you to set the ChatGPT behavior for chat. Remains active when the context is cleared.
//
//                                                If you want to set your own behavior, then send the command:
//
//                                                /behavior description_behavior
//                                                """
//                                ).build();
//                    } else {
//                        final String behavior = optArg.get();
//                        final String personId = mail.getFromPersonId();
//                        final String currentChatName = chatService.getCurrentChatName(personId);
//                        chatService.setBehavior(personId, currentChatName, behavior);
//                        return boxAnswer("\uD83D\uDC4C");
//                    }
//                })
//                .build();
//    }
//
//    @Unit(value = UnitName.CLEAR_BEHAVIOR, global = true)
//    public AnswerText<Mail> clearBehavior() {
//        return AnswerText.<Mail>builder()
//                .triggerCheck(
//                        mail -> {
//                            final List<Attachment> attachments = mail.getAttachments();
//                            final Optional<CommandAttachment> optCommand = Attachments.findFirstCommand(attachments);
//                            if (optCommand.isPresent()) {
//                                final CommandAttachment command = optCommand.get();
//                                return Cmd.CLEAR_BEHAVIOR.equals(command.getCommandType());
//                            }
//                            return false;
//                        }
//                )
//                .answer(mail -> {
//                    chatService.clearBehavior(mail.getFromPersonId());
//                    return boxAnswer("Behavior successfully cleared");
//                })
//                .build();
//    }
//
//    @Unit(value = CURRENT_BEHAVIOR, global = true)
//    public AnswerText<Mail> currentBehavior() {
//        return AnswerText.<Mail>builder()
//                .triggerCheck(
//                        mail -> {
//                            final List<Attachment> attachments = mail.getAttachments();
//                            final Optional<CommandAttachment> optCommand = Attachments.findFirstCommand(attachments);
//                            if (optCommand.isPresent()) {
//                                final CommandAttachment command = optCommand.get();
//                                return Cmd.CURRENT_BEHAVIOR.equals(command.getCommandType());
//                            }
//                            return false;
//                        }
//                )
//                .answer(mail -> {
//                    final ChatInfo currentChat = chatService.getCurrentChat(mail.getFromPersonId());
//                    final String systemBehavior = currentChat.getSystemBehavior();
//                    if (systemBehavior != null && !"".equals(systemBehavior)) {
//                        return boxAnswer(systemBehavior);
//                    }
//                    return boxAnswer("The behavior for this chat is not set");
//                })
//                .build();
//    }
//
//}
