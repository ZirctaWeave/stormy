package dev.stormy.client;

import dev.stormy.client.utils.player.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.weavemc.loader.api.event.EventBus;
import net.weavemc.loader.api.event.KeyboardEvent;
import net.weavemc.loader.api.event.SubscribeEvent;
import dev.stormy.client.clickgui.ClickGui;
import dev.stormy.client.config.ClientConfig;
import dev.stormy.client.config.ConfigManager;
import dev.stormy.client.module.api.Module;
import dev.stormy.client.module.api.ModuleManager;
import dev.stormy.client.utils.game.MouseManager;

// TODO: Fix naming convention
public class Stormy {
   public static ConfigManager configManager;
   public static ClientConfig clientConfig;
   public static ModuleManager moduleManager;
   public static ClickGui clickGui;

   public static void init() {
      EventBus.subscribe(new Stormy());
      EventBus.subscribe(new MouseManager());

      moduleManager = new ModuleManager();
      clickGui = new ClickGui();
      configManager = new ConfigManager();
      clientConfig = new ClientConfig();
      clientConfig.applyConfig();
   }

   @SubscribeEvent
   public void onKeyboard(KeyboardEvent ev) {
      if (PlayerUtils.isPlayerInGame()) {
         for (Module module : moduleManager.getModules()) {
            if (Minecraft.getMinecraft().currentScreen == null) {
               module.keybind();
            } else if (Minecraft.getMinecraft().currentScreen instanceof ClickGui) {
               module.guiUpdate();
            }
         }
      }
   }
}