package org.simyukkuri.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.engine.World;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.entity.core.living.yukkuri.impl.Marisa;
import org.simyukkuri.enums.AgeState;
import org.simyukkuri.event.EventPacket;
import org.simyukkuri.event.impl.RunningWithPlayerEvent;
import org.simyukkuri.system.Sprite;
import org.simyukkuri.util.WorldTestHelper;

/** Headless behavioral regression checks using the game's actual movement engine. */
public final class PlayerPlayLogicTest {

    /** 独自機能の回帰検証を構築します。 */
    public PlayerPlayLogicTest() {}

    @Test
    void invalidParticipantsRegression() throws Exception {
        invalidParticipants();
    }

    @Test
    void selectedParticipantRegression() throws Exception {
        selectedParticipant();
    }

    @Test
    void movementAndRewardRegression() throws Exception {
        movementAndReward();
    }

    @Test
    void stationaryPointerRegression() throws Exception {
        stationaryPointer();
    }

    @Test
    void pausePointerRegression() throws Exception {
        pausePointer();
    }

    @Test
    void switchAndStopRegression() throws Exception {
        switchAndStop();
    }

    @Test
    void participantRemovalRegression() throws Exception {
        participantRemoval();
    }

    @Test
    void worldAndMapChangeRegression() throws Exception {
        worldAndMapChange();
    }

    @Test
    void eventReplacementRegression() throws Exception {
        eventReplacement();
    }

    @Test
    void durationLimitRegression() throws Exception {
        durationLimit();
    }

    @Test
    void serializedEventRegression() throws Exception {
        serializedEvent();
    }

    @AfterEach
    void tearDown() {
        PlayerPlayLogic.stop();
        WorldTestHelper.resetWorld();
    }

    private static void invalidParticipants() {
        reset();
        check(!PlayerPlayLogic.startRunning(null), "null must be rejected");
        Yukkuri body = body(100, 100);
        body.setDead(true);
        check(!PlayerPlayLogic.startRunning(body), "dead participant must be rejected");
        body.setDead(false);
        body.setSleeping(true);
        check(!PlayerPlayLogic.startRunning(body), "sleeping participant must be rejected");
        body.setSleeping(false);
        body.setHungry(0);
        check(!PlayerPlayLogic.startRunning(body), "hungry participant must be rejected");
        body.setHungry(body.getHungryLimit());
        body.setOnNonMovingConveyor(true);
        check(
                !PlayerPlayLogic.startRunning(body),
                "immobilized conveyor participant must be rejected");
        body.setOnNonMovingConveyor(false);
        check(PlayerPlayLogic.startRunning(body), "healthy participant must be accepted");
        body.setOnNonMovingConveyor(true);
        PlayerPlayLogic.tick();
        check(!PlayerPlayLogic.isPlayingWith(body), "immobilization during play must stop session");
        body.setOnNonMovingConveyor(false);
        clean(body);
    }

    private static void selectedParticipant() {
        reset();
        Yukkuri selected = body(100, 100);
        final Yukkuri bystander = body(200, 100);
        check(PlayerPlayLogic.startRunning(selected), "start");
        EventPacket event = selected.getCurrentEvent();
        check(event instanceof RunningWithPlayerEvent, "selected receives running event");
        check(event.getPriority() == EventPacket.EventPriority.LOW, "basic needs retain priority");
        check(!event.checkEventResponse(bystander), "bystander cannot join");
        check(EventLogic.checkWorldEvent(bystander) == null, "no broadcast world event");
        check(bystander.getCurrentEvent() == null, "bystander remains unaffected");
        check(PlayerPlayLogic.startRunning(selected), "repeated start succeeds");
        check(event == selected.getCurrentEvent(), "repeated start keeps existing session");
        check(PlayerPlayLogic.isPlayingWith(selected), "selected is current participant");
        check(!PlayerPlayLogic.isPlayingWith(bystander), "only selected is current participant");
    }

