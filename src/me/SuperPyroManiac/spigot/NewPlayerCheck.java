package me.SuperPyroManiac.spigot;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NewPlayerCheck
  implements Listener
{
  @SuppressWarnings({ "unchecked", "rawtypes" })
public HashMap<Player, Boolean> blocklog = new HashMap();
  @SuppressWarnings({ "unchecked", "rawtypes" })
public HashMap<Player, Integer> maxblock = new HashMap();
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public HashMap<Player, Integer> countblock = new HashMap();
  private final GriefNotice plugin;
  
  public NewPlayerCheck(GriefNotice plugin)
  {
    this.plugin = plugin;
  }
  
  @EventHandler
  public void onPlayerJoinEvent(PlayerJoinEvent event)
  {
    Player p = event.getPlayer();
    String playername = event.getPlayer().getName();
    if (!p.hasPlayedBefore())
    {
    	for (Player play : Bukkit.getServer().getOnlinePlayers()) {
        if (play.hasPermission("GriefNotice.announce")) {
          play.sendMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "GriefNotice" + ChatColor.WHITE + "] " + ChatColor.RED + playername + ChatColor.AQUA + " logged in for the first time and will be monitored");
        }
      }
      this.blocklog.put(p, Boolean.valueOf(true));
      this.maxblock.put(p, Integer.valueOf(this.plugin.getConfig().getInt("Blockbreaks")));
      this.countblock.put(p, Integer.valueOf(0));
    }
  }
}
