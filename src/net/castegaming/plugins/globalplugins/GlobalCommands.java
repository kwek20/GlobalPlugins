package net.castegaming.plugins.globalplugins;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class GlobalCommands {
	
	private GlobalPlugins plugin;
	
	public GlobalCommands(GlobalPlugins plugin) {
		this.plugin = plugin;
	}
	
	public boolean command(CommandSender sender, Command cmd, String[] args){
		if (args.length > 0){
			if ((args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) && sender.hasPermission("globalplugins.reload")){
				plugin.reloadConfig();
				plugin.config.refreshConfig();
				sender.sendMessage(plugin.getPrefix() + "GlobalCommands config reloaded!");
			} else if (args[0].equalsIgnoreCase("scan") && sender.hasPermission("globalplugins.scan")){
				int found = plugin.getGlobalConfig().scan();
				if (found > 0){
					sender.sendMessage(plugin.getPrefix() + "Rescan complete. Found " + found + " new plugins, and added those to the config.");
				} else {
					sender.sendMessage(plugin.getPrefix() + "No new plugins found :/");
				}
			} else {
				sendInfoMessage(sender);
			}
		} else {
			sendInfoMessage(sender);
		}
		return true;
	}
	
    private void sendInfoMessage(CommandSender sender){
    	sender.sendMessage((new StringBuilder()).append(ChatColor.WHITE).append("---------------").append(ChatColor.GRAY).append(plugin.getPrefix()).append(ChatColor.WHITE).append("---------------").toString());
    	sender.sendMessage(plugin.getDescription().getDescription());
    	sender.sendMessage((new StringBuilder("Created for: ")).append(plugin.getDescription().getWebsite()).toString());
    	sender.sendMessage((new StringBuilder("Created by: ")).append((String)plugin.getDescription().getAuthors().get(0)).toString());
    	sender.sendMessage((new StringBuilder("Version: ")).append(plugin.getDescription().getVersion()).toString());
    	sender.sendMessage("------------------------------------------");
    }
}
