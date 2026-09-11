package net.godlycow.org.essc.plugin.dump;

import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

public class PasteUploadClient {

    public static String upload(File dumpFile) throws IOException {
        byte[] bodyBytes = Files.readAllBytes(dumpFile.toPath());

        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(URI.create("https://api.pastes.dev/post")).header("Content-Type", "text/json").header("User-Agent", "EssentialsC").POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes)).build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Upload interrupted", e);
        }

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Upload failed with status " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonParser parser = new JsonParser();

            String key = parser.parse(response.body()).getAsJsonObject().get("key").getAsString();

            return "https://dumps.godlycow.org/?d=" + key;
        } catch (Exception e) {
            throw new IOException("Malformed response: " + response.body(), e);
        }
    }
}