package net.okocraft.zihou;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class ZihouVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private String zihouMessage;

    @Inject
    public ZihouVelocity(@NotNull ProxyServer server, @NotNull Logger logger,
                         @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent ignored) {
        try {
            ConfigurationNode node = YamlConfigurationLoader.builder().path(this.dataDirectory.resolve("config.yml")).build().load();
            this.zihouMessage = node.node("zihou-message").getString();
        } catch (IOException e) {
            this.logger.error("Could not load config.yml", e);
            return;
        }

        Instant now = Instant.now();
        Instant next = now.truncatedTo(ChronoUnit.HOURS);

        if (next.isBefore(now)) {
            next = next.plusSeconds(TimeUnit.HOURS.toSeconds(1));
        }

        this.server.getScheduler()
                .buildTask(this, this::announceTime)
                .delay(Duration.between(now, next))
                .repeat(Duration.ofHours(1))
                .schedule();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent ignored) {
        this.server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
    }

    private void announceTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.getSecond() == 59 ? now.truncatedTo(ChronoUnit.HOURS).plusHours(1) : now;

        this.server.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                this.zihouMessage.replace("%hour%", String.valueOf(time.getHour()))
                        .replace("%minute%", String.valueOf(time.getMinute()))
                        .replace("%second%", String.valueOf(time.getSecond()))
        ));
    }
}
