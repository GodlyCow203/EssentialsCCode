package net.godlycow.org.essc.migration.model;

import java.util.UUID;

public record EssWarp(
        String name,
        EssLocation location,
        UUID lastOwner
) {}