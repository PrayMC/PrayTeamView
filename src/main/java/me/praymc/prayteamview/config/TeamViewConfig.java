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
    private static TeamViewConfig instance;

    public TeamViewSettings teamView = new TeamViewSettings();
    public RallyPointSettings rallyPoint = new RallyPointSettings();

    public static final class TeamViewSettings {
        public boolean enabled = true;
        public boolean screenMarkers = true;
        public boolean directionHud = true;
        // 바닐라 네임태그가 사라지는 64블록부터 이어받는다.
        public double distantDetailsDistance = 64.0;
        public long remoteInterpolationMillis = 1000L;
        public double teleportSnapDistance = 32.0;
        public long snapshotTtlMillis = 3000L;
        public double maxRenderDistance = 4096.0;
    }

    public static final class RallyPointSettings {
        public boolean enabled = true;
        public double maxRenderDistance = 4096.0;
    }

    public static TeamViewConfig load() {
        if (instance != null) return instance;
        TeamViewConfig loaded = null;
        boolean save = !Files.exists(PATH);
        if (!save) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                loaded = GSON.fromJson(reader, TeamViewConfig.class);
            } catch (IOException | JsonParseException ignored) {
            }
        }
        if (loaded == null) loaded = new TeamViewConfig();
        if (loaded.teamView == null) loaded.teamView = new TeamViewSettings();
        if (loaded.rallyPoint == null) loaded.rallyPoint = new RallyPointSettings();
        instance = loaded;
        if (save) save(loaded);
        return instance;
    }

    private static void save(TeamViewConfig config) {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
