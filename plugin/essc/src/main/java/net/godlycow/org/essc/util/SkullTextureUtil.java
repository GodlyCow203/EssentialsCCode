package net.godlycow.org.essc.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public final class SkullTextureUtil {

    private SkullTextureUtil() {}

    public static void applyTexture(SkullMeta meta, String value, Logger logger) {
        if (value == null || value.isBlank()) {
            return;
        }

        String skinUrl = resolveToUrl(value, logger);
        if (skinUrl == null) {
            return;
        }

        try {
            UUID deterministicId = UUID.nameUUIDFromBytes(skinUrl.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(deterministicId, "");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            logger.warning("SkullTextureUtil: malformed skin URL: " + skinUrl);
        }
    }

    private static String resolveToUrl(String value, Logger logger) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }

        if (looksLikeBase64(value)) {
            return extractUrlFromBase64(value, logger);
        }

        return "https://textures.minecraft.net/texture/" + value;
    }

    private static boolean looksLikeBase64(String value) {
        return value.length() > 100 && value.chars().allMatch(c ->
                Character.isLetterOrDigit(c) || c == '+' || c == '/' || c == '=');
    }

    private static String extractUrlFromBase64(String base64, Logger logger) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int urlStart = json.indexOf("\"url\":\"");
            if (urlStart == -1) {
                logger.warning("SkullTextureUtil: no url found in base64 texture JSON");
                return null;
            }
            urlStart += 7;
            int urlEnd = json.indexOf("\"", urlStart);
            if (urlEnd == -1) {
                logger.warning("SkullTextureUtil: malformed url in base64 texture JSON");
                return null;
            }
            return json.substring(urlStart, urlEnd);
        } catch (IllegalArgumentException e) {
            logger.warning("SkullTextureUtil: failed to decode base64 texture value");
            return null;
        }
    }
}
