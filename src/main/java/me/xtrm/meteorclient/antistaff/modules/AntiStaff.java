package me.xtrm.meteorclient.antistaff.modules;

import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.text.Text;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

public class AntiStaff extends Module {
    private static final Random RANDOM = new SecureRandom();

    public AntiStaff() {
        super(Categories.Misc, "anti-staff", "Detect player logins on servers with poorly-configured vanish plugins.");
    }

    private final List<String> inboundIDs = new ArrayList<>();
    private List<String> cachedUsernames = new ArrayList<>();
    private int tickTimer = 0;

    @EventHandler
    public void onUpdate(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.isInSingleplayer()) return;

        if (tickTimer++ < 20) return;
        tickTimer = 0;

        int id = RANDOM.nextInt(0, 200000);
        inboundIDs.add(String.valueOf(id));
        mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(id, "/msg "));
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
            if (inboundIDs.contains(String.valueOf(packet.getCompletionId()))) {
                var lastUsernames = cachedUsernames.stream().toList();

                cachedUsernames = packet.getSuggestions().getList().stream()
                    .map(Suggestion::getText)
                    .toList();

                if (lastUsernames.isEmpty()) {
                    return;
                }

                Predicate<String> joinedOrQuit = playerName -> lastUsernames.contains(playerName) != cachedUsernames.contains(playerName);

                Predicate<String> isHidden = playerName -> mc.player.networkHandler.getPlayerList().stream()
                    .map(PlayerListEntry::getDisplayName)
                    .filter(Objects::nonNull)
                    .map(Text::getString)
                    .filter(str -> !Objects.equals(mc.player.getName().getString(), str))
                    .noneMatch(playerName::equals);

                List<String> silentJoiners = new ArrayList<>();
                for (String playerName : cachedUsernames) {
                    if (Objects.equals(playerName, mc.player.getName().getString())) continue;
                    if (isHidden.test(playerName)) {
                        if (joinedOrQuit.test(playerName)) {
                            silentJoiners.add(playerName);
                        }
                    }
                }
                if (!silentJoiners.isEmpty()) {
                    ChatUtils.info("Anti Staff", silentJoiners.size() + " player" + (silentJoiners.size() == 1 ? "" : "s") + " joined: " + String.join(", ", silentJoiners));
                }

                List<String> quitters = new ArrayList<>();
                for (String playerName : lastUsernames) {
                    if (Objects.equals(playerName, mc.player.getName().getString())) continue;
                    if (joinedOrQuit.test(playerName)) {
                        quitters.add(playerName);
                    }
                }
                if (!quitters.isEmpty()) {
                    ChatUtils.info("Anti Staff", quitters.size() + " player" + (quitters.size() == 1 ? "" : "s") + " left: " + String.join(", ", quitters));
                }

                inboundIDs.remove(String.valueOf(packet.getCompletionId()));
                event.cancel();
            }
        }
    }
}
