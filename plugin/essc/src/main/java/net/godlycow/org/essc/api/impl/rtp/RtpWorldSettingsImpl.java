package net.godlycow.org.essc.api.impl.rtp;

import net.godlycow.org.essc.api.rtp.RtpWorldSettings;
import net.godlycow.org.essc.modules.rtp.RTPManager.WorldRTPSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RtpWorldSettingsImpl implements RtpWorldSettings {
    private final String worldName;
    private final WorldRTPSettings internal;

    public RtpWorldSettingsImpl(String worldName, WorldRTPSettings internal) {
        this.worldName = worldName;
        this.internal = internal;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Override
    public String getDisplayName() {
        return internal.displayName();
    }

    @Override
    public int getMinRadius() {
        return internal.minRadius();
    }

    @Override
    public int getMaxRadius() {
        return internal.maxRadius();
    }

    @Override
    public List<String> getBlockedBiomes() {
        return Collections.unmodifiableList(new ArrayList<>(internal.blockedBiomes()));
    }

    @Override
    public boolean isEnabled() {
        return internal.enabled();
    }
}