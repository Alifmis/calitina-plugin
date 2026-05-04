package org.alifmis.calitinaPlugin;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ReinforcedPickaxeCommand implements CommandExecutor, TabCompleter {
    private final ReinforcedPickaxeItem pickaxeItem;

    public ReinforcedPickaxeCommand(ReinforcedPickaxeItem pickaxeItem) {
        this.pickaxeItem = pickaxeItem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        ItemStack item = pickaxeItem.createItem();
        player.getInventory().addItem(item);
        player.sendMessage("You received a Reinforced Pickaxe.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

