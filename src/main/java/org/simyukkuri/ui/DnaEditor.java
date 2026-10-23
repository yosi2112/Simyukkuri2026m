package org.simyukkuri.ui;

import java.util.Arrays;
import org.simyukkuri.enums.YukkuriType;

/** Species choices for the custom DNA editor. */
public final class DnaEditor {
    private DnaEditor() {}

    /** 種別に対応する日本語名を返します。 */
    public static String getYukkuriNameByType(YukkuriType type) {
        return type.getNameJ();
    }

    /** 表示名に対応する種別を返します。 */
    public static YukkuriType getYukkuriTypeByName(String name) {
        return Arrays.stream(YukkuriType.values())
                .filter(t -> t.getNameJ().equals(name))
                .findFirst()
                .orElse(YukkuriType.TARINAI);
    }

    /** 編集可能な全種別の表示名を返します。 */
    public static String[] getAllYukkuriNames() {
        return Arrays.stream(YukkuriType.values())
                .map(YukkuriType::getNameJ)
                .toArray(String[]::new);
    }
}
