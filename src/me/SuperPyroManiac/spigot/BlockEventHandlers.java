package me.SuperPyroManiac.spigot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;


public class BlockEventHandlers
  implements Listener
{
@SuppressWarnings({ "unchecked", "rawtypes" })
public HashMap<Player, Set<Location>> blockplaced = new HashMap();
  private final GriefNotice plugin;
  
  public BlockEventHandlers(GriefNotice plugin)
  {
    this.plugin = plugin;
  }

@EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
  public void BlockBreakEvent(BlockBreakEvent event)
  {
    Player player = event.getPlayer();
    String playername = event.getPlayer().getName();
    if (player.hasPermission("GriefNotice.ignore")) {
      return;
    }
    Boolean blocklogging = (Boolean)this.plugin.newPlayerCheck.blocklog.get(player);
    Set<Location> locs = (Set<Location>)this.blockplaced.get(player);
    if (locs == null)
    {
      locs = new HashSet<Location>();
      this.blockplaced.put(player, locs);
    }
    if ((blocklogging == null) || (!blocklogging.booleanValue())) {
      return;
    }
    if (((Set<?>)this.blockplaced.get(player)).contains(event.getBlock().getLocation())) {
      return;
    }
    int blockID = event.getBlock().getType().getId();
    int maxblocks = this.plugin.getConfig().getInt("Blockbreaks");
    int countblocks = ((Integer)this.plugin.newPlayerCheck.countblock.get(player)).intValue();
    int plusblocks = countblocks + 1;
    this.plugin.newPlayerCheck.countblock.put(player, Integer.valueOf(plusblocks));
    Statement stmt;
    if ((maxblocks > countblocks) && 
      (this.plugin.getConfig().getIntegerList("Checkblocks").contains(Integer.valueOf(blockID))))
    {

    	for (Player play : Bukkit.getServer().getOnlinePlayers()) {
        if (play.hasPermission("GriefNotice.announce")) {
          play.sendMessage(GriefNotice.preFix + ChatColor.RED + playername + ChatColor.DARK_GRAY + " broke " + ChatColor.GOLD + event.getBlock().getType() + ChatColor.DARK_GRAY + "(" + ChatColor.GOLD + event.getBlock().getType().getId() + ChatColor.DARK_GRAY + ") at X:" + ChatColor.WHITE + event.getBlock().getX() + ChatColor.AQUA + " Y:" + ChatColor.WHITE + event.getBlock().getY() + ChatColor.AQUA + " Z:" + ChatColor.WHITE + event.getBlock().getZ() + ChatColor.AQUA + " In world:" + ChatColor.WHITE + event.getBlock().getWorld().getName());
        }
      }
      Connection c = null;
      stmt = null;
      try
      {
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:" + this.plugin.getDataFolder() + "//data.db");
        c.setAutoCommit(false);
        stmt = c.createStatement();
        long unixTime = System.currentTimeMillis() / 1000L;
        String sql = "INSERT INTO DATA (playername, blockname, blockid, zvalue, xvalue, yvalue, world, location, time) VALUES ('" + 
          playername + "', '" + event.getBlock().getType() + "', " + event.getBlock().getType().getId() + ", " + event.getBlock().getZ() + ", " + event.getBlock().getX() + ", " + event.getBlock().getY() + ", '" + event.getBlock().getWorld().getName() + "', '" + event.getBlock().getLocation() + "', '" + unixTime + "');";
        stmt.executeUpdate(sql);
        stmt.close();
        c.commit();
        c.close();
      }
      catch (Exception e)
      {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        System.exit(0);
      }
    }
    if (maxblocks < plusblocks)
    {
    	for (Player play : Bukkit.getServer().getOnlinePlayers()) {
        if (play.hasPermission("GriefNotice.announce"))
        {
          this.plugin.newPlayerCheck.blocklog.put(player, Boolean.valueOf(false));
          play.sendMessage(GriefNotice.preFix + ChatColor.RED + playername + ChatColor.DARK_GRAY + " Exeeded " + ChatColor.GOLD + countblocks + ChatColor.DARK_GRAY + " block breaks. Monitoring on player stopped");
        }
      }
    }
  }
  
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event)
  {
    Player player = event.getPlayer();
    Set<Location> locs = (Set<Location>)this.blockplaced.get(player);
    if (locs == null)
    {
      locs = new HashSet<Location>();
      this.blockplaced.put(player, locs);
    }
    locs.add(event.getBlock().getLocation());
  }
}
