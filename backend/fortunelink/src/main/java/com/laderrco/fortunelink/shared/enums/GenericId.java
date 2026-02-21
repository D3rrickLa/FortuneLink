package com.laderrco.fortunelink.shared.enums;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public interface GenericId {
    static <T extends GenericId> T generate(Function<UUID, T> constructor) {
        return constructor.apply(UUID.randomUUID());
    }

    static <T extends GenericId> T fromString(Function<UUID, T> constructor, String value) {
        return constructor.apply(UUID.fromString(value));
    }

    static UUID validate(UUID value) {
        return Objects.requireNonNull(value, "ID value cannot be null");
    }

}