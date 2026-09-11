package net.godlycow.org.essc.plugin.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiSession {
    private final UUID playerUuid;
    private final String guiId;
    private final int page;
    private final Map<String, Object> data;
    private final long createdAt;

    public GuiSession(UUID playerUuid, String guiId, int page, Map<String, Object> data) {
        this.playerUuid = playerUuid;
        this.guiId = guiId;
        this.page = page;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public static GuiSession create(UUID playerUuid, String guiId) {
        return new GuiSession(playerUuid, guiId, 1, null);
    }

    public static GuiSession create(UUID playerUuid, String guiId, int page) {
        return new GuiSession(playerUuid, guiId, page, null);
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getGuiId() {
        return guiId;
    }

    public int getPage() {
        return page;
    }
    public Map<String, Object> getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) data.get(key);
    }

    public boolean isStale() {
        return System.currentTimeMillis() - createdAt > 300_000L;
    }
}