package net.okocraft.zihou;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ZihouVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ZihouConfig config;

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
            this.config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));
        } catch (IOException e) {
            this.logger.error("Could not load config.yml", e);
            return;
        }

        this.server.getScheduler()
            .buildTask(this, this::announceTime)
            .delay(calculateTaskDelay(Clock.systemUTC()))
            .repeat(Duration.ofHours(1))
            .schedule();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent ignored) {
        this.server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
    }

    private void announceTime() {
        LocalDateTime now = getAdjustedNow(Clock.systemUTC());
        this.server.sendMessage(this.config.createMessageComponent(now));
    }

    @VisibleForTesting
    static LocalDateTime getAdjustedNow(Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        return now.getSecond() == 59 ? now.truncatedTo(ChronoUnit.HOURS).plusHours(1) : now;
    }

    @VisibleForTesting
    static Duration calculateTaskDelay(Clock clock) {
        Instant now = Instant.now(clock);
        Instant next = now.plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
        return Duration.between(now, next);
    }
}
