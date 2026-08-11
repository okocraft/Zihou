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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ZihouVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private final AtomicReference<RuntimeState> runtimeState = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> announcementTask = new AtomicReference<>();

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
            ZihouConfig config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));
            this.runtimeState.set(createRuntimeState(config));
        } catch (IOException e) {
            this.logger.error("Could not load config.yml", e);
            return;
        }

        RuntimeState state = this.runtimeState.get();
        if (state.config().tryParseTimezoneId() == null) {
            this.logger.warn("Invalid timezone id: {}", state.config().timezoneId());
        }

        this.scheduleAnnouncement(state);

        BrigadierCommand command = this.createCommand();
        CommandMeta meta = this.server.getCommandManager().metaBuilder(command).plugin(this).build();
        this.server.getCommandManager().register(meta, command);
    }

    private void scheduleAnnouncement(RuntimeState state) {
        ScheduledTask newTask = this.server.getScheduler()
            .buildTask(this, this::announceTime)
            .delay(calculateTaskDelay(state.clock()))
            .repeat(Duration.ofHours(1))
            .schedule();
        replaceTask(this.announcementTask, newTask);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent ignored) {
        ScheduledTask task = this.announcementTask.getAndSet(null);
        if (task != null) {
            task.cancel();
        }
    }

    private void announceTime() {
        RuntimeState state = this.runtimeState.get();
        LocalDateTime now = getAdjustedNow(state.clock());
        this.server.sendMessage(state.config().createMessageComponent(now));
    }

    private BrigadierCommand createCommand() {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("zihou")
                .requires(source -> source.hasPermission("zihou.command"))
                .then(
                    BrigadierCommand.literalArgumentBuilder("reload")
                        .executes(context -> {
                            try {
                                ZihouConfig config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));
                                RuntimeState state = createRuntimeState(config);
                                this.runtimeState.set(state);
                                this.scheduleAnnouncement(state);
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
                            RuntimeState state = this.runtimeState.get();
                            LocalDateTime now = getAdjustedNow(state.clock());
                            context.getSource().sendMessage(state.config().createMessageComponent(now));
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
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime next = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return Duration.between(now.toInstant(), next.toInstant());
    }

    @VisibleForTesting
    static RuntimeState createRuntimeState(ZihouConfig config) {
        ZoneId zoneId = config.tryParseTimezoneId();
        return new RuntimeState(config, zoneId == null ? Clock.systemDefaultZone() : Clock.system(zoneId));
    }

    @VisibleForTesting
    static void replaceTask(AtomicReference<ScheduledTask> taskReference, ScheduledTask newTask) {
        ScheduledTask oldTask = taskReference.getAndSet(newTask);
        if (oldTask != null) {
            oldTask.cancel();
        }
    }

    record RuntimeState(ZihouConfig config, Clock clock) {
    }
}
