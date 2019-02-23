package me.SuperPyroManiac.spigot;

import java.io.File;
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
    static String preFix = (ChatColor.GOLD + "- " + ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Grief Alert" + ChatColor.DARK_GRAY + "] ");

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
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (cmd.getName().equalsIgnoreCase("gn"))
        {
            Player player = (Player)sender;
            if (args.length == 0){
                player.sendMessage(preFix + "Unknown Command. Use: " + ChatColor.GOLD + "/gn help");
                return false;
            }
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
                            play.sendMessage(preFix + ChatColor.GOLD + namy + ChatColor.DARK_GRAY + " will be monitored again.");
                        }
                    }
                }
                else if ((args.length == 1) && (args[0].equals("status")))
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
                        player.sendMessage(ChatColor.DARK_GRAY + " ----- " + ChatColor.DARK_RED + "Grief Notice Check " + ChatColor.DARK_GRAY + "- " + ChatColor.GOLD + "Last 10 Results " + ChatColor.DARK_GRAY + "-----");
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
                            player.sendMessage(ChatColor.GOLD + "ID: " + ChatColor.GRAY + breakid + ChatColor.GOLD + " Name: " + ChatColor.RED + name + ChatColor.GOLD + " Broke: " + ChatColor.GRAY + count + ChatColor.GOLD + " last break: " + ChatColor.GRAY + fDate);
                        }
                        player.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.GOLD + "Use /gn checkplayer <playername> to view the log " + ChatColor.GOLD + " -");
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
                else if ((args.length == 2) && (args[0].equals("tp")))
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
                                player.sendMessage(preFix + ChatColor.DARK_GRAY + "Teleporting you to: " + ChatColor.GRAY + x + ChatColor.GOLD + "," + ChatColor.GRAY + y + ChatColor.GOLD + "," + ChatColor.GRAY + z + ChatColor.GOLD + " in the world: " + ChatColor.GRAY + rs.getString("world"));
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
                else if (((args.length == 2) && (args[0].equals("checkplayer"))) || ((args.length == 3) && (args[0].equals("checkplayer"))))
                {
                    Connection c = null;
                    Statement stmt = null;
                    try
                    {
                        Class.forName("org.sqlite.JDBC");
                        c = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "//data.db");
                        c.setAutoCommit(false);
                        stmt = c.createStatement();
                        player.sendMessage(ChatColor.DARK_GRAY + " ----- " + ChatColor.DARK_RED + "Grief Notice " + ChatColor.DARK_GRAY + "- " + ChatColor.GOLD + "Last 10 Results: " + ChatColor.RED + args[1] + ChatColor.DARK_GRAY + " -----");
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
                                player.sendMessage(ChatColor.GOLD + "ID: " + ChatColor.GRAY + id + ChatColor.GOLD + " Time: " + ChatColor.GRAY + fDate + ChatColor.GOLD + " Block: " + ChatColor.GRAY + blockname + ChatColor.GOLD + " World: " + ChatColor.GRAY + world + ChatColor.GOLD + " Cords(ZYX): " + ChatColor.GRAY + Zcord + ChatColor.GOLD + "," + ChatColor.GRAY + Ycord + ChatColor.GOLD + "," + ChatColor.GRAY + Xcord);
                            }
                            int count = 0;
                            ResultSet res = stmt.executeQuery("SELECT COUNT(*) FROM DATA WHERE playername = '" + args[1] + "'");
                            while (res.next()) {
                                count = res.getInt(1);
                            }
                            player.sendMessage(ChatColor.DARK_GRAY + " ----- " + ChatColor.DARK_RED + "Grief Notice " + ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + "Displaying page " + ChatColor.GRAY + "1" + ChatColor.GOLD + "/" + ChatColor.GRAY + (int)Math.ceil(count / 10.0D) + ChatColor.DARK_GRAY + " ----- ");
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
                                player.sendMessage(ChatColor.GOLD + "ID: " + ChatColor.GRAY + id + ChatColor.GOLD + " Time: " + ChatColor.GRAY + fDate + ChatColor.GOLD + " Block: " + ChatColor.GRAY + blockname + ChatColor.GOLD + " World: " + ChatColor.GRAY + world + ChatColor.GOLD + " Cords(ZYX): " + ChatColor.GRAY + Zcord + ChatColor.GOLD + "," + ChatColor.GRAY + Ycord + ChatColor.GOLD + "," + ChatColor.GRAY + Xcord);
                            }
                            int count = 0;
                            ResultSet res = stmt.executeQuery("SELECT COUNT(*) FROM DATA WHERE playername = '" + args[1] + "'");
                            while (res.next()) {
                                count = res.getInt(1);
                            }
                            player.sendMessage(ChatColor.DARK_GRAY + " ----- " + ChatColor.DARK_RED + "Grief Notice " + ChatColor.DARK_GRAY + " - " + ChatColor.GOLD + "Displaying page " + ChatColor.GRAY + offset / 10 + ChatColor.GOLD + "/" + ChatColor.GRAY + (int)Math.ceil(count / 10.0D) + ChatColor.DARK_GRAY + " ----- ");
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
                else if ((args[0].equals("help"))){
                    player.sendMessage(ChatColor.DARK_GRAY + " ----- " + ChatColor.DARK_RED + "Grief Notice " + ChatColor.DARK_GRAY + " ----- ");
                    player.sendMessage(ChatColor.GOLD + "/gn status" + ChatColor.DARK_GRAY + " Checks recent logs for possible griefs.");
                    player.sendMessage(ChatColor.GOLD + "/gn checkplayer <player> <page>" + ChatColor.DARK_GRAY + " Checks player for their possible griefing logs.");
                    player.sendMessage(ChatColor.GOLD + "/gn tp <id>" + ChatColor.DARK_GRAY + " Teleports to the ID.");
                    player.sendMessage(ChatColor.GOLD + "/gn monitor <player>" + ChatColor.DARK_GRAY + " Monitors that player again.");
                }
                else {
                    player.sendMessage(preFix + "Unknown Command. Use: " + ChatColor.GOLD + "/gn help");
                }
            }
            else {
                player.sendMessage(preFix + "You do have permission for this! (" + ChatColor.GOLD + "griefnotice.admin" + ChatColor.DARK_GRAY + ")");
            }
        }
        return true;
    }
}