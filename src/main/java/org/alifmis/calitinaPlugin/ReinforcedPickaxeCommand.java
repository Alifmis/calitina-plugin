package org.alifmis.calitinaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
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
        Player target = null;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("Player not found or not online.");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("Usage: /" + label + " <player>");
            return true;
        }

        ItemStack item = pickaxeItem.createItem();
        target.getInventory().addItem(item);
        if (target.equals(sender)) {
            target.sendMessage("You received a Reinforced Pickaxe.");
        } else {
            target.sendMessage("You received a Reinforced Pickaxe from " + sender.getName() + ".");
            sender.sendMessage("Gave a Reinforced Pickaxe to " + target.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
