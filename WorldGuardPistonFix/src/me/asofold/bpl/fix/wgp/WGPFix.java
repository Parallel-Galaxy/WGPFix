package me.asofold.bpl.fix.wgp;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import me.asofold.bpl.fix.wgp.compatlayer.CompatConfig;
import me.asofold.bpl.fix.wgp.compatlayer.CompatConfigFactory;
import me.asofold.bpl.fix.wgp.compatlayer.ConfigUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * 
 * 
 * @author mc_dev
 *
 */
public class WGPFix extends JavaPlugin {
	final WGPFixCoreListener blockListener = new WGPFixCoreListener(this);
	final WGPFixServerListener serverListener = new WGPFixServerListener(this);
	final static List<WGPRegionChecker> regionCheckers = new LinkedList<WGPRegionChecker>();
	
	final static int defaultMaxBlocks = 14;
	
	@Override
	public void onDisable() {
		blockListener.monitorPistons = false;
		setPanic(false);
		blockListener.resetWG();
		System.out.println("[WGPFix] WorldGuardPistonFix "+getDescription().getVersion()+" disabled.");
	}

	@Override
	public void onEnable() {
		loadSettings();
		getCommand("wgpfix").setExecutor(new WGPFixCommand(this));
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(blockListener, this);
		pm.registerEvents(serverListener, this);
		System.out.println("[WGPFix] WorldGuardPistonFix "+getDescription().getVersion()+" enabled.");
	}
	
	/**
	 * Delegates to WorldGuard, will throw NullPointerExceptionif WorldGuard is not present.
	 * (This might change if the plugin is made independent of WorldGuard.)
	 * @param sender
	 * @param perm
	 * @return
	 */
	public boolean hasPermission(CommandSender sender, String perm) {
		return blockListener.getWorldGuard().hasPermission(sender, perm);
	}
	
	/**
	 * (API)
	 * If set to false pistons will not be monitored at all !
	 * @param monitor
	 */
	public void setMonitorPistons( boolean monitor){
		this.blockListener.monitorPistons = monitor;
	}
	
	/**
	 * (API)
	 * If set to true, non sticky pistons might be prevented from retracting at some performance loss (imagine item lifting devices).
	 * (For the paranoid))
	 * @param prevent
	 */
	public void setPreventNonStickyRetract( boolean prevent){
		this.blockListener.preventNonStickyRetract = prevent;
	}
	
	/**
	 * (API)
	 * Set the interval in ms, at which the WorldGuard instance is set.
	 * @param ms
	 */
	public void setWorldGuardSetInterval(long ms){
		this.blockListener.tsThreshold = ms;
	}
	
	/**
	 * (API)
	 * @param pop
	 */
	public void setPopDisallowed( boolean pop){
		this.blockListener.popDisallowed = pop;
	}
	
	public CompatConfig getDefaultConfig(){
		CompatConfig config = CompatConfigFactory.getConfig(null);
		config.setProperty("monitor-pistons", true);
		config.setProperty("prevent-nonsticky-retract", false);
		config.setProperty("set-worldguard-interval", 4000);
		config.setProperty("pop-disallowed", false);
		config.setProperty("deny-blocks.sticky", new LinkedList<Integer>());
		config.setProperty("deny-blocks.all", new LinkedList<Integer>());
		config.setProperty("panic", false);
		config.setProperty("max-blocks", defaultMaxBlocks);
		config.setProperty("monitor-structure-growth", false);
		config.setProperty("monitor-from-to", false);
		config.save(); // ignore result
		return config;
	}
	
	/**
	 * (API)
	 * Load and apply settings from wgpfx.yml !
	 */
	public boolean loadSettings(){
		File file = new File( getDataFolder(), "wgpfix.yml");
		try{
			CompatConfig config = CompatConfigFactory.getConfig(file);
			config.load();
			if (ConfigUtil.forceDefaults(getDefaultConfig(), config)){
				config.save();
			}
			setPanic(config.getBoolean("panic", false));
			setMonitorPistons(config.getBoolean("monitor-pistons", true));
			setPreventNonStickyRetract(config.getBoolean("prevent-nonsticky-retract", false));
			setWorldGuardSetInterval(config.getInt("set-worldguard-interval", 4000));
			setPopDisallowed(config.getBoolean("pop-disallowed", false));
			setMaxBlocks(config.getInt("max-blocks", defaultMaxBlocks));
			setDeniedBlocks(config.getIntList("deny-blocks.sticky", null), config.getIntList("deny-blocks.all", null));
			setMonitorStructureGrowth(config.getBoolean("monitor-structure-growth", false));
			setMonitorFromTo(config.getBoolean("monitor-from-to", false));
			blockListener.setWG();
			return true;
		} catch (Throwable t){
			getServer().getLogger().severe("[WGPFix] Could not load configuration, set to PANIC - all piston action will be prevented ! Error: "+t.getMessage());
			t.printStackTrace();
			setParanoid();
			return false;
		}
	}
	
	
	/**
	 * API
	 * Set to true to prevent trees and huge mushrooms growing over region borders with differing owners/members.
	 * @param boolean1
	 */
	public void setMonitorStructureGrowth(boolean monitor) {
		blockListener.monitorStructureGrowth = monitor;
	}
	
	/**
	 * API
	 * Set to true to monitor fluid spreading.
	 * @param monitor
	 */
	public void setMonitorFromTo(boolean monitor){
		blockListener.monitorFromTo = monitor;
	}

	/**
	 * (API)
	 * Set which blocks pistons are not allow to affect. 
	 * @param denySticky No sort of piston can affect these. May be null.
	 * @param denyAll Sticky pistons can not affect these. May be null.
	 */
	public void setDeniedBlocks(Collection<Integer> denySticky, Collection<Integer> denyAll){
		blockListener.denySticky.clear();
		blockListener.denyAll.clear();
		if ( denySticky != null ) blockListener.denySticky.addAll(denySticky);
		if ( denyAll != null ){
			blockListener.denyAll.addAll(denyAll);
			blockListener.denySticky.addAll(denyAll);
		}
	}
	
	/**
	 * (API)
	 * Set maximum number of blocks that may be involved in pistons actions, including the piston base.
	 * @param maxBlocks
	 */
	public void setMaxBlocks(Integer maxBlocks) {
		this.blockListener.maxBlocks = maxBlocks;
	}

	/**
	 * (API)
	 * Prevent everything that can be prevented by this plugin.
	 */
	public void setParanoid(){
		setPanic(true);
		this.setMonitorPistons(true);
		this.setPreventNonStickyRetract(true);
		this.setWorldGuardSetInterval(4000);
		this.setPopDisallowed(true); // PARANOID !
		blockListener.setWG();
	}

	/**
	 * (API)
	 * WGPFix will deny all piston action if true.
	 * As opposed to setParanoid this will only deny piston action, so configuration settings stay preserved, except for panic.
	 * @param panic
	 */
	public void setPanic(boolean panic) {
		blockListener.panic = panic;
	}
	
	/**
	 * (API)
	 * Register implementation that will check regions just before allowing piston-action.
	 */
	public static void addRegionChecker( WGPRegionChecker checker){
		regionCheckers.add(checker);
	}
	
	/**
	 * (API)
	 * Unregister implementation for checking regions affected by piston actions.
	 * @param checker
	 */
	public static void removeRegionChecker( WGPRegionChecker checker){
		regionCheckers.remove(checker);
	}

}
