package me.praymc.prayteamview.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TeamViewConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("prayteamview.json");

    public boolean enabled = true;
    public boolean screenMarkers = true;
    public boolean directionHud = true;
    // 바닐라 네임태그가 사라지는 64블록부터 이어받는다 — 라벨이 끊기는 구간이 없도록
    public double distantDetailsDistance = 64.0;
    public long remoteInterpolationMillis = 1000L;
    public double teleportSnapDistance = 32.0;
    public long snapshotTtlMillis = 3000L;
    public double maxRenderDistance = 4096.0;

    public static TeamViewConfig load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                TeamViewConfig loaded = GSON.fromJson(reader, TeamViewConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException | JsonParseException ignored) {
            }
        }

        TeamViewConfig defaults = new TeamViewConfig();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(defaults, writer);
            }
        } catch (IOException ignored) {
        }
        return defaults;
    }
}
