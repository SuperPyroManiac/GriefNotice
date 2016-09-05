package me.SuperPyroManiac.spigot;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GriefNotice
  extends JavaPlugin
{
  public BlockEventHandlers blockEventHandlers = new BlockEventHandlers(this);
  public NewPlayerCheck newPlayerCheck = new NewPlayerCheck(this);
  
  public void onEnable()
  {
    getServer().getPluginManager().registerEvents(this.blockEventHandlers, this);
    getServer().getPluginManager().registerEvents(this.newPlayerCheck, this);
    File configFile = new File(getDataFolder(), "config.yml");
    if (!configFile.exists())
    {
      getLogger().info("GriefNotice is loading for the first time - Generating Config.yml");
      getConfig().options().copyDefaults(true);
      saveConfig();
    }
    File dataFile = new File(getDataFolder(), "data.db");
    if (!dataFile.exists())
    {
      Connection c = null;
      Statement stmt = null;
      try
      {
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "//data.db");
        System.out.println("Opened database successfully");
        
        stmt = c.createStatement();
        String sql = "CREATE TABLE DATA  (ID integer PRIMARY KEY,  playername varchar(16) NOT NULL,  blockname text NOT NULL,  blockid int(16) NOT NULL,  zvalue int(10) NOT NULL,  xvalue int(10) NOT NULL,  yvalue int(10) NOT NULL,  world varchar(200) NOT NULL, location varchar(300) NOT NULL,  time int(12) NOT NULL )";
        
        stmt.executeUpdate(sql);
        stmt.close();
        c.close();
      }
      catch (Exception e)
      {
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
        System.exit(0);
      }
      System.out.println("Table created successfully");
    }
    try
    {
      MetricsLite metrics = new MetricsLite(this);
      metrics.start();
    }
    catch (IOException localIOException) {}
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
  {
    if (cmd.getName().equalsIgnoreCase("gn"))
    {
      Player player = (Player)sender;
      if (player.hasPermission("GriefNotice.admin"))
      {
        if ((args.length == 2) && (args[0].equals("monitor")))
        {
          Player plays = Bukkit.getServer().getPlayer(args[1]);
          String namy = args[1];
          this.newPlayerCheck.blocklog.put(plays, Boolean.valueOf(true));
          this.newPlayerCheck.countblock.put(plays, Integer.valueOf(0));
          for (Player play : Bukkit.getServer().getOnlinePlayers()) {
        	  
          
            if (play.hasPermission("GriefNotice.announce")) {
              play.sendMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "GriefNotice" + ChatColor.WHITE + "] " + ChatColor.RED + namy + ChatColor.AQUA + " was requested to be monitored again");
            }
          }
        }
        if ((args.length == 1) && (args[0].equals("status")))
        {
          Connection c = null;
          Statement stmt = null;
          Statement ctmt = null;
          try
          {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "//data.db");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            ctmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT data.* FROM `data` INNER JOIN ( SELECT max( ID ) AS id FROM `data` GROUP BY playername ) AS id_join ON data.id = id_join.id ORDER BY ID DESC LIMIT 10");
            player.sendMessage(ChatColor.WHITE + " ----- " + ChatColor.AQUA + "Grief Notice Check " + ChatColor.WHITE + "- " + ChatColor.AQUA + "Last 10 Results " + ChatColor.WHITE + "-----");
            while (rs.next())
            {
              String name = rs.getString("playername");
              int breakid = rs.getInt("ID");
              int timeStamp = rs.getInt("time");
              Date time = new Date(timeStamp * 1000L);
              SimpleDateFormat sdf = new SimpleDateFormat("dd/MM-yyyy HH:mm:ss");
              String fDate = sdf.format(time);
              ResultSet bs = ctmt.executeQuery("SELECT COUNT(playername) AS total FROM DATA WHERE playername = '" + name + "'");
              int count = bs.getInt("total");
              player.sendMessage(ChatColor.AQUA + "ID: " + ChatColor.WHITE + breakid + ChatColor.AQUA + " Name: " + ChatColor.RED + name + ChatColor.AQUA + " Broke: " + ChatColor.WHITE + count + ChatColor.AQUA + " last break: " + ChatColor.WHITE + fDate);
            }
            player.sendMessage(ChatColor.WHITE + "- " + ChatColor.AQUA + "Use /gn checkplayer <playername> to view the log " + ChatColor.WHITE + " -");
            rs.close();
            stmt.close();
            c.close();
          }
          catch (Exception e)
          {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
          }
        }
        if ((args.length == 2) && (args[0].equals("tp")))
        {
          Connection c = null;
          Statement stmt = null;
          try
          {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "//data.db");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            String location = args[1];
            ResultSet rs = stmt.executeQuery("SELECT * FROM DATA where ID='" + location + "'");
            while (rs.next())
            {
              double x = Double.parseDouble(rs.getString("xvalue"));
              double y = Double.parseDouble(rs.getString("yvalue"));
              double z = Double.parseDouble(rs.getString("zvalue"));
              World world = getServer().getWorld(rs.getString("world"));
              if (world != null)
              {
                Location locationToTeleport = new Location(world, x, y, z);
                player.teleport(locationToTeleport);
                player.sendMessage(ChatColor.AQUA + "Teleporting you to: " + ChatColor.WHITE + x + ChatColor.AQUA + "," + ChatColor.WHITE + y + ChatColor.AQUA + "," + ChatColor.WHITE + z + ChatColor.AQUA + " in the world: " + ChatColor.WHITE + rs.getString("world"));
              }
            }
            rs.close();
            stmt.close();
            c.close();
          }
          catch (Exception e)
          {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
          }
        }
        if (((args.length == 2) && (args[0].equals("checkplayer"))) || ((args.length == 3) && (args[0].equals("checkplayer"))))
        {
          Connection c = null;
          Statement stmt = null;
          try
          {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "//data.db");
            c.setAutoCommit(false);
            stmt = c.createStatement();
            player.sendMessage(ChatColor.WHITE + " ----- " + ChatColor.AQUA + "Grief Notice " + ChatColor.WHITE + "- " + ChatColor.AQUA + "Last 10 Results: " + ChatColor.RED + args[1] + ChatColor.WHITE + " -----");
            if ((args.length == 2) && (args[0].equals("checkplayer")))
            {
              int offset = 0;
              ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE playername = '" + args[1] + "' ORDER BY ID DESC LIMIT 10 OFFSET " + offset);
              while (rs.next())
              {
                int id = rs.getInt("id");
                int timeStamp = rs.getInt("time");
                String blockname = rs.getString("blockname");
                String Zcord = rs.getString("zvalue");
                String Ycord = rs.getString("yvalue");
                String Xcord = rs.getString("xvalue");
                String world = rs.getString("world");
                Date time = new Date(timeStamp * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM-yyyy HH:mm:ss");
                String fDate = sdf.format(time);
                player.sendMessage(ChatColor.AQUA + "ID: " + ChatColor.WHITE + id + ChatColor.AQUA + " Time: " + ChatColor.WHITE + fDate + ChatColor.AQUA + " Block: " + ChatColor.WHITE + blockname + ChatColor.AQUA + " World: " + ChatColor.WHITE + world + ChatColor.AQUA + " Cords(ZYX): " + ChatColor.WHITE + Zcord + ChatColor.AQUA + "," + ChatColor.WHITE + Ycord + ChatColor.AQUA + "," + ChatColor.WHITE + Xcord);
              }
              int count = 0;
              ResultSet res = stmt.executeQuery("SELECT COUNT(*) FROM DATA WHERE playername = '" + args[1] + "'");
              while (res.next()) {
                count = res.getInt(1);
              }
              player.sendMessage(ChatColor.WHITE + " ----- " + ChatColor.AQUA + "Grief Notice " + ChatColor.WHITE + " - " + ChatColor.AQUA + "Displaying page " + ChatColor.WHITE + "1" + ChatColor.AQUA + "/" + ChatColor.WHITE + (int)Math.ceil(count / 10.0D) + ChatColor.WHITE + " ----- ");
              rs.close();
              stmt.close();
              c.close();
            }
            if ((args.length == 3) && (args[0].equals("checkplayer")))
            {
              String offsets = args[2];
              int offset = Integer.parseInt(offsets) * 10;
              ResultSet rs = stmt.executeQuery("SELECT * FROM DATA WHERE playername = '" + args[1] + "' ORDER BY ID DESC LIMIT 10 OFFSET " + offset);
              while (rs.next())
              {
                int id = rs.getInt("id");
                int timeStamp = rs.getInt("time");
                String blockname = rs.getString("blockname");
                String Zcord = rs.getString("zvalue");
                String Ycord = rs.getString("yvalue");
                String Xcord = rs.getString("xvalue");
                String world = rs.getString("world");
                Date time = new Date(timeStamp * 1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM-yyyy HH:mm:ss");
                String fDate = sdf.format(time);
                player.sendMessage(ChatColor.AQUA + "ID: " + ChatColor.WHITE + id + ChatColor.AQUA + " Time: " + ChatColor.WHITE + fDate + ChatColor.AQUA + " Block: " + ChatColor.WHITE + blockname + ChatColor.AQUA + " World: " + ChatColor.WHITE + world + ChatColor.AQUA + " Cords(ZYX): " + ChatColor.WHITE + Zcord + ChatColor.AQUA + "," + ChatColor.WHITE + Ycord + ChatColor.AQUA + "," + ChatColor.WHITE + Xcord);
              }
              int count = 0;
              ResultSet res = stmt.executeQuery("SELECT COUNT(*) FROM DATA WHERE playername = '" + args[1] + "'");
              while (res.next()) {
                count = res.getInt(1);
              }
              player.sendMessage(ChatColor.WHITE + " ----- " + ChatColor.AQUA + "Grief Notice " + ChatColor.WHITE + " - " + ChatColor.AQUA + "Displaying page " + ChatColor.WHITE + offset / 10 + ChatColor.AQUA + "/" + ChatColor.WHITE + (int)Math.ceil(count / 10.0D) + ChatColor.WHITE + " ----- ");
              rs.close();
              stmt.close();
              c.close();
            }
          }
          catch (Exception e)
          {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
          }
        }
      }
    }
    return true;
  }
}