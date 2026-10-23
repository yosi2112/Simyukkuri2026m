package org.simyukkuri.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.simyukkuri.ConstState;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.entity.core.living.yukkuri.impl.Marisa;
import org.simyukkuri.enums.AgeState;
import org.simyukkuri.enums.Intelligence;
import org.simyukkuri.enums.Numbering;
import org.simyukkuri.logic.EventLogic;
import org.simyukkuri.system.Sprite;
import org.simyukkuri.util.WorldTestHelper;

/** Exercises proposal approach through the real movement and event update paths. */
public class ProposeMovementTest {

    @BeforeEach
    void setUp() {
        WorldTestHelper.resetWorld();
        WorldTestHelper.initializeMinimalWorld();
        WorldTestHelper.initializeStandardTranslate200();
        SimYukkuri.RND = new ConstState(1);
        SimYukkuri.world.getCurrentWorldState().setWallGrid(new int[1002][1002]);
        SimYukkuri.world.getCurrentWorldState().setFieldGrid(new int[1002][1002]);
    }

    @AfterEach
    void tearDown() {
        WorldTestHelper.resetWorld();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void startUsesSourceSizeForEitherParticipant(boolean sourceStarts) {
        Yukkuri source = createBody(500, 20);
        Yukkuri target = createBody(200, 80);
        ProposeEvent event = new ProposeEvent(source, target, null, 1);

        event.start(sourceStarts ? source : target);

        assertEquals(237, source.getDestX());
        assertEquals(237, event.getToX());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void waitingTargetCannotOverwriteSourceContactPoint(boolean sourceFirst) {
        Yukkuri source = createBody(500, 20);
        Yukkuri target = createBody(200, 80);
        ProposeEvent event = new ProposeEvent(source, target, null, 1);
        event.start(source);

        event.update(sourceFirst ? source : target);
        assertEquals(237, source.getDestX());
        event.update(sourceFirst ? target : source);
        assertEquals(237, source.getDestX());
        assertEquals(237, event.getToX());
    }

    @ParameterizedTest
    @CsvSource({
            "500, 200, 20, 80, true",
            "500, 200, 20, 80, false",
            "50, 200, 20, 80, true",
            "50, 200, 20, 80, false",
            "500, 200, 80, 20, true",
            "500, 200, 80, 20, false",
            "500, 200, 40, 40, true",
            "500, 200, 40, 40, false",
            "50, 200, 40, 40, true",
            "50, 200, 40, 40, false"
    })
    void stationaryTargetIsReachedWithDifferentSizesAndUpdateOrders(
            int sourceX, int targetX, int sourceWidth, int targetWidth, boolean sourceFirst) {
        assertProposalStarts(sourceX, targetX, sourceWidth, targetWidth, sourceFirst);
    }

    @ParameterizedTest
    @CsvSource({
            "995, 990, true",
            "995, 990, false",
            "5, 10, true",
            "5, 10, false"
    })
    void proposalStartsAtMapBoundary(int sourceX, int targetX, boolean sourceFirst) {
        assertProposalStarts(sourceX, targetX, 40, 40, sourceFirst);
    }

    private void assertProposalStarts(
            int sourceX, int targetX, int sourceWidth, int targetWidth, boolean sourceFirst) {
        Yukkuri source = createBody(sourceX, sourceWidth);
        Yukkuri target = createBody(targetX, targetWidth);
        ProposeEvent event = new ProposeEvent(source, target, null, 1);
        event.start(source);

        for (int tick = 1; tick <= 1000 && !event.isStarted(); tick++) {
            source.setAge(source.getChildLimitBase() + tick);
            if (sourceFirst) {
                source.moveYukkuri(false);
                EventLogic.eventUpdate(source);
                EventLogic.eventUpdate(target);
            } else {
                EventLogic.eventUpdate(target);
                source.moveYukkuri(false);
                EventLogic.eventUpdate(source);
            }
        }

        assertTrue(event.isStarted(), "Unobstructed proposal must start within 1000 ticks");
        assertEquals(targetX, target.getX(), "Target must remain stationary");
        assertEquals(100, target.getY());
        assertEquals(0, target.getZ());
    }

    private Yukkuri createBody(int x, int width) {
        Yukkuri body = new Marisa();
        body.setUniqueId(Numbering.INSTANCE.numberingYukkuriId());
        body.setX(x);
        body.setY(100);
        body.setZ(0);
        body.setAgeState(AgeState.ADULT);
        body.setDead(false);
        body.setDamage(0);
        body.setHungry(body.getHungryLimit());
        body.setFlyingType(false);
        body.setHasBaby(false);
        body.setHasStalk(false);
        body.setBlind(false);
        body.setIntelligence(Intelligence.AVERAGE);
        body.setSpeed(100);
        Sprite[] sprites = new Sprite[3];
        for (int index = 0; index < sprites.length; index++) {
            sprites[index] = new Sprite(width, 40, Sprite.PIVOT_CENTER_BOTTOM);
        }
        body.setSpriteSet(sprites);
        SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().put(body.getUniqueId(), body);
        return body;
    }
}
