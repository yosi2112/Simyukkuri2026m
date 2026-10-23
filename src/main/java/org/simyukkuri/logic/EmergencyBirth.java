package org.simyukkuri.logic;

import java.util.ArrayList;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.entity.core.living.yukkuri.Dna;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.AgeState;
import org.simyukkuri.enums.Happiness;
import org.simyukkuri.system.MessagePool;
import org.simyukkuri.util.GameMessages;
import org.simyukkuri.util.YukkuriLookup;

/** 比較元の緊急出産操作を現行の個体生成APIへ接続します。 */
public final class EmergencyBirth {
    private EmergencyBirth() {}

    /** 胎内の子を取り出し、取り出せなかったDNAは保持します。 */
    public static boolean perform(Yukkuri mother) {
        if (mother == null || mother.isDead() || SimYukkuri.mypane == null
                || mother.getBabyTypes().isEmpty()) {
            return false;
        }
        int extracted = 0;
        for (Dna dna : new ArrayList<>(mother.getBabyTypes())) {
            Yukkuri baby = SimYukkuri.mypane.getTerrarium().makeYukkuri(
                    mother.getX(), mother.getY(), mother.getZ(), dna, AgeState.BABY,
                    mother, YukkuriLookup.getYukkuriById(dna.getFather()));
            if (baby == null) {
                continue;
            }
            baby.setX(baby.getX() + SimYukkuri.RND.nextInt(20) - 10);
            baby.setY(baby.getY() + SimYukkuri.RND.nextInt(20) - 10);
            baby.setUnBirth(false);
            baby.setBirth(false);
            SimYukkuri.mypane.getTerrarium().addYukkuri(baby);
            baby.wakeup();
            mother.getBabyTypes().remove(dna);
            extracted++;
        }
        if (extracted == 0) {
            return false;
        }
        mother.addDamage(extracted * 10);
        mother.addStress(extracted * 50);
        mother.setHappiness(Happiness.SAD);
        mother.setMessage(GameMessages.getMessage(mother, MessagePool.Action.Birth), true);
        mother.addLovePlayer(10);
        mother.setHasBaby(!mother.getBabyTypes().isEmpty());
        if (mother.getBabyTypes().isEmpty()) {
            mother.setPregnantPeriod(0);
            mother.setShit(mother.getShitLimit() / 2);
        }
        return true;
    }
}
