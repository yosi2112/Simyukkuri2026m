package org.simyukkuri.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.Attitude;
import org.simyukkuri.enums.Intelligence;
import org.simyukkuri.enums.YukkuriRank;

/** ゆっくりのパラメータを直接編集するためのGUIダイアログ */
public class CharacterEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private Yukkuri targetBody;

    private JComboBox<YukkuriRank> rankCombo;
    private JComboBox<Attitude> attitudeCombo;
    private JComboBox<Intelligence> intelligenceCombo;
    private JSpinner damageSpinner;
    private JSpinner stressSpinner;
    private JSpinner hungrySpinner;
    private JSpinner shitSpinner;
    private JSpinner lovePlayerSpinner;

    /** 選択個体のパラメーター編集画面を構築します。 */
    public CharacterEditorDialog(JFrame owner, Yukkuri body) {
        super(owner, "パラメータ編集 (" + body.getNameJ() + ")", true);
        this.targetBody = body;

        initComponents();
        loadParameters();

        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        final JPanel editorPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- コンポーネントの初期化 ---
        rankCombo = new JComboBox<>(YukkuriRank.values());
        attitudeCombo = new JComboBox<>(Attitude.values());
        intelligenceCombo = new JComboBox<>(Intelligence.values());
        damageSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        stressSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        hungrySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        shitSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000000, 100));
        lovePlayerSpinner = new JSpinner(new SpinnerNumberModel(0, -10000, 10000, 100));

        // --- レイアウト ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        editorPanel.add(new JLabel("ランク:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(rankCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        editorPanel.add(new JLabel("性格:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(attitudeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        editorPanel.add(new JLabel("知能:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(intelligenceCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        editorPanel.add(new JLabel("ダメージ:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(damageSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        editorPanel.add(new JLabel("ストレス:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(stressSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        editorPanel.add(new JLabel("満腹度:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(hungrySpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        editorPanel.add(new JLabel("うんうん:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(shitSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        editorPanel.add(new JLabel("なつき度:"), gbc);
        gbc.gridx = 1;
        editorPanel.add(lovePlayerSpinner, gbc);

        // --- ボタンパネル ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        JButton cancelButton = new JButton("キャンセル");

        saveButton.addActionListener(e -> saveParameters());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(editorPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadParameters() {
        rankCombo.setSelectedItem(targetBody.getRank());
        attitudeCombo.setSelectedItem(targetBody.getAttitude());
        intelligenceCombo.setSelectedItem(targetBody.getIntelligence());
        damageSpinner.setValue(targetBody.getDamage());
        stressSpinner.setValue(targetBody.getStress());
        hungrySpinner.setValue(targetBody.getHungry());
        shitSpinner.setValue(targetBody.getShit());
        lovePlayerSpinner.setValue(targetBody.getLovePlayer());
    }

    private void saveParameters() {
        synchronized (SimYukkuri.lock) {
            targetBody.setRank((YukkuriRank) rankCombo.getSelectedItem());
            targetBody.setAttitude((Attitude) attitudeCombo.getSelectedItem());
            targetBody.setIntelligence((Intelligence) intelligenceCombo.getSelectedItem());
            targetBody.setDamage((Integer) damageSpinner.getValue());
            targetBody.setStress((Integer) stressSpinner.getValue());
            targetBody.setHungry((Integer) hungrySpinner.getValue());
            targetBody.setShit((Integer) shitSpinner.getValue());
            targetBody.setLovePlayer((Integer) lovePlayerSpinner.getValue());

            // UIのステータス表示を即時更新
            MainCommandUi.showStatus(targetBody);
        }
        dispose();
    }
}
