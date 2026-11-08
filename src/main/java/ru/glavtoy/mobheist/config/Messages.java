package ru.glavtoy.mobheist.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class Messages {

    private final ConfigLoader configLoader;
    private FileConfiguration config;

    public void load() {
        config = configLoader.loadMessages();
    }

    public String get(String key) {
        String message = config.getString(key);
        return ChatColor.translateAlternateColorCodes('&', message != null ? message : key);
    }

    public List<String> getList(String key) {
        return config.getStringList(key).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }
}
