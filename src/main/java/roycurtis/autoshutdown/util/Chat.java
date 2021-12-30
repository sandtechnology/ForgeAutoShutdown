package roycurtis.autoshutdown.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

/**
 * Static utility class for chat functions (syntactic sugar)
 */
public class Chat
{
    /**
     * Attempts to a translate a given string/key using the local language, and then
     * using the fallback language.
     * @param msg String or language key to translate
     * @return Translated or same string
     */
    public static String translate(String msg)
    {
        return I18n.canTranslate(msg)
            ? I18n.translateToLocal(msg)
            : I18n.translateToFallback(msg);
    }

    /**
     * Broadcasts an auto. translated, formatted encapsulated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void toAll(MinecraftServer server, String msg, Object... parts)
    {
        server.getServer().getPlayerList().sendMessage( prepareText(msg, parts) );
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a player
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void to(ICommandSender sender, String msg, Object... parts)
    {
        sender.sendMessage( prepareText(msg, parts) );
    }

    private static ITextComponent prepareText(String msg, Object... parts)
    {
        String translated = translate(msg);
        String formatted  = String.format(translated, parts);

        return new TextComponentString(formatted);
    }
}
