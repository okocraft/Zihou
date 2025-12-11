package net.okocraft.zihou;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class ZihouVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ZihouConfig config;
    private Clock clock;

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

        ZoneId zoneId = this.config.tryParseTimezoneId();
        if (zoneId == null) {
            this.logger.warn("Invalid timezone id: {}", this.config.timezoneId());
            this.clock = Clock.systemDefaultZone();
        } else {
            this.clock = Clock.system(zoneId);
        }

        this.server.getScheduler()
            .buildTask(this, this::announceTime)
            .delay(calculateTaskDelay(this.clock))
            .repeat(Duration.ofHours(1))
            .schedule();

        this.server.getCommandManager().metaBuilder(this.createCommand()).plugin(this).build();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent ignored) {
        this.server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
    }

    private void announceTime() {
        LocalDateTime now = getAdjustedNow(this.clock);
        this.server.sendMessage(this.config.createMessageComponent(now));
    }

    private BrigadierCommand createCommand() {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("zihou")
                .requires(source -> source.hasPermission("zihou.command"))
                .then(
                    BrigadierCommand.literalArgumentBuilder("reload")
                        .executes(context -> {
                            try {
                                this.config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));
                                context.getSource().sendMessage(Component.text("config.yml reloaded.", NamedTextColor.GRAY));
                            } catch (IOException e) {
                                context.getSource().sendMessage(Component.text("Failed to reload config.yml: " + e.getMessage(), NamedTextColor.RED));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(
                    BrigadierCommand.literalArgumentBuilder("test")
                        .executes(context -> {
                            LocalDateTime now = getAdjustedNow(this.clock);
                            context.getSource().sendMessage(this.config.createMessageComponent(now));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendMessage(Component.text("Usage: /zihou reload | /zihou test", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                })
        );
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
