package dev.jaqobb.message_editor.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.cryptomorin.xseries.XSound;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import dev.jaqobb.message_editor.message.MessagePlace;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtils {
    
    public static final String MESSAGE_PREFIX = "&8[&6Message Editor&8] ";
    public static final int MESSAGE_LENGTH = 40;
    
    public static final Pattern CHAT_COLOR_PATTERN = Pattern.compile("(?i)" + ChatColor.COLOR_CHAR + "([0-9A-FK-ORX])");
    
    public static final String SPECIAL_REGEX_CHARACTERS = "[/<>{}()\\[\\],.+\\-*?^$\\\\|]";
    
    private static final Random ID_NUMBER_GENERATOR = new SecureRandom();
    private static final char[] ID_CHARACTERS = "_-0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".toCharArray();
    private static final int ID_LENGTH = 8;
    private static final int ID_MASK = (2 << (int) Math.floor(StrictMath.log(ID_CHARACTERS.length - 1) / StrictMath.log(2))) - 1;
    private static final int ID_STEP = (int) Math.ceil(1.6D * ID_MASK * ID_LENGTH / ID_CHARACTERS.length);
    
    public static final boolean HEX_COLORS_SUPPORTED;
    public static final boolean ADVENTURE_PRESENT;
    
    static {
        boolean hexColorsSupported;
        try {
            ChatColor.class.getDeclaredMethod("of", String.class);
            hexColorsSupported = true;
        } catch (NoSuchMethodException exception) {
            hexColorsSupported = false;
        }
        HEX_COLORS_SUPPORTED = hexColorsSupported;
        boolean adventurePresent;
        try {
            Class.forName("net.kyori.adventure.text.Component");
            adventurePresent = true;
        } catch (ClassNotFoundException exception) {
            adventurePresent = false;
        }
        ADVENTURE_PRESENT = adventurePresent;
    }
    
    private MessageUtils() {
        throw new UnsupportedOperationException("Cannot create instance of this class");
    }
    
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String translateWithPrefix(String message) {
        return translate(MESSAGE_PREFIX + message);
    }
    
    public static void sendSuccessSound(Player recipient) {
        recipient.playSound(recipient.getLocation(), XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound(), 1.0F, 1.0F);
    }
    
    public static void sendErrorSound(Player recipient) {
        recipient.playSound(recipient.getLocation(), XSound.ENTITY_ITEM_BREAK.parseSound(), 1.0F, 1.0F);
    }
    
    public static void sendIllegalOptionSound(Player recipient) {
        recipient.playSound(recipient.getLocation(), XSound.BLOCK_ANVIL_HIT.parseSound(), 1.0F, 1.0F);
    }
    
    public static void sendMessage(CommandSender recipient, String message) {
        recipient.sendMessage(translate(message));
    }
    
    public static void sendPrefixedMessage(CommandSender recipient, String message) {
        recipient.sendMessage(translateWithPrefix(message));
    }
    
    public static List<String> splitMessage(String message, boolean json) {
        List<String> result = new ArrayList<>();
        for (String messageData : message.split(json ? "\\n" : "\\\\n")) {
            String[] messageDataChunk = messageData.split(" ");
            String messageChunk = "";
            for (int index = 0; index < messageDataChunk.length; index += 1) {
                if (index > 0 && index < messageDataChunk.length && !messageChunk.isEmpty()) {
                    messageChunk += " ";
                }
                messageChunk += messageDataChunk[index];
                if (index == messageDataChunk.length - 1 || messageChunk.length() >= MESSAGE_LENGTH) {
                    if (result.isEmpty()) {
                        result.add(messageChunk);
                    } else {
                        result.add(getLastColors(result.get(result.size() - 1)) + messageChunk);
                    }
                    messageChunk = "";
                }
            }
        }
        return result;
    }
    
    // https://github.com/aventrix/jnanoid/blob/develop/src/main/java/com/aventrix/jnanoid/jnanoid/NanoIdUtils.java
    public static String generateId(MessagePlace place) {
        StringBuilder idBuilder = new StringBuilder(place.getId());
        while (true) {
            byte[] bytes = new byte[ID_STEP];
            ID_NUMBER_GENERATOR.nextBytes(bytes);
            for (int index = 0; index < ID_STEP; index++) {
                int characterIndex = bytes[index] & ID_MASK;
                if (characterIndex < ID_CHARACTERS.length) {
                    idBuilder.append(ID_CHARACTERS[characterIndex]);
                    if (idBuilder.length() == ID_LENGTH) {
                        return idBuilder.toString();
                    }
                }
            }
        }
    }
    
    public static String getLastColors(String message) {
        int length = message.length();
        StringBuilder colors = new StringBuilder();
        for (int index = length - 1; index > -1; index -= 1) {
            char section = message.charAt(index);
            if (section == ChatColor.COLOR_CHAR && index < length - 1) {
                char character = message.charAt(index + 1);
                if (index - 12 >= 0) {
                    char hexColorSection = message.charAt(index - 12);
                    if (hexColorSection == ChatColor.COLOR_CHAR) {
                        char hexColorCharacter = message.charAt(index - 11);
                        if ((hexColorCharacter == 'x' || hexColorCharacter == 'X') && HEX_COLORS_SUPPORTED) {
                            StringBuilder hexColor = new StringBuilder();
                            for (int j = -9; j <= 1; j += 2) {
                                hexColor.append(message.charAt(index + j));
                            }
                            try {
                                index -= 13;
                                colors.insert(0, ChatColor.of("#" + hexColor));
                                continue;
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
                ChatColor color = ChatColor.getByChar(character);
                if (color != null) {
                    index -= 1;
                    colors.insert(0, color);
                    if (color == ChatColor.RESET || (color != ChatColor.MAGIC && color != ChatColor.BOLD && color != ChatColor.STRIKETHROUGH && color != ChatColor.UNDERLINE && color != ChatColor.ITALIC)) {
                        break;
                    }
                }
            }
        }
        return colors.toString();
    }
    
    public static BaseComponent[] toBaseComponents(String message) {
        TextComponent finalComponent = new TextComponent();
        TextComponent component = new TextComponent();
        ChatColor componentNewColor = null;
        boolean firstColor = true;
        for (int index = 0; index < message.length(); index += 1) {
            boolean makeComponent = false;
            char character = message.charAt(index);
            if (index == message.length() - 1) {
                makeComponent = true;
                component.setText(component.getText() + character);
            } else if (character != '§') {
                component.setText(component.getText() + character);
            } else {
                char hexColorCharacter = message.charAt(index + 1);
                if ((hexColorCharacter == 'x' || hexColorCharacter == 'X') && HEX_COLORS_SUPPORTED) {
                    StringBuilder hexColor = new StringBuilder();
                    for (int j = 3; j <= 13; j += 2) {
                        hexColor.append(message.charAt(index + j));
                    }
                    try {
                        index += 13;
                        component.setColor(ChatColor.of("#" + hexColor));
                        firstColor = false;
                        continue;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                ChatColor color = ChatColor.getByChar(message.charAt(index + 1));
                if (color != null) {
                    index += 1;
                    if ((color != component.getColor() && firstColor) || color == ChatColor.MAGIC || color == ChatColor.BOLD || color == ChatColor.STRIKETHROUGH || color == ChatColor.UNDERLINE || color == ChatColor.ITALIC) {
                        if (color == ChatColor.MAGIC) {
                            component.setObfuscated(true);
                        } else if (color == ChatColor.BOLD) {
                            component.setBold(true);
                        } else if (color == ChatColor.STRIKETHROUGH) {
                            component.setStrikethrough(true);
                        } else if (color == ChatColor.UNDERLINE) {
                            component.setUnderlined(true);
                        } else if (color == ChatColor.ITALIC) {
                            component.setItalic(true);
                        } else {
                            component.setColor(color);
                            firstColor = false;
                        }
                    } else {
                        makeComponent = true;
                        componentNewColor = color;
                    }
                } else {
                    component.setText(component.getText() + character);
                }
            }
            if (makeComponent) {
                finalComponent.addExtra(component);
                component = new TextComponent();
                if (componentNewColor != null) {
                    component.setColor(componentNewColor);
                    componentNewColor = null;
                    firstColor = false;
                } else {
                    firstColor = true;
                }
            }
        }
        return new BaseComponent[] {finalComponent};
    }
    
    // Using ComponentSerializer#toString when the amount of components is greater than 1 wraps the message into TextComponent
    // and can thus break plugins where the index of a message component is important.
    public static String toJson(BaseComponent[] components, boolean wrapIntoTextComponent) {
        if (components.length == 1) {
            return ComponentSerializer.toString(components[0]);
        }
        if (!wrapIntoTextComponent) {
            return ComponentSerializer.toString(components);
        }
        StringJoiner json = new StringJoiner(",", "[", "]");
        for (BaseComponent component : components) {
            json.add(ComponentSerializer.toString(component));
        }
        return json.toString();
    }
    
    public static boolean isJson(String message) {
        try {
            // Streams is being used instead of JsonParser as JsonParser parses the string in lenient mode which we do not want.
            Streams.parse(new JsonReader(new StringReader(message)));
            return true;
        } catch (JsonParseException exception) {
            return false;
        }
    }
    
    public static void logMessage(Logger logger, MessagePlace place, Player player, String messageId, boolean json, String message) {
        logger.log(Level.INFO, "Place: " + place.getFriendlyName() + " (" + place.name() + ")");
        logger.log(Level.INFO, "Player: " + player.getName());
        if (json) {
            String messageReplaced = message.replaceAll(SPECIAL_REGEX_CHARACTERS, "\\\\$0");
            String messageClear = BaseComponent.toLegacyText(ComponentSerializer.parse(message));
            logger.log(Level.INFO, "Message JSON: '" + messageReplaced + "'");
            logger.log(Level.INFO, "Message clear: '" + messageClear + "'");
        } else {
            Matcher matcher = CHAT_COLOR_PATTERN.matcher(message);
            String messageSuffix = matcher.find() ? " (replace & -> § (section sign) in colors)" : "";
            logger.log(Level.INFO, "Message: '" + matcher.replaceAll("&$1").replace("\\", "\\\\") + "'" + messageSuffix);
            logger.log(Level.INFO, "Message clear: '" + matcher.replaceAll("") + "'");
        }
        logger.log(Level.INFO, "Message ID: '" + messageId + "'");
    }
    
    public static String retrieveMessage(PacketContainer packet, PacketType simulatedPacketType) {
        if (simulatedPacketType != PacketType.Play.Server.CHAT && simulatedPacketType != PacketType.Play.Server.SYSTEM_CHAT) {
            return null;
        }
        if (simulatedPacketType == PacketType.Play.Server.CHAT) {
            WrappedChatComponent message = packet.getChatComponents().readSafely(0);
            if (message != null) {
                return message.getJson();
            }
            BaseComponent[] messageComponents = packet.getSpecificModifier(BaseComponent[].class).readSafely(0);
            if (messageComponents != null) {
                return toJson(messageComponents, false);
            }
            return null;
        }
        if (ADVENTURE_PRESENT) {
            // Adventure may be present, but it is not guaranteed that packets use it.
            Component component = packet.getSpecificModifier(Component.class).readSafely(0);
            if (component != null) {
                return GsonComponentSerializer.gson().serialize(component);
            }
        }
        if (MinecraftVersion.v1_20_4.atOrAbove()) {
            return packet.getChatComponents().readSafely(0).getJson();
        }
        return packet.getStrings().readSafely(0);
    }
    
    public static void updateMessage(PacketContainer packet, PacketType simulatedPacketType, String message, boolean json) {
        if (simulatedPacketType == PacketType.Play.Server.CHAT) {
            if (packet.getChatComponents().readSafely(0) != null) {
                if (json) {
                    packet.getChatComponents().write(0, WrappedChatComponent.fromJson(message));
                } else {
                    packet.getChatComponents().write(0, WrappedChatComponent.fromJson(toJson(toBaseComponents(message), true)));
                }
            } else if (packet.getSpecificModifier(BaseComponent[].class).size() == 1) {
                if (json) {
                    packet.getSpecificModifier(BaseComponent[].class).write(0, ComponentSerializer.parse(message));
                } else {
                    packet.getSpecificModifier(BaseComponent[].class).write(0, toBaseComponents(message));
                }
            }
        } else if (simulatedPacketType == PacketType.Play.Server.SYSTEM_CHAT) {
            // Adventure may be present, but it is not guaranteed that packets use it.
            if (ADVENTURE_PRESENT && packet.getSpecificModifier(Component.class).readSafely(0) != null) {
                packet.getSpecificModifier(Component.class).write(0, GsonComponentSerializer.gson().deserialize(message));
                return;
            }
            if (json) {
                if (MinecraftVersion.v1_20_4.atOrAbove()) {
                    packet.getChatComponents().write(0, WrappedChatComponent.fromJson(message));
                } else {
                    packet.getStrings().write(0, message);
                }
            } else {
                if (MinecraftVersion.v1_20_4.atOrAbove()) {
                    packet.getChatComponents().write(0, WrappedChatComponent.fromJson(toJson(toBaseComponents(message), true)));
                } else {
                    packet.getStrings().write(0, toJson(toBaseComponents(message), true));
                }
            }
        }
    }
}
