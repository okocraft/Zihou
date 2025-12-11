package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@NotNullByDefault
public record ZihouConfig(String message, String timezoneId) {

    @VisibleForTesting
    static final String DEFAULT_MESSAGE = "<dark_gray>[<blue>時報<dark_gray>] <gray><hour>時<minute>分<second>秒になりました";

    public static ZihouConfig loadFromYaml(Path filepath) throws IOException {
        if (!Files.isRegularFile(filepath)) {
            saveDefaultConfig(filepath);
            return new ZihouConfig(DEFAULT_MESSAGE, "");
        }

        ConfigurationNode root = YamlConfigurationLoader.builder().path(filepath).build().load();
        String message = root.node("message").getString(DEFAULT_MESSAGE);
        String timezoneId = root.node("timezone-id").getString("");
        return new ZihouConfig(message, timezoneId);
    }

    private static void saveDefaultConfig(Path filepath) throws IOException {
        ConfigurationNode root = BasicConfigurationNode.root();
        root.node("message").set(DEFAULT_MESSAGE);
        root.node("timezone-id").set(ZoneId.systemDefault().getId());
        YamlConfigurationLoader.builder().path(filepath).nodeStyle(NodeStyle.BLOCK).build().save(root);
    }

    public @Nullable ZoneId tryParseTimezoneId() {
        if (this.timezoneId.isEmpty() || this.timezoneId.equalsIgnoreCase("default")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(this.timezoneId);
        } catch (DateTimeException ignored) {
            return null;
        }
    }

    public Component createMessageComponent(LocalDateTime time) {
        return MiniMessage.miniMessage().deserialize(
            this.message,
            Placeholder.component("year", Component.text(time.getYear())),
            Placeholder.component("month", Component.text(time.getMonthValue())),
            Placeholder.component("day", Component.text(time.getDayOfMonth())),
            Placeholder.component("hour", Component.text(time.getHour())),
            Placeholder.component("minute", Component.text(time.getMinute())),
            Placeholder.component("second", Component.text(time.getSecond()))
        );
    }
}
