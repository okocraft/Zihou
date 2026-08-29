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
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class ZihouVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private final AtomicReference<ZihouConfig> configRef = new AtomicReference<>(ZihouConfig.DEFAULT);
    private ScheduledTask scheduledTask;

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
            this.configRef.set(ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml")));
        } catch (IOException e) {
            this.logger.error("Could not load config.yml: {}", e.getMessage());
        }

        ZoneId zoneId = this.configRef.get().tryParseTimezoneId();
        if (zoneId == null) {
            this.logger.warn("Could not parse timezone id: {}", this.configRef.get().timezoneId());
        }

        this.scheduleNext(false);
        this.registerCommand();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent ignored) {
        this.server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
    }

    private void registerCommand() {
        BrigadierCommand command = this.createCommand();
        CommandMeta meta = this.server.getCommandManager().metaBuilder(command).plugin(this).build();
        this.server.getCommandManager().register(meta, command);
    }

    private BrigadierCommand createCommand() {
        return new BrigadierCommand(
            BrigadierCommand.literalArgumentBuilder("zihou")
                .requires(source -> source.hasPermission("zihou.command"))
                .then(
                    BrigadierCommand.literalArgumentBuilder("reload")
                        .executes(context -> {
                            ZihouConfig config;
                            try {
                                config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));
                            } catch (IOException e) {
                                context.getSource().sendMessage(Component.text("Zihou: Failed to reload config.yml: " + e.getMessage(), NamedTextColor.RED));
                                return 0;
                            }

                            ZoneId zoneId = config.tryParseTimezoneId();
                            if (zoneId == null) {
                                context.getSource().sendMessage(Component.text("Zihou: Could not parse timezone id: " + config.timezoneId(), NamedTextColor.YELLOW));
                                return 0;
                            }

                            this.configRef.set(config);
                            this.scheduleNext(true);
                            context.getSource().sendMessage(Component.text("Zihou: config.yml reloaded", NamedTextColor.GRAY));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(
                    BrigadierCommand.literalArgumentBuilder("test")
                        .executes(context -> {
                            Zihou.create(this.configRef.get()).announce(context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendMessage(Component.text("Usage: /zihou reload | /zihou test", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                })
        );
    }

    private synchronized void scheduleNext(boolean cancel) {
        if (cancel && this.scheduledTask != null) {
            this.scheduledTask.cancel();
        }

        Zihou zihou = Zihou.create(this.configRef.get());
        this.scheduledTask = this.server.getScheduler()
            .buildTask(this, () -> {
                zihou.announce(this.server);
                this.scheduleNext(false);
            })
            .delay(zihou.calculateTaskDelay())
            .schedule();
    }
}
