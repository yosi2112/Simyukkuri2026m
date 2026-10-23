package org.simyukkuri.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JOptionPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.simyukkuri.enums.YukkuriType;

class MyPaneDialogTest {
    @Test
    void shouldProceedAfterAddDialog_onlyAcceptsOk() {
        assertTrue(MyPane.shouldProceedAfterAddDialog(JOptionPane.OK_OPTION));
        assertFalse(MyPane.shouldProceedAfterAddDialog(JOptionPane.CANCEL_OPTION));
        assertFalse(MyPane.shouldProceedAfterAddDialog(JOptionPane.CLOSED_OPTION));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, MARISA", "0, 1, REIMU", "0, 2, ALICE", "0, 3, PATCH", "0, 4, CHEN", "0, 5, MYON",
        "1, 0, YURUSANAE", "1, 1, AYAYA", "1, 2, TENKO", "1, 3, UDONGE", "1, 4, MEIRIN", "1, 5, SUWAKO",
        "1, 6, CHIRUNO", "1, 7, EIKI", "1, 8, RAN", "1, 9, NITORI", "1, 10, YUUKA", "1, 11, SAKUYA",
        "2, 0, REMIRYA", "2, 1, FRAN", "2, 2, YUYUKO"
    })
    void selectedSpeciesMatchesBothDialogLabels(int category, int index, YukkuriType expected) {
        assertEquals(expected, MyPane.resolveSelectedType(category, index));
        String[][] japanese = {MyPane.namesCommonJ, MyPane.namesRareJ, MyPane.namesPredatorJ};
        String[][] english = {MyPane.namesCommonE, MyPane.namesRareE, MyPane.namesPredatorE};
        assertEquals(expected.getJapaneseName(), japanese[category][index]);
        assertEquals(expected.getEnglishName(), english[category][index]);
    }
}
