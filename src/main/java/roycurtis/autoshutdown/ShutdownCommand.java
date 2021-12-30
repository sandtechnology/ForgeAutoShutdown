package roycurtis.autoshutdown;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;
import roycurtis.autoshutdown.util.Chat;
import roycurtis.autoshutdown.util.Server;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Singleton that handles the `/shutdown` voting command
 */
public class ShutdownCommand implements ICommand
{
    static final List<String> ALIASES = Collections.singletonList("shutdown");
    static final List<String> OPTIONS = Arrays.asList("yes", "no");

    private static ShutdownCommand INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger          LOGGER;

    HashMap<String, Boolean> votes = new HashMap<>();

    Date    lastVote = new Date(0);
    boolean voting   = false;

    /** Creates and registers the `/shutdown` command for use */
    public static void create(FMLServerStartingEvent event)
    {
        if (INSTANCE != null)
            throw new RuntimeException("ShutdownCommand can only be created once");

        INSTANCE = new ShutdownCommand();
        SERVER   = ForgeAutoShutdown.server;
        LOGGER   = ForgeAutoShutdown.LOGGER;

        event.registerServerCommand(INSTANCE);
        LOGGER.debug("`/shutdown` command registered");
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/shutdown <yes|no>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("shutdown");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (sender == SERVER)
            throw new CommandException("FAS.error.playersonly");

        if (voting)
            processVote(sender, args);
        else
            initiateVote(args);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return Config.voteEnabled;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return OPTIONS;
    }


    private ShutdownCommand() { }

    private void initiateVote(String[] args) throws CommandException
    {
        if (args.length >= 1)
            throw new CommandException("FAS.error.novoteinprogress");

        Date now        = new Date();
        long interval   = (long) Config.voteInterval * 60 * 1000;
        long difference = now.getTime() - lastVote.getTime();

        if (difference < interval)
            throw new CommandException("FAS.error.toosoon", (interval - difference) / 1000);

        List<EntityPlayerMP> players = SERVER.getPlayerList().getPlayers();

        if (players.size() < Config.minVoters)
            throw new CommandException("FAS.error.notenoughplayers", Config.minVoters);

        Chat.toAll(SERVER, "FAS.msg.votebegun");
        voting = true;
    }

    private void processVote(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
            throw new CommandException("FAS.error.voteinprogress");
        else if ( !OPTIONS.contains( args[0].toLowerCase() ) )
            throw new CommandException("FAS.error.votebadsyntax");

        String  name = sender.getName();
        Boolean vote = args[0].equalsIgnoreCase("yes");

        if ( votes.containsKey(name) )
            Chat.to(sender, "FAS.msg.votecleared");

        votes.put(name, vote);
        Chat.to(sender, "FAS.msg.voterecorded");
        checkVotes();
    }

    private void checkVotes()
    {
        int players = SERVER.getPlayerList().getPlayers().size();

        if (players < Config.minVoters)
        {
            voteFailure("FAS.fail.notenoughplayers");
            return;
        }

        int yes = Collections.frequency(votes.values(), true);
        int no  = Collections.frequency(votes.values(), false);

        if (no >= Config.maxNoVotes)
        {
            voteFailure("FAS.fail.maxnovotes");
            return;
        }

        if (yes + no == players)
            voteSuccess();
    }

    private void voteSuccess()
    {
        LOGGER.info("Server shutdown initiated by vote");
        Server.shutdown("FAS.msg.usershutdown");
    }

    private void voteFailure(String reason)
    {
        Chat.toAll(SERVER, reason);
        votes.clear();

        lastVote = new Date();
        voting   = false;
    }


    @Override
    public boolean isUsernameIndex(String[] args, int idx)
    {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return o.getName().compareTo( getName() );
    }
    // </editor-fold>
}
