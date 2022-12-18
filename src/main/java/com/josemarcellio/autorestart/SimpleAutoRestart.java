package com.josemarcellio.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleAutoRestart extends JavaPlugin {

    private Map<DayOfWeek, List<String>> commands;
    private long announcementIntervalTicks;
    private long restartDelayTicks;
    private Map<Long, String> announcementMessages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadCommands();
        loadConfig();
        loadAnnouncementMessages();
        scheduleNextRestart();
    }

    private void loadCommands() {
        FileConfiguration config = getConfig();
        commands = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<String> dayCommands = config.getStringList(day.name().toLowerCase() + "-commands");
            if (!dayCommands.isEmpty()) {
                commands.put(day, dayCommands);
            }
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        announcementIntervalTicks = config.getLong("announcement-interval-seconds");
        restartDelayTicks = config.getLong("restart-delay-seconds");
    }

    private void loadAnnouncementMessages() {
        FileConfiguration config = getConfig();
        announcementMessages = new HashMap<>();
        for (String key : config.getConfigurationSection("announcement-messages").getKeys(false)) {
            long seconds = Long.parseLong(key);
            String message = config.getString("announcement-messages." + key);
            message = ChatColor.translateAlternateColorCodes('&', message);
            announcementMessages.put(seconds, message);
        }
    }

    private void scheduleNextRestart() {
        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        if (commands.containsKey(today)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRestartTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0);
            if (now.isAfter(nextRestartTime)) {
                nextRestartTime = nextRestartTime.plusDays(1);
            }
            long delayTicks = (nextRestartTime.toEpochSecond(null) - now.toEpochSecond(null)) * 20L;
            Bukkit.getScheduler().runTaskLater(this, this::executeCommands, delayTicks);
        }
    }

    private void executeCommands() {
        long delayTicks = restartDelayTicks - (5 * announcementIntervalTicks);
        for (int i = 5; i > 0; i--) {
            final int countdown = i;
            Bukkit.getScheduler().runTaskLater(this, () -> announceRestart(countdown), delayTicks);
            delayTicks += announcementIntervalTicks;
        }
        Bukkit.getScheduler().runTaskLater(this, this::doRestart, restartDelayTicks);
    }

    private void announceRestart(int countdown) {
        long seconds = countdown * announcementIntervalTicks / 20L;
        if (announcementMessages.containsKey(seconds)) {
            Bukkit.broadcastMessage(announcementMessages.get(seconds));
        } else {
            Bukkit.broadcastMessage("Server restarting in " + countdown + " minutes");
        }
    }

    private void doRestart() {
        List<String> dayCommands = commands.get(LocalDateTime.now().getDayOfWeek());
        for (String command : dayCommands) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}