package com.laderrco.fortunelink.shared.valueobjects;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public interface GenericId {
    static <T extends GenericId> T random(Function<UUID, T> constructor) {
        return constructor.apply(UUID.randomUUID());
    }

    static UUID validate(UUID value) {
        return Objects.requireNonNull(value, "ID value cannot be null");
    } 
}
