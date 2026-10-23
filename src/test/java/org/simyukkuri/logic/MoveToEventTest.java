package org.simyukkuri.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.simyukkuri.ConstState;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.entity.core.living.yukkuri.impl.Marisa;
import org.simyukkuri.event.EventPacket;
import org.simyukkuri.util.WorldTestHelper;

/** Checks event destinations and arrival at reachable map boundaries. */
public class MoveToEventTest {

    @BeforeEach
    void setUp() {
        WorldTestHelper.resetWorld();
        WorldTestHelper.initializeMinimalWorld();
        WorldTestHelper.initializeStandardTranslate200();
        SimYukkuri.RND = new ConstState(1);
    }

    @AfterEach
    void tearDown() {
        WorldTestHelper.resetWorld();
    }

    @ParameterizedTest
    @CsvSource({
            "100, 200, 50, 100, 200, 50",
            "0, 0, 0, 0, 0, 0",
            "1001, 1001, 201, 1001, 1001, 201",
            "-50, 200, 50, 0, 200, 50",
            "1051, 200, 50, 1001, 200, 50",
            "100, -50, 50, 100, 0, 50",
            "100, 1051, 50, 100, 1001, 50",
            "100, 200, -50, 100, 200, 0",
            "100, 200, 251, 100, 200, 201",
            "-2147483648, -2147483648, -2147483648, 0, 0, 0",
            "2147483647, 2147483647, 2147483647, 1001, 1001, 201"
    })
    void eventCoordinatesMatchReachableDestination(
            int x, int y, int z, int expectedX, int expectedY, int expectedZ) {
        Yukkuri body = createBody();
        RecordingEvent event = new RecordingEvent();

        body.moveToEvent(event, x, y, z);

        assertDestination(body, event, expectedX, expectedY, expectedZ);
    }

    @Test
    void groundOverloadResetsDestinationHeight() {
        Yukkuri body = createBody();
        RecordingEvent event = new RecordingEvent();
        body.moveToEvent(event, 100, 200, 50);

        body.moveToEvent(event, 100, 200);

        assertDestination(body, event, 100, 200, 0);
    }

    @Test
    void flyingBodyUsesClampedThreeDimensionalDestination() {
        Yukkuri body = createBody();
        body.setFlyingType(true);
        assertTrue(body.canflyCheck());
        RecordingEvent event = new RecordingEvent();

        body.moveToEvent(event, 1051, -50, 251);

        assertDestination(body, event, 1001, 0, 201);
    }

    @Test
    void deadBodyLeavesEventAndMovementDestinationsUntouched() {
        Yukkuri body = createBody();
        body.setDestX(400);
        body.setDestY(500);
        body.setDestZ(60);
        RecordingEvent event = new RecordingEvent();
        event.setToX(100);
        event.setToY(200);
        event.setToZ(30);
        body.setDead(true);

        body.moveToEvent(event, -50, 1051, 251);

        assertEquals(400, body.getDestX());
        assertEquals(500, body.getDestY());
        assertEquals(60, body.getDestZ());
        assertEquals(100, event.getToX());
        assertEquals(200, event.getToY());
        assertEquals(30, event.getToZ());
    }

    @ParameterizedTest
    @CsvSource({
            "-50, 100, 0, 0, 100, 0",
            "1051, 100, 0, 1001, 100, 0",
            "100, -50, 0, 100, 0, 0",
            "100, 1051, 0, 100, 1001, 0",
            "100, 100, -50, 100, 100, 0",
            "100, 100, 251, 100, 100, 201"
    })
    void reachingClampedEndpointAllowsEventExecution(
            int x, int y, int z, int reachedX, int reachedY, int reachedZ) {
        Yukkuri body = createBody();
        body.setX(reachedX);
        body.setY(reachedY);
        body.setZ(reachedZ);
        body.setFlyingType(reachedZ != 0);
        RecordingEvent event = new RecordingEvent();
        body.setCurrentEvent(event);
        body.moveToEvent(event, x, y, z);

        EventLogic.eventUpdate(body);

        assertTrue(event.executed, "Reaching a clamped endpoint must execute the event");
    }

    @Test
    void eventWaitsUntilBodyReachesEndpoint() {
        Yukkuri body = createBody();
        body.setX(0);
        body.setY(100);
        body.setZ(0);
        RecordingEvent event = new RecordingEvent();
        body.setCurrentEvent(event);
        body.moveToEvent(event, 1051, 100);

        EventLogic.eventUpdate(body);

        assertFalse(event.executed);
    }

    @Test
    void eventWaitsForMatchingHeightEvenWhenHorizontalPositionMatches() {
        Yukkuri body = createBody();
        body.setX(100);
        body.setY(100);
        body.setZ(200);
        body.setFlyingType(true);
        assertTrue(body.canflyCheck());
        RecordingEvent event = new RecordingEvent();
        body.setCurrentEvent(event);
        body.moveToEvent(event, 100, 100, 251);

        EventLogic.eventUpdate(body);

        assertFalse(event.executed);
        body.setZ(201);
        EventLogic.eventUpdate(body);
        assertTrue(event.executed);
    }

    @Test
    void sharedEventKeepsLatestEndpointWithoutOverwritingOtherBodyMovement() {
        Yukkuri first = createBody();
        Yukkuri second = createBody();
        RecordingEvent event = new RecordingEvent();
        first.moveToEvent(event, -50, 100, 0);

        second.moveToEvent(event, 1051, 200, 251);

        assertEquals(0, first.getDestX());
        assertEquals(100, first.getDestY());
        assertEquals(0, first.getDestZ());
        assertDestination(second, event, 1001, 200, 201);
    }

    private Yukkuri createBody() {
        Yukkuri body = new Marisa();
        body.setDead(false);
        body.setDamage(0);
        body.setHasBraid(true);
        return body;
    }

    private void assertDestination(
            Yukkuri body, EventPacket event, int x, int y, int z) {
        assertEquals(x, body.getDestX());
        assertEquals(y, body.getDestY());
        assertEquals(z, body.getDestZ());
        assertEquals(x, event.getToX());
        assertEquals(y, event.getToY());
        assertEquals(z, event.getToZ());
    }

    private static final class RecordingEvent extends EventPacket {
        private static final long serialVersionUID = 1L;
        private boolean executed;

        @Override
        public boolean checkEventResponse(Yukkuri body) {
            return true;
        }

        @Override
        public void start(Yukkuri body) {
            // The test controls when movement starts.
        }

        @Override
        public boolean execute(Yukkuri body) {
            executed = true;
            return false;
        }
    }
}
