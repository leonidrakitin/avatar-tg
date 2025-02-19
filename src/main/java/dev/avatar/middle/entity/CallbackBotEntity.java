package dev.avatar.middle.entity;

import dev.avatar.middle.model.CallbackType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "telegram_callbacks")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CallbackBotEntity {

    @Id
    private final UUID id;

    private final String botTokenId;

    private final Integer callbackMessageId;

    @Enumerated(EnumType.STRING)
    private final CallbackType callbackType;
}
