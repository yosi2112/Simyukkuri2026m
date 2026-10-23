package org.simyukkuri.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.command.GadgetAction;
import org.simyukkuri.command.GadgetMenu;
import org.simyukkuri.command.GadgetMenu.GadgetMenuChoice;
import org.simyukkuri.command.GadgetMenu.MainCategoryName;
import org.simyukkuri.draw.MyPane;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.Attitude;
import org.simyukkuri.enums.CoreAnkoState;
import org.simyukkuri.enums.Intelligence;
import org.simyukkuri.enums.YukkuriType;
import org.simyukkuri.system.MainCommandSelection;
import org.simyukkuri.ui.DnaEditor;
import org.simyukkuri.ui.InputController;
import org.simyukkuri.util.WorldTestHelper;

class MakaizouIntegrationTest {

    @BeforeEach
    void setUp() {
        WorldTestHelper.resetWorld();
        WorldTestHelper.initializeMinimalWorld();
        SimYukkuri.allowPeniWrestling = false;
    }

    @AfterEach
    void tearDown() {
        PlayerPlayLogic.stop();
        MyPane.setSelectedYukkuri(null);
        SimYukkuri.allowPeniWrestling = false;
        WorldTestHelper.resetWorld();
    }

    @Test
    void existingCategoriesKeepTheirPopupIndices() {
        GadgetMenuChoice[] categories = GadgetMenu.getMainCategory();
        assertEquals(GadgetMenuChoice.TOOL, categories[0]);
        assertEquals(GadgetMenuChoice.DEBUG, categories[12]);
        assertEquals(GadgetMenuChoice.PLAY, categories[13]);
        for (int i = 0; i < categories.length; i++) {
            assertEquals(i + 1, MainCategoryName.valueOf(categories[i].name()).ordinal());
            assertNotNull(MainCommandSelection.getSubItem(categories[i], 0));
        }
        assertEquals(
                GadgetMenuChoice.RUN_WITH_PLAYER,
                MainCommandSelection.getSubItem(GadgetMenuChoice.PLAY, 0));
        assertEquals(
                GadgetMenuChoice.STOP_PLAY,
                MainCommandSelection.getSubItem(GadgetMenuChoice.PLAY, 1));
    }

    @Test
    void customShortcutEditsOnlyTheSelectedIndividual() {
        Yukkuri selected = WorldTestHelper.createBody();
        Yukkuri bystander = WorldTestHelper.createBody();
        bystander.setAttitude(Attitude.AVERAGE);
        bystander.setIntelligence(Intelligence.AVERAGE);
        MyPane.setSelectedYukkuri(selected);
        new InputController().handleKeyPressed(key(KeyEvent.VK_K), null);
        assertEquals(Attitude.VERY_NICE, selected.getAttitude());
        assertEquals(Intelligence.WISE, selected.getIntelligence());
        assertEquals(Attitude.AVERAGE, bystander.getAttitude());
        assertEquals(Intelligence.AVERAGE, bystander.getIntelligence());
    }

    @Test
    void diseaseResetUsesTheCurrentTypedState() {
        Yukkuri selected = WorldTestHelper.createBody();
        selected.setCoreAnkoState(CoreAnkoState.NON_YUKKURI_DISEASE);
        MyPane.setSelectedYukkuri(selected);
        new InputController().handleKeyPressed(key(KeyEvent.VK_Y), null);
        assertEquals(CoreAnkoState.NORMAL, selected.getCoreAnkoState());
    }

    @Test
    void speciesNamesRoundTripThroughTypedDna() {
        assertTrue(DnaEditor.getAllYukkuriNames().length > 20);
        for (YukkuriType type : YukkuriType.values()) {
            assertSame(type, DnaEditor.getYukkuriTypeByName(DnaEditor.getYukkuriNameByType(type)));
        }
    }

    @Test
    void invincibilityCommandTogglesTheCurrentIndividual() {
        Yukkuri body = WorldTestHelper.createBody();
        MouseEvent click =
                new MouseEvent(
                        new Canvas(),
                        MouseEvent.MOUSE_CLICKED,
                        0,
                        0,
                        0,
                        0,
                        1,
                        false,
                        MouseEvent.BUTTON1);
        GadgetAction.leftClickEvaluate(GadgetMenuChoice.CANTDIE, body, click, new int[2]);
        assertTrue(body.getCantDiePeriod() > 0);
        GadgetAction.leftClickEvaluate(GadgetMenuChoice.CANTDIE, body, click, new int[2]);
        assertEquals(0, body.getCantDiePeriod());
    }

    @Test
    void stopSelectionEndsPlayWithoutOverwritingOtherEvents() {
        Yukkuri body = WorldTestHelper.createBody();
        assertNull(body.getCurrentEvent());
        GadgetMenu.setSelectSub(GadgetMenuChoice.STOP_PLAY);
        assertFalse(PlayerPlayLogic.isPlayingWith(body));
        assertNull(body.getCurrentEvent());
    }

    @Test
    void originalSplashImageIsPackagedAndReadable() throws Exception {
        assertNotNull(ImageIO.read(SimYukkuri.class.getResource("/images/SimYukkuri_splash.png")));
    }

    private static KeyEvent key(int code) {
        return new KeyEvent(
                new Canvas(), KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
    }
}
