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
import java.util.function.Consumer;

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
            this.configRef.set(this.loadAndValidate(this.logger::warn));
        } catch (IOException e) {
            this.logger.error("Could not load config.yml: {}", e.getMessage());
            return;
        }

        this.scheduleNext();
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
                            try {
                                this.configRef.set(this.loadAndValidate(warn -> context.getSource().sendMessage(Component.text(warn, NamedTextColor.YELLOW))));
                            } catch (IOException e) {
                                context.getSource().sendMessage(Component.text("Failed to reload config.yml: " + e.getMessage(), NamedTextColor.RED));
                                return 0;
                            }

                            this.scheduleNext();
                            context.getSource().sendMessage(Component.text("config.yml reloaded.", NamedTextColor.GRAY));
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

    private ZihouConfig loadAndValidate(Consumer<String> warn) throws IOException {
        ZihouConfig config = ZihouConfig.loadFromYaml(this.dataDirectory.resolve("config.yml"));

        ZoneId zoneId = config.tryParseTimezoneId();
        if (zoneId == null) {
            warn.accept("Could not parse timezone id: " + config.timezoneId());
        }

        return config;
    }

    private synchronized void scheduleNext() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }

        Zihou zihou = Zihou.create(this.configRef.get());
        this.scheduledTask = this.server.getScheduler()
            .buildTask(this, () -> {
                zihou.announce(this.server);
                this.scheduleNext();
            })
            .delay(zihou.calculateTaskDelay())
            .schedule();
    }
}
