package org.simyukkuri.ui;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.command.GadgetAction;
import org.simyukkuri.command.GadgetMenu.GadgetMenuChoice;
import org.simyukkuri.draw.MyPane;
import org.simyukkuri.draw.Translate;
import org.simyukkuri.entity.core.living.yukkuri.Dna;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.Attitude;
import org.simyukkuri.enums.Burst;
import org.simyukkuri.enums.CoreAnkoState;
import org.simyukkuri.enums.Happiness;
import org.simyukkuri.enums.ImageCode;
import org.simyukkuri.enums.Intelligence;
import org.simyukkuri.event.EventPacket;
import org.simyukkuri.event.impl.FuneralEvent;
import org.simyukkuri.logic.EmergencyBirth;
import org.simyukkuri.logic.EventLogic;
import org.simyukkuri.util.GameMessages;

/** 比較元の拡張ショートカットを現行APIへ接続します。 */
public final class InputController {

    private int savedGameSpeed;

    /** 選択個体と画面に対するショートカットを処理します。 */
    public void handleKeyPressed(KeyEvent e, SimYukkuri owner) {
        switch (e.getKeyCode()) {

            // 画面操作系ショートカット (変更不可)

            case KeyEvent.VK_ESCAPE:
                synchronized (SimYukkuri.lock) {
                    if (MainCommandUi.getSelectedGameSpeed() != 0) {
                        savedGameSpeed = MainCommandUi.getSelectedGameSpeed();
                        MainCommandUi.setSelectedGameSpeed(0);
                    } else {
                        MainCommandUi.setSelectedGameSpeed(savedGameSpeed);
                    }
                    MainCommandUi.getGameSpeedCombo()
                            .setSelectedIndex(MainCommandUi.getSelectedGameSpeed());
                }
                break;
            case KeyEvent.VK_DELETE:
                synchronized (SimYukkuri.lock) {
                    GadgetAction.immediateEvaluate(GadgetMenuChoice.ALL);
                }
                break;
            case KeyEvent.VK_Z:
            case KeyEvent.VK_X:
                synchronized (SimYukkuri.lock) {
                    int zoomDir = (e.getKeyCode() == KeyEvent.VK_Z) ? -1 : 1;
                    if (Translate.addZoomRate(zoomDir)) {
                        java.awt.Point mpos = owner.getMousePosition();
                        Translate.setBufferZoom();
                        if (mpos != null) {
                            Translate.transCanvasToField(mpos.x, mpos.y, SimYukkuri.fieldMousePos);
                            Translate.setBufferCenterPos(
                                    SimYukkuri.fieldMousePos[0], SimYukkuri.fieldMousePos[1]);
                        }
                    }
                }
                break;
            case KeyEvent.VK_C:
                synchronized (SimYukkuri.lock) {
                    Translate.setZoomRate(0);
                    Translate.setBufferPos(0, 0);
                    Translate.setBufferZoom();
                }
                break;
            case KeyEvent.VK_W:
            case KeyEvent.VK_S:
            case KeyEvent.VK_A:
            case KeyEvent.VK_D:
                synchronized (SimYukkuri.lock) {
                    int dx = 0;
                    int dy = 0;
                    int moveAmountX = Translate.getDisplayArea().getWidth() / 3;
                    int moveAmountY = Translate.getDisplayArea().getHeight() / 3;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_W:
                            dy = -moveAmountY;
                            break;
                        case KeyEvent.VK_S:
                            dy = moveAmountY;
                            break;
                        default:
                            break;
                        case KeyEvent.VK_A:
                            dx = -moveAmountX;
                            break;
                        case KeyEvent.VK_D:
                            dx = moveAmountX;
                            break;
                    }
                    Translate.addBufferPos(dx, dy);
                }
                break;

            // 選択中のゆっくりに対する操作 (アルファベットキー)

            // 強制老ゆ化
            case KeyEvent.VK_H:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null && !selectedBody.isDead()) {
                        // 老ゆになる年齢(寿命の90% + 1)を設定
                        int oldAge = (int) (selectedBody.getLifeLimitBase() * 0.99) + 1;
                        selectedBody.setAge(oldAge);
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") を強制的に老ゆ化しました。");
                    } else {
                        System.out.println("老ゆ化するゆっくりが選択されていないか、死んでいます。");
                    }
                }
                break;
            // クローン作成
            case KeyEvent.VK_I:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null || selectedBody.isDead()) {
                        System.out.println("クローンするゆっくりが選択されていないか、死亡しています。");
                        break;
                    }
                    // 母親と父親のIDを取得
                    int motherId = selectedBody.getParents()[0]; // ※インデックスは要確認
                    int fatherId = selectedBody.getParents()[1];

                    // IDからBodyインスタンスを取得
                    Yukkuri mother =
                            org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                    .getYukkuriRegistry()
                                    .get(motherId);
                    Yukkuri father =
                            org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                    .getYukkuriRegistry()
                                    .get(fatherId);

                    // makeBodyメソッドを正しい引数で呼び出す
                    Yukkuri clone =
                            SimYukkuri.mypane
                                    .getTerrarium()
                                    .makeYukkuri(
                                            selectedBody.getX(),
                                            selectedBody.getY(),
                                            selectedBody.getZ(),
                                            selectedBody.getDna(), // Dnaはそのまま
                                            selectedBody.getAgeState(), // AgeStateを取得
                                            mother, // Bodyインスタンスを渡す
                                            father // Bodyインスタンスを渡す
                                            );

                    if (clone != null) {
                        if (selectedBody.getDna().getType()
                                == org.simyukkuri.enums.YukkuriType.DOSMARISA) {
                            clone.getDna()
                                    .setType(org.simyukkuri.enums.YukkuriType.fromTypeId(2006));
                        } // ドス
                        if (selectedBody.getDna().getType().getTypeId() == 2005) {
                            clone.getDna()
                                    .setType(org.simyukkuri.enums.YukkuriType.fromTypeId(2005));
                        }
                        // 元のゆっくりの近くに配置
                        int newX = selectedBody.getX() + 20 - SimYukkuri.RND.nextInt(40);
                        int newY = selectedBody.getY() + 20 - SimYukkuri.RND.nextInt(40);
                        clone.setX(newX);
                        clone.setY(newY);
                        clone.setZ(0);
                        // テラリウムに追加
                        SimYukkuri.mypane.getTerrarium().addYukkuri(clone);
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") のクローンを作成しました。");
                    }
                }
                break;
            // 賢くする
            case KeyEvent.VK_K:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.setAttitude(Attitude.VERY_NICE);
                        selectedBody.setIntelligence(Intelligence.WISE);
                    }
                }
                break;
            // 即死
            case KeyEvent.VK_L:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.setHungry(0);
                        selectedBody.setDamage(selectedBody.getDamageLimit());
                        selectedBody.setDead(true);
                    }
                }
                break;
            // 堕胎
            case KeyEvent.VK_M:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        if (selectedBody.isHasBaby()) {
                            selectedBody.getBabyTypes().clear(); // 堕胎
                            selectedBody.setHappiness(Happiness.VERY_SAD);
                            selectedBody.setStress(selectedBody.getStress() + 100);
                            selectedBody.setMessage(
                                    GameMessages.getMessage(
                                            selectedBody,
                                            org.simyukkuri.system.MessagePool.Action
                                                    .AbuseBabyKilled),
                                    true);
                        } else {
                            System.out.println(
                                    "Selected Yukkuri: "
                                            + selectedBody.getNameJ()
                                            + " (ID:"
                                            + selectedBody.getUniqueId()
                                            + ")を堕胎しました。");
                        }
                    }
                }
                break;
            // 空腹度を0に
            case KeyEvent.VK_O:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ")の空腹度を0にしました。");
                        selectedBody.setHungry(0);
                    }
                }
                break;
            // 弔いイベント (テスト用)
            case KeyEvent.VK_P:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null) {
                        System.out.println("弔いイベントの対象が選択されていません。");
                        break;
                    }
                    if (selectedBody.getCurrentEvent() instanceof FuneralEvent) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") は既に弔いイベント中です。");
                        break;
                    }
                    if (selectedBody.getChildren() != null
                            && !selectedBody.getChildren().isEmpty()) { // 独身(=配列が空あるいはnull)でなければ
                        for (Integer childId : selectedBody.getChildren()) {
                            Yukkuri child =
                                    org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .get(childId);
                            // int cnt = 0;
                            if (child != null
                                    && selectedBody.isAdult()
                                    && selectedBody.isParent(child)
                                    && child.isDead()) {
                                System.out.println("弔いイベントテスト: ");
                                System.out.println(
                                        "\t親: "
                                                + selectedBody.getNameJ()
                                                + " (ID:"
                                                + selectedBody.getUniqueId()
                                                + ")");
                                System.out.println(
                                        "\t弔う子供: "
                                                + child.getNameJ()
                                                + " (ID:"
                                                + child.getUniqueId()
                                                + ")");
                                EventLogic.addWorldEvent(
                                        new FuneralEvent(selectedBody, child, null, 10),
                                        selectedBody,
                                        null);
                                EventPacket funeral = EventLogic.checkWorldEvent(selectedBody);
                                if (funeral != null) {
                                    selectedBody.setCurrentEvent(funeral);
                                    funeral.start(selectedBody);
                                }
                                break;
                            }
                        }
                    } else {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") は独身です。");
                    }
                }
                break;
            // 蘇生
            case KeyEvent.VK_R:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.revival();
                    }
                }
                break;
            // ドス化・でいぶ化
            case KeyEvent.VK_T:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.execTransform();
                    }
                }
                break;
            // NYD解除
            case KeyEvent.VK_Y:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null
                            && selectedBody.getCoreAnkoState() != CoreAnkoState.NORMAL) {
                        selectedBody.setCoreAnkoState(CoreAnkoState.NORMAL);
                        selectedBody.setStress(0);
                        selectedBody.addStress(selectedBody.getStress() * -1);
                    }
                }
                break;

            // 数字キー操作

            // ゆっくり花火
            case KeyEvent.VK_1:
                Yukkuri selectedForFireworks = MyPane.getSelectedYukkuri();
                if (selectedForFireworks != null && !selectedForFireworks.isDead()) {
                    System.out.println(
                            "Selected Yukkuri: "
                                    + selectedForFireworks.getNameJ()
                                    + " (ID:"
                                    + selectedForFireworks.getUniqueId()
                                    + ") で花火を開始します。");
                    new Thread(
                                    () -> {
                                        while (selectedForFireworks.getBurstState()
                                                        != org.simyukkuri.enums.Burst.BURST
                                                && !selectedForFireworks.isDead()) {
                                            synchronized (SimYukkuri.lock) {
                                                if (selectedForFireworks.isBurst()) {
                                                    System.out.println(
                                                            "Selected Yukkuri: "
                                                                    + selectedForFireworks
                                                                            .getNameJ()
                                                                    + " (ID:"
                                                                    + selectedForFireworks
                                                                            .getUniqueId()
                                                                    + ")  が破裂しました。");
                                                    break;
                                                }
                                                selectedForFireworks.addHungry(100);
                                            }
                                            try {
                                                Thread.sleep(333);
                                            } catch (InterruptedException ex) {
                                                Thread.currentThread().interrupt();
                                                break;
                                            }
                                        }
                                    })
                            .start();
                }
                break;
            case KeyEvent.VK_0:
                synchronized (SimYukkuri.lock) {
                    System.out.println("「ゲスバイバイ」を開始します。");
                    for (Yukkuri b :
                            SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().values()) {

                        b.checkWait(1000);
                        if (b.getAttitude() == Attitude.SHITHEAD
                                || b.getAttitude() == Attitude.SUPER_SHITHEAD) {
                            // ゲスバイバイ
                            b.setHungry(b.getHungryLimit() * 2);
                        }
                    }
                }
                break;
            case KeyEvent.VK_2: // 往復ビンタ（超善良になるまで）
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null || selectedBody.isDead()) {
                        System.out.println("往復ビンタ対象が選択されていないか、既に死亡しています。");
                        break;
                    }

                    int cycleCount = 0;
                    int totalSlaps = 0;
                    int noProgressCycles = 0;
                    final int maxCycles = 200;
                    final int maxSlapsPerCycle = 300;

                    System.out.println(
                            "「往復ビンタ改」を開始: "
                                    + selectedBody.getNameJ()
                                    + " (ID:"
                                    + selectedBody.getUniqueId()
                                    + ")");
                    while (!selectedBody.isDead()
                            && selectedBody.getAttitude() != Attitude.VERY_NICE
                            && cycleCount < maxCycles) {
                        final int beforePoint = selectedBody.getAttitudePoint();
                        final Attitude beforeAttitude = selectedBody.getAttitude();
                        int slapsThisCycle = 0;
                        int damageThreshold = (int) (selectedBody.getDamageLimit() * 0.8);

                        while (!selectedBody.isDead()
                                && selectedBody.getDamage() < damageThreshold
                                && slapsThisCycle < maxSlapsPerCycle) {
                            selectedBody.strikeByPunish();
                            selectedBody.checkWait(500);
                            slapsThisCycle++;
                            totalSlaps++;
                        }

                        if (!selectedBody.isDead()) {
                            selectedBody.giveJuice();
                        }
                        cycleCount++;

                        if (selectedBody.getAttitude() == beforeAttitude
                                && selectedBody.getAttitudePoint() == beforePoint) {
                            noProgressCycles++;
                        } else {
                            noProgressCycles = 0;
                        }
                        if (noProgressCycles >= 10) {
                            System.out.println("性格が変化しない状態が続いたため中断しました。");
                            break;
                        }
                    }
                    System.out.println(
                            "「往復ビンタ改」終了: 総ビンタ回数="
                                    + totalSlaps
                                    + ", ループ回数="
                                    + cycleCount
                                    + ", 最終性格="
                                    + selectedBody.getAttitude()
                                    + ", 最終ポイント="
                                    + selectedBody.getAttitudePoint());
                    if (!selectedBody.isDead()) {
                        selectedBody.giveJuice();
                    }
                }
                break;

            // 全体操作

            case KeyEvent.VK_4: // 全てのゆっくりをきれいに
                synchronized (SimYukkuri.lock) {
                    for (Yukkuri b :
                            SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().values()) {
                        b.setCleaning();
                        b.setStubbornlyDirty(false);
                    }
                }
                break;
            // 全てのゆっくりを回復
            case KeyEvent.VK_U:
                synchronized (SimYukkuri.lock) {
                    List<Yukkuri> bodies =
                            new ArrayList<Yukkuri>(
                                    SimYukkuri.world
                                            .getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .values());
                    int healed = 0;
                    int skipped = 0;
                    for (Yukkuri b : bodies) {
                        if (b == null || b.isRemoved()) {
                            skipped++;
                            continue;
                        }
                        try {
                            b.revival();
                            b.setHungry(Math.max(1000, b.getHungryLimit()));
                            b.giveJuice();
                            b.giveJuice();
                            b.setStress(0);
                            healed++;
                        } catch (Exception ex) {
                            skipped++;
                        }
                    }
                    System.out.println("「全ゆ回復」完了: 回復=" + healed + ", スキップ=" + skipped);
                }
                break;
            // (デバッグ用) 全てのゆっくりを胎ゆ爆発させ回復
            case KeyEvent.VK_J:
                synchronized (SimYukkuri.lock) {
                    List<Yukkuri> bodies =
                            new ArrayList<Yukkuri>(
                                    SimYukkuri.world
                                            .getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .values());
                    int modifiedMothers = 0;
                    int skippedCastrated = 0;
                    int totalAddedBabies = 0;

                    for (Yukkuri mother : bodies) {
                        if (mother == null || mother.isRemoved() || mother.isDead()) {
                            continue;
                        }
                        if (mother.isCastrated()) {
                            skippedCastrated++;
                            continue;
                        }

                        int maxBabies = mother.getMaxHaveBaby();
                        int safeMaxBabies = Math.max(0, maxBabies - 1);
                        int currentBabies = mother.getBabyTypes().size();
                        int numToAdd = Math.max(0, safeMaxBabies - currentBabies);
                        int added = 0;

                        for (int n = 0; n < numToAdd; n++) {
                            Dna babyDna =
                                    new Dna(
                                            mother.getType(),
                                            Attitude.AVERAGE,
                                            Intelligence.AVERAGE,
                                            false);
                            babyDna.setMother(mother.getUniqueId());
                            Yukkuri partner =
                                    org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .get(mother.getPartner());
                            babyDna.setFather(partner != null ? partner.getUniqueId() : -1);
                            mother.getBabyTypes().add(babyDna);
                            added++;
                        }

                        if (added > 0) {
                            mother.setHasBaby(true);
                            if (mother.getPregnantPeriod() <= 0) {
                                mother.setPregnantPeriod(1);
                            }
                            modifiedMothers++;
                            totalAddedBabies += added;
                        }
                    }

                    int revivedBodies = 0;
                    for (Yukkuri b : bodies) {
                        if (b != null && !b.isRemoved() && b.isDead()) {
                            b.setHungry(1000);
                            b.revival();
                            b.giveJuice();
                            b.giveJuice();
                            b.setStress(0);
                            b.setSpeed(500);
                            b.setNonYukkuriDiseasePeriod(0);
                            revivedBodies++;
                        }
                    }
                    System.out.println(
                            "「全ゆ胎ゆ補充+蘇生」完了: 胎ゆ補充対象="
                                    + modifiedMothers
                                    + ", 追加胎ゆ数="
                                    + totalAddedBabies
                                    + ", 去勢スキップ="
                                    + skippedCastrated
                                    + ", 蘇生数="
                                    + revivedBodies);
                }
                break;
            case KeyEvent.VK_5: // 選択したゆっくりを発情させ、交尾をしたときに連続ですっきりさせる - "すすすすすすすすす、すっきりー！"
                synchronized (SimYukkuri.lock) {
                    for (Yukkuri b :
                            SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().values()) {
                        b.setSpeed(500);
                    }
                }
                break;
            case KeyEvent.VK_6: // 実ゆ強制誕生
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        for (Integer childId : selectedBody.getChildren()) {
                            Yukkuri child =
                                    org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .get(childId);
                            if (child.isUnBirth() && !selectedBody.hasBindStalk()) { // 実ゆ?
                                child.setUnBirth(false); // 未誕生フラグ解除
                                child.setBirth(false);
                                child.wakeup(); // 覚醒
                                System.out.println(
                                        "Selected Yukkuri: "
                                                + selectedBody.getNameJ()
                                                + " (ID:"
                                                + selectedBody.getUniqueId()
                                                + ")の実ゆを強制誕生させました。");
                            }
                        }
                    }
                }
                break;
            case KeyEvent.VK_7: // 特定種だけ全部「持ち物リスト」に移動
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null || selectedBody.isDead()) {
                        System.out.println("対象種を決めるため、ゆっくりを1体選択してください。");
                        break;
                    }
                    org.simyukkuri.enums.YukkuriType targetType = selectedBody.getType();
                    String targetName = selectedBody.getNameJ();
                    int movedCount = 0;
                    List<Yukkuri> bodies =
                            new ArrayList<Yukkuri>(
                                    SimYukkuri.world
                                            .getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .values());
                    for (Yukkuri b : bodies) {
                        if (b == null || b.isRemoved()) {
                            continue;
                        }
                        if (b.getType() == targetType) {
                            b.removeAllStalks();
                            b.setCanPullOrPush(false);
                            b.setLockmove(false);
                            b.setFixBack(false);
                            b.setTaken(true);
                            SimYukkuri.world.getPlayer().getInventoryView().addElement(b);
                            SimYukkuri.world
                                    .getCurrentWorldState()
                                    .getYukkuriRegistry()
                                    .remove(b.getUniqueId());
                            movedCount++;
                        }
                    }
                    MyPane.setSelectedYukkuri(null);
                    System.out.println("「特定種だけ全部持ち物移動」完了: 種=" + targetName + ", 移動数=" + movedCount);
                }
                break;
            case KeyEvent.VK_8: // ゲスバイバイ2（破裂させず削除）
                synchronized (SimYukkuri.lock) {
                    int removedCount = 0;
                    List<Yukkuri> bodies =
                            new ArrayList<Yukkuri>(
                                    SimYukkuri.world
                                            .getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .values());
                    for (Yukkuri b : bodies) {
                        if (b == null || b.isRemoved()) {
                            continue;
                        }
                        if (b.getAttitude() == Attitude.SHITHEAD
                                || b.getAttitude() == Attitude.SUPER_SHITHEAD) {
                            b.remove();
                            removedCount++;
                        }
                    }
                    System.out.println("「ゲスバイバイ2」完了: 削除数=" + removedCount);
                }
                break;

            // 機能キー (ファンクションキー)

            // ヘルプ表示
            case KeyEvent.VK_F1:
                synchronized (SimYukkuri.lock) {
                    String helpText =
                            "ショートカット一覧\n\n"
                                    + "[画面操作]\n"
                                    + "  Esc      : 一時停止 / 再開\n"
                                    + "  Delete   : 全ガジェット即時実行\n"
                                    + "  W A S D  : 画面スクロール\n"
                                    + "  Shift+ドラッグ : 画面スクロール\n"
                                    + "  Z / X    : ズームイン / ズームアウト\n"
                                    + "  C        : 表示位置とズームをリセット\n\n"
                                    + "[指定したゆっくりと遊ぶ]\n"
                                    + "  メニューの「かけっこ」を選んで相手をクリック\n"
                                    + "  地面の上でカーソルを動かすと追いかけます\n"
                                    + "  「遊ぶのをやめる」で終了（画面外では待機）\n\n"
                                    + "[選択中のゆっくり]\n"
                                    + "  H : 強制老ゆ化\n"
                                    + "  I : クローン作成\n"
                                    + "  K : 賢くする（超善良+賢い）\n"
                                    + "  L : 即死\n"
                                    + "  M : 堕胎\n"
                                    + "  O : 空腹度0\n"
                                    + "  P : 弔いイベントテスト\n"
                                    + "  R : 蘇生\n"
                                    + "  T : ドス/でいぶ化\n"
                                    + "  Y : NYD解除\n\n"
                                    + "[数字キー]\n"
                                    + "  0 : ゲスバイバイ（餓死）\n"
                                    + "  1 : ゆっくり花火\n"
                                    + "  2 : 往復ビンタ改（超善良まで繰り返し）\n"
                                    + "  4 : 全ゆをきれいに\n"
                                    + "  5 : 全ゆの速度を500に設定\n"
                                    + "  6 : 選択個体の実ゆ強制誕生\n"
                                    + "  7 : 選択種を全て持ち物リストへ移動\n"
                                    + "  8 : ゲスバイバイ2（即削除）\n\n"
                                    + "[全体/機能]\n"
                                    + "  U    : 全ゆ回復\n"
                                    + "  J    : 全ゆ胎ゆ補充 + 死体蘇生\n"
                                    + "  F3   : おとこのこにする\n"
                                    + "  F4   : おんなのこ(胎)にする\n"
                                    + "  F5   : おんなのこ(茎)にする\n"
                                    + "  F6   : 選択個体を強制誕生\n"
                                    + "  F7   : 持ち物を全て奪う\n"
                                    + "  F8   : パラメータ編集\n"
                                    + "  F9   : 所持金変更\n"
                                    + "  F10  : 胎ゆ最大補充\n"
                                    + "  F11  : 緊急帝王切開\n"
                                    + "  F12  : 胎ゆ/茎ゆDNA編集\n"
                                    + "  F1   : このヘルプ";

                    JOptionPane.showMessageDialog(
                            owner, helpText, SimYukkuri.TITLE, JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            // 「おとこのこ」に
            case KeyEvent.VK_F3:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.setStalkCastration(true);
                        selectedBody.setCastrated(true);
                        selectedBody.setPenipeniCutted(false);
                    }
                }
                break;
            // 「おんなのこ(胎ゆ)」に
            case KeyEvent.VK_F4:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.setStalkCastration(true);
                        selectedBody.setCastrated(false);
                        selectedBody.setPenipeniCutted(true);
                    }
                }
                break;
            // 「おんなのこ(茎ゆ)」に
            case KeyEvent.VK_F5:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        selectedBody.setCastrated(true);
                        selectedBody.setStalkCastration(false);
                        selectedBody.setPenipeniCutted(true);
                    }
                }
                break;
            // 強制誕生
            case KeyEvent.VK_F6:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null
                            && selectedBody.isUnBirth()
                            && !selectedBody.hasBindStalk()) {
                        selectedBody.setUnBirth(false);
                        selectedBody.setBirth(false);
                        selectedBody.wakeup();
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") を強制的に誕生させました。");
                    } else if (selectedBody != null
                            && selectedBody.isUnBirth()
                            && selectedBody.hasBindStalk()) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") はまだ茎についているため、誕生させられません。先に茎から外してください。");
                    } else if (selectedBody != null) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") は実ゆではありません。");
                    }
                }
                break;
            // 持ち物強奪
            case KeyEvent.VK_F7:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null && !selectedBody.isDead()) {
                        if (!selectedBody.getCarryItems().isEmpty()) {
                            selectedBody.dropAllTakeoutItem();
                            selectedBody.setHappiness(Happiness.VERY_SAD);
                            selectedBody.setForceFace(ImageCode.CRYING.ordinal());
                            selectedBody.addStress(300);
                            selectedBody.addLovePlayer(-200);
                            selectedBody.setMessage(
                                    GameMessages.getMessage(
                                            selectedBody,
                                            org.simyukkuri.system.MessagePool.Action.Alarm),
                                    true);
                            System.out.println(
                                    "Selected Yukkuri: "
                                            + selectedBody.getNameJ()
                                            + " (ID:"
                                            + selectedBody.getUniqueId()
                                            + ") から持ち物を奪いました。");
                        } else {
                            System.out.println(
                                    "Selected Yukkuri: "
                                            + selectedBody.getNameJ()
                                            + " (ID:"
                                            + selectedBody.getUniqueId()
                                            + ") は何も持っていません。");
                        }
                    }
                }
                break;
            // パラメータ編集
            case KeyEvent.VK_F8:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody != null) {
                        new CharacterEditorDialog(owner, selectedBody).setVisible(true);
                    } else {
                        System.out.println("パラメータを編集するゆっくりが選択されていません。");
                    }
                }
                break;
            // 所持金変更
            case KeyEvent.VK_F9:
                synchronized (SimYukkuri.lock) {
                    String input =
                            JOptionPane.showInputDialog(
                                    owner,
                                    "新しい所持金を入力してください",
                                    SimYukkuri.world.getPlayer().getCash());
                    if (input != null) {
                        try {
                            int newCash = Integer.parseInt(input);

                            SimYukkuri.world.getPlayer().setCash(newCash);
                            MainCommandUi.showPlayerStatus(); // UIを更新
                            System.out.println("所持金を " + newCash + " に変更しました。");

                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(
                                    owner, "数値で入力してください。", "エラー", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                break;
            // 胎ゆ最大補充
            case KeyEvent.VK_F10:
                synchronized (SimYukkuri.lock) {
                    Yukkuri mother = MyPane.getSelectedYukkuri();
                    if (mother == null) {
                        System.out.println("ゆっくりが選択されていません。");
                        break;
                    }

                    if (mother.isDead()) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + mother.getNameJ()
                                        + " (ID:"
                                        + mother.getUniqueId()
                                        + ") は死んでいます。");
                        break;
                    }

                    if (mother.isCastrated()) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + mother.getNameJ()
                                        + " (ID:"
                                        + mother.getUniqueId()
                                        + ") は去勢されているため妊娠できません。");
                        break;
                    }

                    if (mother.getBurstState() == Burst.NEAR) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + mother.getNameJ()
                                        + " (ID:"
                                        + mother.getUniqueId()
                                        + ") は既に破裂寸前のため、胎ゆを補充できません。");
                        break;
                    }

                    int maxBabies = mother.getMaxHaveBaby();
                    int safeMaxBabies = Math.max(0, maxBabies - 1);
                    int currentBabies = mother.getBabyTypes().size();
                    int numToAdd = safeMaxBabies - currentBabies;
                    int i;

                    if (numToAdd > 0) {
                        for (i = 0; i < numToAdd; i++) {
                            Dna babyDna =
                                    new Dna(
                                            mother.getType(),
                                            Attitude.AVERAGE,
                                            Intelligence.AVERAGE,
                                            false);
                            babyDna.setMother(mother.getUniqueId());

                            Yukkuri partner =
                                    org.simyukkuri.util.GameWorld.getCurrentWorldState()
                                            .getYukkuriRegistry()
                                            .get(mother.getPartner());
                            if (partner != null) {
                                babyDna.setFather(partner.getUniqueId());
                            } else {
                                babyDna.setFather(-1);
                            }
                            if (!mother.isAboutToBurst()) {
                                mother.getBabyTypes().add(babyDna);
                            } else {

                                break;
                            }
                        }

                        mother.setHasBaby(true);

                        if (mother.getPregnantPeriod() == 0) {
                            mother.setPregnantPeriod(1);
                        }

                        System.out.println(
                                "Selected Yukkuri: "
                                        + mother.getNameJ()
                                        + " (ID:"
                                        + mother.getUniqueId()
                                        + ") の胎内を赤ゆで満たしました。 ("
                                        + i
                                        + "匹追加)");
                    } else {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + mother.getNameJ()
                                        + " (ID:"
                                        + mother.getUniqueId()
                                        + ") の胎内は既に満杯か、それに近いです。");
                    }
                }
                break;
            // 緊急帝王切開
            case KeyEvent.VK_F11:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null) {
                        System.out.println("ゆっくりが選択されていません。");
                        break;
                    }

                    if (selectedBody.isHasBaby() && selectedBody.getBurstState() == Burst.NEAR) {
                        boolean success = EmergencyBirth.perform(selectedBody);
                        if (success) {
                            System.out.println(
                                    "Selected Yukkuri: "
                                            + selectedBody.getNameJ()
                                            + " (ID:"
                                            + selectedBody.getUniqueId()
                                            + ") の緊急帝王切開に成功しました。");
                        } else {
                            System.out.println(
                                    "Selected Yukkuri: "
                                            + selectedBody.getNameJ()
                                            + " (ID:"
                                            + selectedBody.getUniqueId()
                                            + ") の緊急帝王切開に失敗しました。");
                        }
                    } else {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") は緊急帝王切開の対象ではありません。（妊娠中かつ破裂寸前である必要があります）");
                    }
                }
                break;
            // DNA編集
            case KeyEvent.VK_F12:
                synchronized (SimYukkuri.lock) {
                    Yukkuri selectedBody = MyPane.getSelectedYukkuri();
                    if (selectedBody == null) {
                        System.out.println("ゆっくりが選択されていません。");
                        break;
                    }
                    if (!selectedBody.isHasBaby() && !selectedBody.isHasStalk()) {
                        System.out.println(
                                "Selected Yukkuri: "
                                        + selectedBody.getNameJ()
                                        + " (ID:"
                                        + selectedBody.getUniqueId()
                                        + ") は妊娠していません。");
                        break;
                    }
                    DnaEditorDialog editorDialog = new DnaEditorDialog(owner, selectedBody);
                    editorDialog.setVisible(true);
                }
                break;
            default:
                break;
        }
    }

    /** キー解放を受け取ります。 */
    public void handleKeyReleased(KeyEvent e) {}

    /** 文字入力を受け取ります。 */
    public void handleKeyTyped(KeyEvent e) {}
}
