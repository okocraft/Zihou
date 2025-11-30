package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@NotNullByDefault
public record ZihouConfig(String message) {

    @VisibleForTesting
    static final String DEFAULT_MESSAGE = "<dark_gray>[<blue>時報<dark_gray>] <gray><hour>時<minute>分<second>秒になりました";

    public static ZihouConfig loadFromYaml(Path filepath) throws IOException{
        if (!Files.isRegularFile(filepath)) {
            saveDefaultConfig(filepath);
            return new ZihouConfig(DEFAULT_MESSAGE);
        }

        ConfigurationNode root = YamlConfigurationLoader.builder().path(filepath).build().load();
        String message = root.node("message").getString(DEFAULT_MESSAGE);
        return new ZihouConfig(message);
    }

    private static void saveDefaultConfig(Path filepath) throws IOException {
        ConfigurationNode root = BasicConfigurationNode.root();
        root.node("message").set(DEFAULT_MESSAGE);
        YamlConfigurationLoader.builder().path(filepath).nodeStyle(NodeStyle.BLOCK).build().save(root);
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
