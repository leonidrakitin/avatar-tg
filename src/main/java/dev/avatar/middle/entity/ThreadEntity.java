package dev.avatar.middle.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "thread")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadEntity {

    @Id
    private final String threadId;

    private final Long telegramChatId;

    private final LocalDate creationDate;

    private final LocalDate deprecatedAt;

    public static ThreadEntity of(String threadId, Long telegramChatId) {
        return ThreadEntity.builder()
                .threadId(threadId)
                .telegramChatId(telegramChatId)
                .build();
    }
}
