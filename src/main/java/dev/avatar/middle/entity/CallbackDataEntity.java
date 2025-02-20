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

import java.util.UUID;

@Entity
@Table(name = "telegram_callback_data")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CallbackDataEntity {

    @Id
    private final UUID id;

    private final String data;

    public static CallbackDataEntity of(String data) {
        return new CallbackDataEntity(UUID.randomUUID(), data);
    }
}
