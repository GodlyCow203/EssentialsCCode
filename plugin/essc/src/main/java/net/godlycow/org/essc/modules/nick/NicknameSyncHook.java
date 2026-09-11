package net.godlycow.org.essc.modules.nick;

import java.util.UUID;

public interface NicknameSyncHook {

    void onNicknameSet(UUID uuid, String nickname);

    void onNicknameCleared(UUID uuid);
}