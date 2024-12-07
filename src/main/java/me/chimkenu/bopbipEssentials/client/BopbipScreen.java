package me.chimkenu.bopbipEssentials.client;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

import static me.chimkenu.bopbipEssentials.client.BopbipEssentialsClient.*;

public class BopbipScreen extends Screen {
    public BopbipScreen (MinecraftClient client) {
        super(Text.of("Loading bopbip..."));
        this.client = client;
        if (client == null || hasEncounteredError(client)) {
            return;
        }

        firstThen(() -> updateLocalDirectory(client), () -> client.execute(() -> {
            if (isBopbipOnline(client)) {
                handleOnlineBopbip(client);
            } else {
                handleOfflineBopbip(client);
            }
        }));
    }

    private void handleOnlineBopbip(MinecraftClient client) {
        if (!getBopbipHost(client).equals(client.getSession().getUsername())) {
            client.setScreen(new NoticeScreen(() -> client.setScreen(null), Text.of("Bopbip is currently being hosted by " + getBopbipHost(client) + ", go join them!"), Text.of("NOTE: If this is no longer the case, wait a few minutes for this to reflect on the client.")));
            return;
        }

        client.setScreen(new ConfirmScreen(b -> {
            if (!b) {
                client.setScreen(null);
                return;
            }

            firstThen(() -> client.execute(() ->
                    client.setScreen(new MessageScreen(Text.of("Uploading world to server...")))
            ), () -> {
                if (modifyBopbipHost(client, false)) {
                    client.execute(() -> client.setScreen(new NoticeScreen(() -> client.setScreen(null), Text.of("You are no longer hosting!"), Text.empty())));
                } else {
                    client.execute(() -> client.setScreen(new NoticeScreen(() -> client.setScreen(null), Text.of("something went wrong, contact chimkenu"), Text.empty())));
                }
            });
        }, Text.of("It looks like you've already claimed hosting."), Text.of("Stop hosting?")));
    }

    private void handleOfflineBopbip(MinecraftClient client) {
        client.setScreen(new ConfirmScreen(b -> {
            if (!b) {
                client.setScreen(null);
                return;
            }

            client.setScreen(new MessageScreen(Text.of("Fetching latest bopbip world...\nIf this is taking too long (over 3 minutes), close the game and delete your bopbip world folder.")));
            firstThen(() -> updateLocalDirectory(client), () -> firstThen(() -> {
                if (modifyBopbipHost(client, true)) {
                    client.execute(() -> client.setScreen(new NoticeScreen(() -> client.setScreen(null), Text.of("Fetching latest bopbip world... Done!"), Text.of("Open the world in Singleplayer and host using the Essential mod!"), Text.of("Close"), true)));
                } else {
                    client.setScreen(new NoticeScreen(() -> client.setScreen(null), Text.of("Fetching latest bopbip world..."), Text.of("ping chimkenu, something went wrong."), Text.of("Close"), true));
                }
            }, () -> {}));
        }, Text.of("Bopbip is currently offline. Host?"), Text.of("NOTE: You MUST ensure that your game closes properly after playing.")));
    }

    private void firstThen(Runnable first, Runnable then) {
        CompletableFuture<Void> start = CompletableFuture.runAsync(first);
        start.thenRun(then);
    }
}
