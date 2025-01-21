package dev.avatar.middle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assistant")
@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "of")
public class AssistantEntity {

    @Id
    private final String assistantId;

    private final String telegramBotId;
}
