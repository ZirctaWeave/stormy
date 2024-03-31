package dev.stormy.client.module.impl.combat;

import dev.stormy.client.clickgui.Theme;
import dev.stormy.client.events.UpdateEvent;
import dev.stormy.client.module.Module;
import dev.stormy.client.module.setting.impl.DescriptionSetting;
import dev.stormy.client.module.setting.impl.SliderSetting;
import dev.stormy.client.module.setting.impl.TickSetting;
import dev.stormy.client.utils.math.MathUtils;
import dev.stormy.client.utils.math.TimerUtils;
import dev.stormy.client.utils.player.PlayerUtils;
import dev.stormy.client.utils.render.Render3DUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.weavemc.loader.api.event.RenderHandEvent;
import net.weavemc.loader.api.event.RenderWorldEvent;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Optional;

@SuppressWarnings("unused")
public class Killaura extends Module {
    public static SliderSetting range, frequency, hurtTimeAmt, rotRand;
    public static TickSetting shouldBlock, targetESP, testSetting, alwaysAB, rots, whenLooking;
    static Optional<EntityPlayer> target = Optional.empty();
    public TimerUtils timer = new TimerUtils();
    public boolean delaying, isAttacking = false;
    long lastClickTime = 0;
    int rmb = mc.gameSettings.keyBindUseItem.getKeyCode();

    public Killaura() {
        super("Killaura", ModuleCategory.Combat, 0);
        this.registerSetting(new DescriptionSetting("Probably doesn't bypass much."));
        this.registerSetting(range = new SliderSetting("Range", 3, 3, 6, 0.1));
        this.registerSetting(frequency = new SliderSetting("CPS", 10, 1, 20, 0.5));
        this.registerSetting(hurtTimeAmt = new SliderSetting("Ignore before hurt time", 1, 1, 20, 1));
        this.registerSetting(rotRand = new SliderSetting("Rotation Randomization", 2, 0, 3, .01));
        this.registerSetting(rots = new TickSetting("Rotations (for bypassing)", false));
        this.registerSetting(whenLooking = new TickSetting("Only when looking at player", false));
        this.registerSetting(shouldBlock = new TickSetting("Autoblock (Hold RMB)", false));
        this.registerSetting(alwaysAB = new TickSetting("Autoblock", false));
        this.registerSetting(targetESP = new TickSetting("ESP", false));
    }

    @Override
    public void onDisable() {
        target = Optional.empty();
    }

    @SubscribeEvent
    public void onTickPre(TickEvent.Pre ev) {
        if (PlayerUtils.isPlayerInGame()) {
            target = mc.theWorld != null
                    ? mc.theWorld.playerEntities.stream()
                    .filter(player -> player.getEntityId() != mc.thePlayer.getEntityId() &&
                            player.getDistanceToEntity(mc.thePlayer) <= range.getInput())
                    .findFirst() : Optional.empty();
        }
    }

    @SubscribeEvent
    public void onUpdatePre(UpdateEvent.Pre ev) {
        if (target.isEmpty() || !PlayerUtils.isPlayerInGame()) {
            isAttacking = false;
            return;
        }
        if (timer.hasReached(1000 / frequency.getInput() + MathUtils.randomInt(-3, 3)) && mc.thePlayer.hurtTime < hurtTimeAmt.getInput() && mc.currentScreen == null) {
            if (target.isPresent()) {
                if (mc.thePlayer.isBlocking() || mc.thePlayer.isEating()) return;
                if (whenLooking.isToggled() && !lookingAt()) return;
                if (target.get().deathTime > 0) return;
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target.get());
                timer.reset();
                isAttacking = true;
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderHandEvent ev) {
        if (((Mouse.isButtonDown(1) && shouldBlock.isToggled()) || alwaysAB.isToggled()) && PlayerUtils.isPlayerHoldingWeapon() && isAttacking && mc.currentScreen == null) {
            long currentTime = System.currentTimeMillis();
            int delay = 1000 / (int) frequency.getInput() + MathUtils.randomInt(-3, 3) - 4;
            if (currentTime - lastClickTime >= delay && !delaying) {
                lastClickTime = currentTime;
                KeyBinding.setKeyBindState(rmb, true);
                KeyBinding.onTick(rmb);
                delaying = true;
            }
            if (delaying) {
                finishDelay();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent ev) {
        if (targetESP.isToggled() && target.isPresent()) {
            Render3DUtils.drawEntity(target.get(), 1, Theme.getMainColor().getRGB(), true);
            Render3DUtils.drawEntity(target.get(), 2, Theme.getMainColor().getRGB(), true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent ev) {
        if (!PlayerUtils.isPlayerInGame()) return;
        if (mc.thePlayer.isBlocking() && PlayerUtils.isPlayerHoldingWeapon() && !Mouse.isButtonDown(1) && mc.currentScreen == null && !isAttacking) {
            long neow = System.currentTimeMillis();
            int ubdelay = MathUtils.randomInt(850, 1050);
            if (neow >= ubdelay) {
                KeyBinding.setKeyBindState(rmb, false);
                KeyBinding.onTick(rmb);
            }
        }
    }

    @SubscribeEvent
    public void onUpdate(UpdateEvent ev) {
        if (PlayerUtils.isPlayerInGame() && target.isPresent() && mc.currentScreen == null && rots.isToggled() && !mc.thePlayer.isEating()) {
            double deltaX = target.get().posX - mc.thePlayer.posX;
            double deltaY = target.get().posY + target.get().getEyeHeight() - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
            double deltaZ = target.get().posZ - mc.thePlayer.posZ;
            double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

            float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180 / Math.PI)) - 90.0F + (float) MathUtils.randomInt(-rotRand.getInput(), rotRand.getInput());
            float pitch = (float) (-(Math.atan2(deltaY, distance) * (180 / Math.PI))) + (float) MathUtils.randomInt(-rotRand.getInput(), rotRand.getInput());

            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
        }
    }

    public boolean lookingAt() {
        if (!whenLooking.isToggled()) return false;
        MovingObjectPosition result = mc.objectMouseOver;
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit instanceof EntityPlayer targetPlayer) {
            return whenLooking.isToggled() && PlayerUtils.lookingAtPlayer(mc.thePlayer, targetPlayer, range.getInput() + 1);
        } else return false;
    }

    public void finishDelay() {
        long currentTime = System.currentTimeMillis();
        int newdelay = MathUtils.randomInt(20, 70);
        if (currentTime - lastClickTime >= newdelay) {
            lastClickTime = currentTime;
            KeyBinding.setKeyBindState(rmb, false);
            KeyBinding.onTick(rmb);
            delaying = false;
        }
    }
}