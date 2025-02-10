package dev.avatar.middle.entity;

import jakarta.persistence.Column;
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
@Table(name = "eleven_labs_data")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ElevenLabsData {

    @Id
    private final UUID id;

    @Column(nullable = false)
    private final String voiceId;

    @Column(nullable = false)
    private final String voiceModelId;

    @Column(nullable = false, unique = true)
    private final String botTokenId;
}
