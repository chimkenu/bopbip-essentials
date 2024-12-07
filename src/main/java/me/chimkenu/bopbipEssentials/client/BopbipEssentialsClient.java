package me.chimkenu.bopbipEssentials.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.text.Text;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;

public class BopbipEssentialsClient implements ClientModInitializer {
    public static me.chimkenu.bopbipEssentials.client.BopbipConfig CONFIG = me.chimkenu.bopbipEssentials.client.BopbipConfig.createAndLoad();
    private static BopbipError error = BopbipError.NONE;

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            File world = new File(client.runDirectory, "saves");
            File bopbipWorld = new File(world, "bopbip");
            File host = new File(bopbipWorld, "host.txt");
            if (bopbipWorld.exists() && !host.exists()) {
                System.out.println("error: bopbip directory in use");
                error = BopbipError.DIRECTORY_IN_USE;
            }
            if (host.exists() && scanFirstLine(host).equals(client.getSession().getUsername())) {
                System.out.println("error: host.txt is still user");
                error = BopbipError.INVALID_HOST;
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (getBopbipHost(client).equals(client.getSession().getUsername())) {
                modifyBopbipHost(client, false);
            }
        });
    }

    public static boolean hasEncounteredError(MinecraftClient client) {
        switch (error) {
            case DIRECTORY_IN_USE -> client.setScreen(new MessageScreen(Text.of("This is to warn you that you have a world folder named 'bopbip' that does not have the necessary data. The Bopbip Essentials mod requires that the 'bopbip' folder in your worlds folder is used exclusively for the bopbip SMP. To ensure no data is lost, contact Chimkenu. Otherwise, to get rid of this message, make sure that there is no bopbip folder in your worlds folder then relaunch the game.")));
            case INVALID_HOST -> client.setScreen(new MessageScreen(Text.of("This is to warn you that the state of your most recent bopbip world is invalid. This does not mean that your progress is gone or corrupted, but rather that hosting was not managed properly in the previous session (Hosting was not released before closing the game). To ensure no data is lost, contact Chimkenu. Otherwise, to get rid of this message, delete the bopbip world folder and relaunch the game.")));
            default -> { return false; }
        }
        return true;
    }

    public static boolean modifyBopbipHost(MinecraftClient client, boolean claim) {
        File bopbipWorld = getBopbipDirectory(client);
        File host = new File(bopbipWorld, "host.txt");
        File gitDir = new File(bopbipWorld, ".git");
        if (!host.exists() || !gitDir.exists()) {
            return false;
        }

        if (!claim) {
            try {
                File level = new File(bopbipWorld, "level.dat");

                NbtSizeTracker tracker = new NbtSizeTracker(level.getTotalSpace(), Integer.MAX_VALUE);
                NbtCompound nbt = NbtIo.readCompressed(level.toPath(), tracker);
                nbt.getCompound("Data").remove("Player");
                NbtIo.writeCompressed(nbt, level.toPath());

                File old = new File(bopbipWorld, "level.dat_old");
                NbtCompound nbtOld = NbtIo.readCompressed(old.toPath(), tracker);
                nbtOld.getCompound("Data").remove("Player");
                NbtIo.writeCompressed(nbtOld, level.toPath());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            FileWriter writer = new FileWriter(host);
            writer.write(claim ? client.getSession().getUsername() : "NONE");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            Git git = Git.open(gitDir);
            git.add()
                    .addFilepattern(".")
                    .call();
            git.commit()
                    .setAll(true)
                    .setMessage(claim ? "New host: " + client.getSession().getUsername() : "Reset host")
                    .setSign(false)
                    .call();
            git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(CONFIG.github_link()))
                    .call();
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(CONFIG.github_key(), ""))
                    .add("main")
                    .call();
            git.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static String getBopbipHost(MinecraftClient client) {
        File hostFile = new File(getBopbipDirectory(client), "host.txt");
        if (!hostFile.exists()) {
            throw new RuntimeException();
        }

        return scanFirstLine(hostFile);
    }

    public static boolean isBopbipOnline(MinecraftClient client) {
        String host = "NONE";
        try {
            host = getBopbipHost(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return !host.equals("NONE");
    }

    public static File getBopbipDirectory(MinecraftClient client) {
        File world = new File(client.runDirectory, "saves");
        File bopbipWorld = new File(world, "bopbip");
        if (!bopbipWorld.exists()) {
            updateLocalDirectory(client);
            return getBopbipDirectory(client);
        }

        return bopbipWorld;
    }

    public static void updateLocalDirectory(MinecraftClient client) {
        File worlds = new File(client.runDirectory, "saves");
        File bopbipWorld = new File(worlds, "bopbip");
        if (!bopbipWorld.exists()) {
            bopbipWorld.mkdirs();
        }
        File gitDir = new File(bopbipWorld, ".git");
        if (!gitDir.exists() && !cloneGithubRepo(bopbipWorld)) {
            throw new RuntimeException("Something fucked up");
        }
        try {
            Git git = Git.open(gitDir);
            git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName("main")
                    .setRebase(true)
                    .call();
            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .call();
            git.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean cloneGithubRepo(File dir) {
        try {
            Git.cloneRepository()
                    .setURI(CONFIG.github_link())
                    .setDirectory(dir)
                    .call().close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private static String scanFirstLine(File file) {
        String line;
        try {
            Scanner scanner = new Scanner(file);
            line = scanner.nextLine();
            scanner.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return line;
    }
}
