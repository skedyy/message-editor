package dev.jaqobb.message_editor.listener.player;

import com.comphenix.protocol.utility.MinecraftVersion;
import dev.jaqobb.message_editor.MessageEditorPlugin;
import dev.jaqobb.message_editor.message.MessageEditData;
import dev.jaqobb.message_editor.message.MessagePlace;
import dev.jaqobb.message_editor.util.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerChatListener implements Listener {
    
    private final MessageEditorPlugin plugin;
    
    public PlayerChatListener(MessageEditorPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MessageEditData editData = this.plugin.getCurrentMessageEditData(player.getUniqueId());
        if (editData == null) {
            return;
        }
        MessageEditData.Mode editDataMode = editData.getCurrentMode();
        if (editDataMode == MessageEditData.Mode.NONE) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equals("done")) {
            editData.setCurrentMode(MessageEditData.Mode.NONE);
            if (editDataMode == MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_KEY || editDataMode == MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_VALUE) {
                editData.setOldMessagePatternKey("");
            } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE) {
                if (!editData.getNewMessageCache().isEmpty()) {
                    editData.setNewMessage(editData.getNewMessageCache());
                    if (MessageUtils.isJson(editData.getNewMessage())) {
                        editData.setNewMessageJson(true);
                    } else {
                        editData.setNewMessage(MessageUtils.translate(editData.getNewMessage()));
                        editData.setNewMessageJson(false);
                    }
                    editData.setNewMessageCache("");
                }
            } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE_KEY || editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE_VALUE) {
                editData.setNewMessageKey("");
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getMenuManager().openMenu(player, editData, true));
        } else if (editDataMode == MessageEditData.Mode.EDITING_FILE_NAME) {
            if (message.charAt(0) == '#') {
                MessageUtils.sendErrorSound(player);
                MessageUtils.sendPrefixedMessage(player, "&cMessage edit file name cannot start with the '&7#&c' character.");
                return;
            }
            File file = new File(this.plugin.getDataFolder(), "edits" + File.separator + message + ".yml");
            if (file.exists()) {
                MessageUtils.sendErrorSound(player);
                MessageUtils.sendPrefixedMessage(player, "&cThere is already a message edit that uses a file named '&7" + message + ".yml&c'.");
                return;
            }
            editData.setCurrentMode(MessageEditData.Mode.NONE);
            editData.setFileName(message);
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getMenuManager().openMenu(player, editData, true));
        } else if (editDataMode == MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_KEY) {
            editData.setOldMessagePatternKey(message);
            editData.setCurrentMode(MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_VALUE);
            MessageUtils.sendSuccessSound(player);
            MessageUtils.sendPrefixedMessage(player, "&7Now enter what you want it to be replaced with or '&edone&7' if you are done replacing.");
        } else if (editDataMode == MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_VALUE) {
            String patternKey = editData.getOldMessagePatternKey();
            String patternValue = message;
            editData.setCurrentMode(MessageEditData.Mode.EDITING_OLD_MESSAGE_PATTERN_KEY);
            editData.setOldMessage(editData.getOldMessage().replaceFirst(Pattern.quote(patternKey), Matcher.quoteReplacement(patternValue.replace("\\", "\\\\"))));
            editData.setOldMessagePattern(editData.getOldMessagePattern().replaceFirst(Pattern.quote(patternKey.replaceAll(MessageUtils.SPECIAL_REGEX_CHARACTERS, "\\\\$0")), Matcher.quoteReplacement(patternValue)));
            if (MessageUtils.isJson(editData.getOldMessage())) {
                editData.setOldMessageJson(true);
            } else {
                editData.setOldMessage(MessageUtils.translate(editData.getOldMessage()));
                editData.setOldMessagePattern(MessageUtils.translate(editData.getOldMessagePattern()));
                editData.setOldMessageJson(false);
            }
            editData.setOldMessagePatternKey("");
            MessageUtils.sendSuccessSound(player);
            MessageUtils.sendPrefixedMessage(player, "&7The first occurence of '&e" + patternKey + "&7' has been replaced with '&e" + patternValue + "&7'.");
            MessageUtils.sendPrefixedMessage(player, "&7Enter what you want to replace or '&edone&7' if you are done replacing.");
        } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE) {
            MessagePlace place = editData.getNewMessagePlace();
            if ((place == MessagePlace.GAME_CHAT || place == MessagePlace.SYSTEM_CHAT || place == MessagePlace.ACTION_BAR) && editData.getNewMessageCache().isEmpty() && message.equals("remove")) {
                editData.setCurrentMode(MessageEditData.Mode.NONE);
                editData.setNewMessage("");
                editData.setNewMessageJson(false);
                editData.setNewMessageCache("");
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getMenuManager().openMenu(player, editData, true));
                return;
            }
            editData.setNewMessageCache(editData.getNewMessageCache() + message);
            MessageUtils.sendSuccessSound(player);
            MessageUtils.sendPrefixedMessage(player, "&7Message has been added. Continue if your message is longer and had to be divided into parts. Otherwise enter '&edone&7' to set the new message.");
        } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE_KEY) {
            editData.setNewMessageKey(message);
            editData.setCurrentMode(MessageEditData.Mode.EDITING_NEW_MESSAGE_VALUE);
            MessageUtils.sendSuccessSound(player);
            MessageUtils.sendPrefixedMessage(player, "&7Now enter what you want it to be replaced with or '&edone&7' if you are done replacing.");
        } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE_VALUE) {
            String key = editData.getNewMessageKey();
            String value = message;
            editData.setCurrentMode(MessageEditData.Mode.EDITING_NEW_MESSAGE_KEY);
            editData.setNewMessage(editData.getNewMessage().replaceFirst(Pattern.quote(key), Matcher.quoteReplacement(value)));
            if (MessageUtils.isJson(editData.getNewMessage())) {
                editData.setNewMessageJson(true);
            } else {
                editData.setNewMessage(MessageUtils.translate(editData.getNewMessage()));
                editData.setNewMessageJson(false);
            }
            editData.setNewMessageKey("");
            MessageUtils.sendSuccessSound(player);
            MessageUtils.sendPrefixedMessage(player, "&7The first occurence of '&e" + key + "&7' has been replaced with '&e" + value + "&7'.");
            MessageUtils.sendPrefixedMessage(player, "&7Enter what you want to replace or '&edone&7' if you are done replacing.");
        } else if (editDataMode == MessageEditData.Mode.EDITING_NEW_MESSAGE_PLACE) {
            MessagePlace place = MessagePlace.fromName(message);
            if (place == null) {
                MessageUtils.sendErrorSound(player);
                MessageUtils.sendPrefixedMessage(player, "&cCould not convert '&7" + message + "&c' to a message place.");
                return;
            }
            if (!place.isSupported() || (place != MessagePlace.GAME_CHAT && place != MessagePlace.SYSTEM_CHAT && place != MessagePlace.ACTION_BAR)) {
                MessageUtils.sendIllegalOptionSound(player);
                MessageUtils.sendPrefixedMessage(player, "&cThis message place is not supported by your server or is unavailable.");
                MessageUtils.sendPrefixedMessage(player, "&7Available message places:");
                Collection<MessagePlace> availableMessagePlaces = new ArrayList<>(3);
                if (!MinecraftVersion.WILD_UPDATE.atOrAbove()) {
                    availableMessagePlaces.add(MessagePlace.GAME_CHAT);
                }
                availableMessagePlaces.add(MessagePlace.SYSTEM_CHAT);
                availableMessagePlaces.add(MessagePlace.ACTION_BAR);
                for (MessagePlace availableMessagePlace : availableMessagePlaces) {
                    MessageUtils.sendMessage(player, " &8- &e" + availableMessagePlace.name() + " &7(&e" + availableMessagePlace.getFriendlyName() + "&7)");
                }
                return;
            }
            if (place == MessagePlace.GAME_CHAT && MinecraftVersion.WILD_UPDATE.atOrAbove()) {
                place = MessagePlace.SYSTEM_CHAT;
            }
            editData.setCurrentMode(MessageEditData.Mode.NONE);
            editData.setNewMessagePlace(place);
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.plugin.getMenuManager().openMenu(player, editData, true));
        }
    }
}
