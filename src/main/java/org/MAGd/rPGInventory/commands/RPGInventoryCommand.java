package org.MAGd.rPGInventory.commands;

import org.MAGd.rPGInventory.RPGInventory;
import org.MAGd.rPGInventory.gui.InventoryGUI;
import org.MAGd.rPGInventory.listeners.TotemEffectListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class RPGInventoryCommand implements CommandExecutor {

    private final RPGInventory plugin;

    public RPGInventoryCommand(RPGInventory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                sendHelpMessage((Player) sender);
            } else {
                sendHelpMessageToConsole(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c[RPGInventory] 'open' 子命令只能由玩家在遊戲中使用。");
                    return true;
                }
                Player playerForOpen = (Player) sender;
                InventoryGUI.openInventory(playerForOpen);
                return true;

            case "debug":
                if (!sender.hasPermission("rpginventory.admin")) {
                    sender.sendMessage("§c[RPGInventory] 你沒有權限使用此命令!");
                    return true;
                }

                TotemEffectListener totemListener = plugin.getTotemEffectListener();
                if (totemListener == null) {
                    sender.sendMessage("§c[RPGInventory] 錯誤：無法獲取 TotemEffectListener 內部組件。");
                    return true;
                }

                if (sender instanceof Player) {
                    Player playerForDebug = (Player) sender;
                    boolean newState = totemListener.togglePlayerDebugMode(playerForDebug);
                    playerForDebug.sendMessage("§6[RPGInventory] §a你的個人調試訊息模式已 " + (newState ? "§e開啟" : "§c關閉") + "§a。");
                } else if (sender instanceof ConsoleCommandSender) {
                    boolean newState = totemListener.toggleConsoleDebugMode();
                    sender.sendMessage("§6[RPGInventory] §a控制台後台詳細日誌模式已 " + (newState ? "§e開啟" : "§c關閉") + "§a。");
                } else {
                    sender.sendMessage("§c[RPGInventory] 'debug' 子命令只能由玩家或控制台執行。");
                }
                return true;

            default:
                if (sender instanceof Player) {
                    sendHelpMessage((Player) sender);
                } else {
                    sendHelpMessageToConsole(sender);
                }
                return true;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§6========== RPGInventory 幫助 ==========");
        player.sendMessage("§e/rpginv open §7- 打開你的RPG物品欄。");
        if (player.hasPermission("rpginventory.admin")) {
            player.sendMessage("§e/rpginv debug §7- 切換你個人的調試訊息顯示。 (管理員)");
        }
        player.sendMessage("§6====================================");
    }

    private void sendHelpMessageToConsole(CommandSender sender) {
        sender.sendMessage("§6========== RPGInventory 幫助 (控制台) ==========");
        sender.sendMessage("§e/rpginv debug §7- 切換控制台後台詳細日誌的顯示。 (管理員)");
        sender.sendMessage("§7 (玩家使用 /rpginv open 打開物品欄)");
        sender.sendMessage("§6==============================================");
    }
} 