    private static void movementAndReward() {
        reset();
        Yukkuri body = body(30, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        PlayerPlayLogic.moveTarget(85, 100);
        int previousX = body.getX();
        int firstRewardFrame = -1;
        for (int frame = 1; frame <= 120; frame++) {
            frame(body);
            check(
                    body.getX() >= previousX && body.getX() - previousX <= 1,
                    "chase uses normal movement without teleporting");
            previousX = body.getX();
            if (body.getLovePlayer() != 0) {
                firstRewardFrame = frame;
                break;
            }
        }
        check(body.getX() > 50, "participant traveled toward pointer");
        check(firstRewardFrame >= 30, "first reward requires elapsed cooldown");
        check(firstRewardFrame <= 120, "catch eventually rewarded");
        equal(5, body.getLovePlayer(), "catch love reward");
        equal(900, body.getStress(), "catch stress reduction");

        // A short new chase must not farm rewards even after waiting out the cooldown.
        PlayerPlayLogic.moveTarget(body.getX() + 8, body.getY());
        frames(body, 50);
        equal(5, body.getLovePlayer(), "less than 20 traveled units gives no further reward");
        PlayerPlayLogic.moveTarget(body.getX() + 40, body.getY());
        awaitLove(body, 10, 70);
        equal(800, body.getStress(), "second catch stress reduction");

        // A faster body reaches the next target before the cooldown expires.
        body.setSpeed(1000);
        PlayerPlayLogic.moveTarget(body.getX() + 40, body.getY());
        frames(body, 28);
        equal(10, body.getLovePlayer(), "new distance does not bypass reward cooldown");
        awaitLove(body, 15, 10);
        equal(700, body.getStress(), "third bounded catch reward");
    }

    private static void stationaryPointer() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        PlayerPlayLogic.moveTarget(body.getX(), body.getY());
        frames(body, 180);
        equal(0, body.getLovePlayer(), "waiting at pointer gives no love");
        equal(1000, body.getStress(), "waiting at pointer gives no stress benefit");
        equal(100, body.getX(), "stationary target holds horizontal position");
        equal(100, body.getY(), "stationary target holds vertical position");
    }

