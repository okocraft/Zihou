package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

class ZihouConfigTest {

    @Test
    void loadFromYaml_FileNotExists(@TempDir Path dir) throws IOException {
        Path filepath = dir.resolve("config.yml");
        ZihouConfig config = ZihouConfig.loadFromYaml(filepath);
        Assertions.assertEquals(ZihouConfig.DEFAULT_MESSAGE, config.message());

        Assertions.assertEquals(
                "message: <dark_gray>[<blue>時報<dark_gray>] <gray><hour>時<minute>分<second>秒になりました\n",
                Files.readString(filepath, StandardCharsets.UTF_8)
        );
    }

    @Test
    void loadFromYaml_FileExists(@TempDir Path dir) throws IOException {
        String customMessage = "<dark_gray>[<blue>カスタム時報<dark_gray>] <gray><hour>時<minute>分<second>秒になりました";
        String yamlContent = "message: " + customMessage + "\n";
        Path filepath = dir.resolve("config.yml");
        Files.writeString(filepath, yamlContent, StandardCharsets.UTF_8);

        ZihouConfig config = ZihouConfig.loadFromYaml(filepath);
        Assertions.assertEquals(customMessage, config.message());
        Assertions.assertEquals(yamlContent, Files.readString(filepath, StandardCharsets.UTF_8));
    }

    @Test
    void loadFromYaml_MessageNotFound(@TempDir Path dir) throws IOException {
        String yamlContent = "other-key: other-value\n";
        Path filepath = dir.resolve("config.yml");
        Files.writeString(filepath, yamlContent, StandardCharsets.UTF_8);

        ZihouConfig config = ZihouConfig.loadFromYaml(filepath);
        Assertions.assertEquals(ZihouConfig.DEFAULT_MESSAGE, config.message());
        Assertions.assertEquals(yamlContent, Files.readString(filepath, StandardCharsets.UTF_8));
    }

    @Test
    void createMessageComponent_DefaultMessage() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        Assertions.assertEquals(
                MiniMessage.miniMessage().deserialize("<dark_gray>[<blue>時報<dark_gray>] <gray>3時4分5秒になりました"),
                new ZihouConfig(ZihouConfig.DEFAULT_MESSAGE).createMessageComponent(time)
        );
    }

    @Test
    void createMessageComponent_Placeholder() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 3, 4, 5);
        Assertions.assertEquals(
                Component.text("2025年1月2日3時4分5秒"),
                new ZihouConfig("<year>年<month>月<day>日<hour>時<minute>分<second>秒").createMessageComponent(time)
        );
    }
}
