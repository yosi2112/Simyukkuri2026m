package org.simyukkuri.event.impl;

import org.simyukkuri.draw.Translate;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.Happiness;
import org.simyukkuri.enums.YukkuriType;
import org.simyukkuri.event.EventPacket;
import org.simyukkuri.logic.PlayerPlayLogic;
import org.simyukkuri.util.GameText;

/** A short game of chasing the player's pointer, using normal movement and collisions. */
public class RunningWithPlayerEvent extends EventPacket {

    private static final long serialVersionUID = 1L;
    private static final int MAX_TICKS = 600;
    private static final int REWARD_INTERVAL = 30;
    private static final int MIN_RUN_DISTANCE = 20;

    private int elapsed;
    private int lastRewardTick;
    private int lastX;
    private int lastY;
    private int distanceRun;
    private int targetX;
    private int targetY;
    private boolean hasTarget;
    private boolean ended;

    /**
     * Required by the existing save format. Loaded sessions end without a live player controller.
     */
    public RunningWithPlayerEvent() {
        priority = EventPriority.LOW;
    }

    /** 選択した相手とのかけっこイベントを作成します。 */
    public RunningWithPlayerEvent(Yukkuri body) {
        super(body, body, null, -1);
        priority = EventPriority.LOW;
    }

    @Override
    public boolean checkEventResponse(Yukkuri body) {
        return body != null
                && body.getUniqueId() == getTo()
                && PlayerPlayLogic.canRun(body)
                && PlayerPlayLogic.isPlayingWith(body)
                && body.getCurrentEvent() == this;
    }

    @Override
    public void start(Yukkuri body) {
        lastX = body.getX();
        lastY = body.getY();
        body.setHappiness(Happiness.HAPPY);
        say(body, "start");
        body.moveToEvent(this, lastX, lastY, body.getZ());
        body.stay(1);
    }

    /** マウス位置に対応する追跡目標を設定します。 */
    public void setRunningTarget(int x, int y) {
        targetX = x;
        targetY = y;
        hasTarget = true;
    }

    /** カーソルが地面を離れた場合に追跡を停止します。 */
    public void pauseTarget() {
        hasTarget = false;
    }

    /** カーソルが地面を離れた場合に追跡を停止します。 */
    public void pauseTarget(Yukkuri body) {
        pauseTarget();
        if (body != null) {
            body.moveToEvent(this, body.getX(), body.getY(), body.getZ());
            body.stay(1);
        }
    }

    @Override
    public UpdateState update(Yukkuri body) {
        if (ended || !checkEventResponse(body) || ++elapsed >= MAX_TICKS) {
            return UpdateState.ABORT;
        }
        if (hasTarget) {
            distanceRun += Math.abs(body.getX() - lastX) + Math.abs(body.getY() - lastY);
        }
        lastX = body.getX();
        lastY = body.getY();
        if (!hasTarget) {
            body.moveToEvent(this, lastX, lastY, body.getZ());
            body.stay(1);
            return null;
        }
        // Preserve altitude for flying species; no teleporting or movement speed mutation.
        body.moveToEvent(this, targetX, targetY, body.getZ());
        if (Translate.distance(body.getX(), body.getY(), targetX, targetY)
                <= body.getStepDist() + 2) {
            body.stay(1);
            return UpdateState.FORCE_EXEC;
        }
        body.setStaying(false);
        return null;
    }

    @Override
    public boolean execute(Yukkuri body) {
        if (!hasTarget || ended || !checkEventResponse(body)) {
            return false;
        }
        if (distanceRun >= MIN_RUN_DISTANCE && elapsed - lastRewardTick >= REWARD_INTERVAL) {
            body.addLovePlayer(5);
            body.addStress(-100);
            body.setHappiness(Happiness.VERY_HAPPY);
            say(body, "catch");
            distanceRun = 0;
            lastRewardTick = elapsed;
        }
        return false;
    }

    @Override
    public void end(Yukkuri body) {
        if (ended) {
            return;
        }
        ended = true;
        PlayerPlayLogic.eventEnded(this);
        if (body.getCurrentEvent() == this) {
            body.setDestX(-1);
            body.setDestY(-1);
            body.setDestZ(-1);
            body.setStaying(false);
            if (PlayerPlayLogic.canRun(body)) {
                say(body, "end");
            }
        }
    }

    private void say(Yukkuri body, String stage) {
        String species = body.getMsgType() == YukkuriType.MARISA ? "marisa_" : "";
        String message = GameText.read("play_running_" + species + stage);
        body.setMessage(message.replace("%name", body.getMyName()), true);
    }
}
