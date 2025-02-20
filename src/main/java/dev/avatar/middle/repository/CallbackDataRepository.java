package dev.avatar.middle.repository;

import dev.avatar.middle.entity.CallbackDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CallbackDataRepository extends JpaRepository<CallbackDataEntity, UUID> {
}