    private static void pausePointer() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        PlayerPlayLogic.moveTarget(180, 100);
        frames(body, 10);
        PlayerPlayLogic.pauseTarget();
        int x = body.getX();
        int y = body.getY();
        frames(body, 70);
        equal(x, body.getX(), "paused pointer stops chase horizontally");
        equal(y, body.getY(), "paused pointer stops chase vertically");
        equal(0, body.getLovePlayer(), "paused pointer cannot reward");
        PlayerPlayLogic.moveTarget(x + 50, y);
        frames(body, 10);
        check(body.getX() > x, "entering field again resumes chase");
    }

    private static void switchAndStop() {
        reset();
        Yukkuri first = body(100, 100);
        final Yukkuri second = body(220, 100);
        check(PlayerPlayLogic.startRunning(first), "first start");
        PlayerPlayLogic.moveTarget(150, 100);
        frames(first, 3);
        check(PlayerPlayLogic.startRunning(second), "second start");
        check(!PlayerPlayLogic.isPlayingWith(first), "previous participant released");
        clean(first);
        check(PlayerPlayLogic.isPlayingWith(second), "replacement participant selected");
        PlayerPlayLogic.stop();
        clean(second);
        check(!PlayerPlayLogic.isPlayingWith(second), "stop releases participant");
        PlayerPlayLogic.stop();
        clean(second);
    }

    private static void participantRemoval() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().remove(body.getUniqueId());
        PlayerPlayLogic.tick();
        check(!PlayerPlayLogic.isPlayingWith(body), "removed participant terminates session");
        clean(body);
    }

    private static void worldAndMapChange() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start before map change");
        SimYukkuri.world.setCurrentWorldStateIndex(1);
        PlayerPlayLogic.tick();
        check(!PlayerPlayLogic.isPlayingWith(body), "map change terminates session");
        clean(body);
        reset();
        body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start before world load");
        SimYukkuri.world = new World(0, 0);
        PlayerPlayLogic.tick();
        check(!PlayerPlayLogic.isPlayingWith(body), "world replacement terminates session");
        clean(body);
    }

    private static void eventReplacement() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        EventPacket replacement = new OtherEvent();
        body.setCurrentEvent(replacement);
        PlayerPlayLogic.tick();
        check(!PlayerPlayLogic.isPlayingWith(body), "replacement cancels session");
        check(body.getCurrentEvent() == replacement, "unrelated replacement event preserved");
    }

    private static void durationLimit() {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        for (int i = 0; i < 600 && PlayerPlayLogic.isPlayingWith(body); i++) {
            frame(body);
        }
        check(!PlayerPlayLogic.isPlayingWith(body), "session limited to 600 updates");
        clean(body);
    }

    private static void serializedEvent() throws Exception {
        reset();
        Yukkuri body = body(100, 100);
        check(PlayerPlayLogic.startRunning(body), "start");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(body.getCurrentEvent());
        }
        EventPacket loaded;
        try (ObjectInputStream input =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            loaded = (EventPacket) input.readObject();
        }
        PlayerPlayLogic.stop();
        body.setCurrentEvent(loaded);
        loaded.start(body);
        EventLogic.eventUpdate(body);
        clean(body);
        check(!PlayerPlayLogic.isPlayingWith(body), "loaded event cannot recreate pointer session");
    }

    private static void reset() {
        PlayerPlayLogic.stop();
        WorldTestHelper.resetWorld();
        WorldTestHelper.initializeMinimalWorld();
        SimYukkuri.RND.setSeed(20260929L);
        SimYukkuri.world = new World(0, 0);
    }

    private static Yukkuri body(int x, int y) {
        Yukkuri body = new ProbeBody(x, y);
        body.setHungry(body.getHungryLimit());
        body.setStress(1000);
        body.setLovePlayer(0);
        body.setSpeed(100);
        body.setSleeping(false);
        SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().put(body.getUniqueId(), body);
        return body;
    }

    private static void frame(Yukkuri body) {
        PlayerPlayLogic.tick();
        body.setAge(body.getAge() + 1);
        body.setStaying(false);
        body.moveYukkuri(false);
        EventLogic.eventUpdate(body);
    }

    private static void frames(Yukkuri body, int count) {
        for (int i = 0; i < count; i++) {
            frame(body);
        }
    }

    private static void awaitLove(Yukkuri body, int expected, int maxFrames) {
        for (int i = 0; i < maxFrames && body.getLovePlayer() < expected; i++) {
            frame(body);
        }
        equal(expected, body.getLovePlayer(), "expected catch reward within frame limit");
    }

    private static void clean(Yukkuri body) {
        check(body.getCurrentEvent() == null, "running event removed");
        equal(-1, body.getDestX(), "horizontal chase destination cleared");
        equal(-1, body.getDestY(), "vertical chase destination cleared");
    }

    private static void equal(int expected, int actual, String label) {
        check(expected == actual, label + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static final class OtherEvent extends EventPacket {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean checkEventResponse(Yukkuri body) {
            return false;
        }

        @Override
        public void start(Yukkuri body) {}

        @Override
        public boolean execute(Yukkuri body) {
            return false;
        }
    }

    private static final class ProbeBody extends Marisa {
        private static final long serialVersionUID = 1L;

        ProbeBody(int x, int y) {
            super(x, y, 0, AgeState.ADULT, null, null);
            setSpriteSet(
                    new Sprite[] {
                        new Sprite(40, 30, Sprite.PIVOT_CENTER_BOTTOM),
                        new Sprite(50, 35, Sprite.PIVOT_CENTER_BOTTOM),
                        new Sprite(60, 40, Sprite.PIVOT_CENTER_BOTTOM)
                    });
        }
    }
}
