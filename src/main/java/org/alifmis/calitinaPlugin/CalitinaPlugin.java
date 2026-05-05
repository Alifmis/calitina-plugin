package org.alifmis.calitinaPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class CalitinaPlugin extends JavaPlugin {

    private ReinforcedPickaxeItem pickaxeItem;
    private ReinforcedDeepslateFragmentItem fragmentItem;
    private ReinforcedIngotItem ingotItem;

    @Override
    public void onEnable() {
        // Plugin startup logic
        pickaxeItem = new ReinforcedPickaxeItem(this);
        fragmentItem = new ReinforcedDeepslateFragmentItem(this);
        ingotItem = new ReinforcedIngotItem(this);
        getServer().getPluginManager().registerEvents(
                new ReinforcedPickaxeListener(this, pickaxeItem, fragmentItem, ingotItem),
                this
        );
        getCommand("reinforcedpickaxe").setExecutor(new ReinforcedPickaxeCommand(pickaxeItem));
        getLogger().info("CalitinaPlugin enabled. Reinforced pickaxe ready.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("CalitinaPlugin disabled.");
    }
}
