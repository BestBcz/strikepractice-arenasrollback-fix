package com.micet.arenareset;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class HungerManager implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    // 减缓系数 (0.0 = 正常速度, 0.5 = 慢一倍, 0.8 = 非常慢, 1.0 = 不掉饥饿)
    // 建议设置为 0.7 左右，这样既不会完全不饿，也能打很久
    private final double slowdownChance = 0.7;

    public HungerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // 注册监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        // 只处理玩家
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        int oldFoodLevel = player.getFoodLevel();
        int newFoodLevel = event.getFoodLevel();

        // 只有当饥饿度在 "下降" 时才干预
        // (如果是吃东西增加饥饿度，我们不阻止)
        if (newFoodLevel < oldFoodLevel) {

            // 生成一个 0.0 到 1.0 之间的随机数
            // 如果随机数小于 slowdownChance，就取消这次饥饿度扣除
            if (random.nextDouble() < slowdownChance) {
                event.setCancelled(true);
            }
        }
    }
}