package com.tpmod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TPDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dataFolder;
    
    // 플레이어별 홈 위치 (UUID -> 홈이름 -> 위치)
    private static final Map<UUID, Map<String, TPLocation>> playerHomes = new HashMap<>();
    
    // 공개 워프 포인트 (이름 -> 위치)
    private static final Map<String, TPLocation> warps = new HashMap<>();
    
    public static void loadData(MinecraftServer server) {
        dataFolder = server.getServerDirectory().toPath().resolve("tpmod_data");
        try {
            Files.createDirectories(dataFolder);
            loadHomes();
            loadWarps();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveData() {
        try {
            saveHomes();
            saveWarps();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // 홈 관리
    public static void setHome(ServerPlayer player, String name, TPLocation location) {
        UUID uuid = player.getUUID();
        playerHomes.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, location);
        saveData();
    }
    
    public static TPLocation getHome(ServerPlayer player, String name) {
        Map<String, TPLocation> homes = playerHomes.get(player.getUUID());
        return homes != null ? homes.get(name) : null;
    }
    
    public static Map<String, TPLocation> getPlayerHomes(ServerPlayer player) {
        return playerHomes.getOrDefault(player.getUUID(), new HashMap<>());
    }
    
    public static void deleteHome(ServerPlayer player, String name) {
        Map<String, TPLocation> homes = playerHomes.get(player.getUUID());
        if (homes != null) {
            homes.remove(name);
            saveData();
        }
    }
    
    // 워프 관리
    public static void setWarp(String name, TPLocation location) {
        warps.put(name, location);
        saveData();
    }
    
    public static TPLocation getWarp(String name) {
        return warps.get(name);
    }
    
    public static Map<String, TPLocation> getAllWarps() {
        return new HashMap<>(warps);
    }
    
    public static void deleteWarp(String name) {
        warps.remove(name);
        saveData();
    }
    
    // 파일 저장/로드
    private static void loadHomes() throws IOException {
        File file = dataFolder.resolve("homes.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, Map<String, TPLocation>> data = GSON.fromJson(reader, 
                    new TypeToken<Map<String, Map<String, TPLocation>>>(){}.getType());
                if (data != null) {
                    playerHomes.clear();
                    data.forEach((uuidStr, homes) -> 
                        playerHomes.put(UUID.fromString(uuidStr), homes));
                }
            }
        }
    }
    
    private static void saveHomes() throws IOException {
        Map<String, Map<String, TPLocation>> data = new HashMap<>();
        playerHomes.forEach((uuid, homes) -> data.put(uuid.toString(), homes));
        
        try (Writer writer = new FileWriter(dataFolder.resolve("homes.json").toFile())) {
            GSON.toJson(data, writer);
        }
    }
    
    private static void loadWarps() throws IOException {
        File file = dataFolder.resolve("warps.json").toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Map<String, TPLocation> data = GSON.fromJson(reader, 
                    new TypeToken<Map<String, TPLocation>>(){}.getType());
                if (data != null) {
                    warps.clear();
                    warps.putAll(data);
                }
            }
        }
    }
    
    private static void saveWarps() throws IOException {
        try (Writer writer = new FileWriter(dataFolder.resolve("warps.json").toFile())) {
            GSON.toJson(warps, writer);
        }
    }
}
