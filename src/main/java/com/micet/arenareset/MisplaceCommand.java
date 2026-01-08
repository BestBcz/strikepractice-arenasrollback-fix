package com.micet.arenareset;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MisplaceCommand implements CommandExecutor {

    private final ArenaResetPlugin plugin;

    public MisplaceCommand(ArenaResetPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenareset.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }

        MisplaceManager manager = plugin.getMisplaceManager();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "=== Misplace 控制台 ===");
            sender.sendMessage(ChatColor.GOLD + "/misplace on " + ChatColor.WHITE + "- 全局开启 Misplace");
            sender.sendMessage(ChatColor.GOLD + "/misplace off " + ChatColor.WHITE + "- 全局关闭 Misplace");
            sender.sendMessage(ChatColor.GRAY + "当前状态: " + (manager.isEnabled ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            manager.isEnabled = false;
            sender.sendMessage(ChatColor.YELLOW + "[Misplace] 功能已全局关闭。");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            manager.isEnabled = true;
            sender.sendMessage(ChatColor.GREEN + "[Misplace] 已全局开启！");
            sender.sendMessage(ChatColor.GRAY + "说明: 延迟数值现在根据玩家所在的 Kit 自动调整 (Nodebuff=1t, Boxing=2t 等)。");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "用法错误。请使用 /misplace on 或 /misplace off");
        return true;
    }
}