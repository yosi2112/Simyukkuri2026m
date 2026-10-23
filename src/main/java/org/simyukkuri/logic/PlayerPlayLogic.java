package org.simyukkuri.logic;

import org.simyukkuri.SimYukkuri;
import org.simyukkuri.draw.Translate;
import org.simyukkuri.engine.World;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.FootBake;
import org.simyukkuri.event.impl.RunningWithPlayerEvent;
import org.simyukkuri.system.WorldState;

/** One player, one playmate. Call under SimYukkuri.lock, like other commands. */
public final class PlayerPlayLogic {

    private static Yukkuri playmate;
    private static RunningWithPlayerEvent running;
    private static World playWorld;
    private static WorldState playMap;

    private PlayerPlayLogic() {}

    /** Start only after validating the new participant; an invalid click keeps the current game. */
    public static boolean startRunning(Yukkuri body) {
        tick();
        if (isPlayingWith(body)) {
            return true;
        }
        if (!canRun(body) || !body.canEventResponse()) {
            return false;
        }
        stop();
        body.clearActions();
        playmate = body;
        playWorld = SimYukkuri.world;
        playMap = playWorld.getCurrentWorldState();
        running = new RunningWithPlayerEvent(body);
        body.setCurrentEvent(running);
        running.start(body);
        return true;
    }

    /** 現在のマップ上で健康かつ移動可能な個体かを判定します。 */
    public static boolean canRun(Yukkuri body) {
        return body != null
                && SimYukkuri.world != null
                && SimYukkuri.world
                                .getCurrentWorldState()
                                .getYukkuriRegistry()
                                .get(body.getUniqueId())
                        == body
                && !body.isRemoved()
                && body.canActionForEvent()
                && !body.isGrabbed()
                && !body.isTaken()
                && !body.isUnBirth()
                && !body.isLockmove()
                && !body.isBlind()
                && !body.isEating()
                && !body.isOnNonMovingConveyor()
                && !body.isHungry()
                && !body.isSleepy()
                && !body.isSick()
                && !body.isDamaged()
                && !body.isMelt()
                && body.getFootBakeLevel() == FootBake.NONE
                && body.getPanicType() == null
                && !body.nearToBirth();
    }

    /** 現在のセッションで指定個体と遊んでいるかを返します。 */
    public static boolean isPlayingWith(Yukkuri body) {
        return body != null
                && body == playmate
                && running != null
                && body.getCurrentEvent() == running
                && playWorld == SimYukkuri.world
                && playMap == SimYukkuri.world.getCurrentWorldState()
                && playMap.getYukkuriRegistry().get(body.getUniqueId()) == body;
    }

    /** Also runs when an event cannot be updated (removed body, panic or a different map). */
    public static void tick() {
        if (running != null && (!isPlayingWith(playmate) || !canRun(playmate))) {
            stop();
        }
    }

    /** マップ内に補正した追跡目標を設定します。 */
    public static void moveTarget(int x, int y) {
        tick();
        if (running != null) {
            running.setRunningTarget(
                    Math.max(0, Math.min(x, Translate.getWorldWidth() - 1)),
                    Math.max(0, Math.min(y, Translate.getWorldHeight() - 1)));
        }
    }

    /** カーソルが地面を離れた場合に追跡を停止します。 */
    public static void pauseTarget() {
        if (running != null) {
            running.pauseTarget(playmate);
        }
    }

    /** 追跡を終了し、イベントと移動先を解放します。 */
    public static void stop() {
        Yukkuri previous = playmate;
        RunningWithPlayerEvent previousEvent = running;
        clearSession();
        if (previous != null && previous.getCurrentEvent() == previousEvent) {
            previous.clearActions();
        }
    }

    /** EventLogic and Yukkuri.clearActions may both invoke end, so cleanup is idempotent. */
    public static void eventEnded(RunningWithPlayerEvent event) {
        if (running == event) {
            clearSession();
        }
    }

    private static void clearSession() {
        playmate = null;
        running = null;
        playWorld = null;
        playMap = null;
    }
}
