package net.castegaming.plugins.globalplugins;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;

public class GlobalPluginLoader implements PluginLoader {

	Server server;
	
	public GlobalPluginLoader(Server server) {
		this.server = server;
	}
	
	@Override
	public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(
			Listener arg0, Plugin arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disablePlugin(Plugin arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enablePlugin(Plugin arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PluginDescriptionFile getPluginDescription(File arg0)
			throws InvalidDescriptionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pattern[] getPluginFileFilters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Plugin loadPlugin(File arg0) throws InvalidPluginException,
			UnknownDependencyException {
		// TODO Auto-generated method stub
		return null;
	}

}
