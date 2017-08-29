package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class ServerWatchdog extends Thread {

    public ServerWatchdog() {
        super("DiscordSRV - Server Watchdog");
    }

    private long lastTick = System.currentTimeMillis();
    private boolean hasBeenTriggered = true;

    private void tick() {
        lastTick = System.currentTimeMillis();
        hasBeenTriggered = false;
    }

    @Override
    public void run() {
        int taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), this::tick, 0, 20);
        if (taskNumber == -1) {
            DiscordSRV.debug("Failed to schedule repeating task for server watchdog; returning");
            return;
        }

        while (true) {
            try {
                int timeout = DiscordSRV.config().getInt("ServerWatchdogTimeout");
                if (timeout < 10) timeout = 10; // minimum value
                if (hasBeenTriggered || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastTick) < timeout) {
                    Thread.sleep(1000);
                } else {
                    if (!DiscordSRV.config().getBoolean("ServerWatchdogEnabled")) {
                        DiscordSRV.debug("The Server Watchdog would have triggered right now but it was disabled in the config");
                        return;
                    }

                    for (int i = 0; i < DiscordSRV.config().getInt("ServerWatchdogMessageCount"); i++) {
                        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), LangUtil.Message.SERVER_WATCHDOG.toString()
                                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                                .replace("%guildowner%", DiscordSRV.getPlugin().getMainGuild().getOwner().getAsMention())
                        );
                    }

                    return;
                }
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Server Watchdog thread: interrupted");
                return;
            }
        }
    }

}
