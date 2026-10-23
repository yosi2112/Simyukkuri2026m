package org.simyukkuri.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.entity.core.living.yukkuri.Dna;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.entity.core.world.bodylinked.Stalk;
import org.simyukkuri.enums.Attitude;
import org.simyukkuri.enums.Intelligence;

/** 胎ゆ・茎ゆのDNAを編集するためのGUIダイアログ */
public class DnaEditorDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private Yukkuri parentBody;
    // MODIFIED: Dnaのコピーを保持するリスト
    private List<Dna> babyDnaCopies;
    private List<Dna> stalkDnaCopies;
    private final Map<Integer, Yukkuri> stalkTargets = new HashMap<>();

    private JComboBox<String> typeCombo;
    private JComboBox<Attitude> attitudeCombo;
    private JComboBox<Intelligence> intelligenceCombo;
    private JCheckBox raperChildCheck;
    private JSpinner fatherSpinner;
    private JSpinner motherSpinner;

    private JPanel editorPanel;
    private JList<String> babyList;
    private JList<String> stalkList;

    // MODIFIED: JListのインデックスとDnaのコピーをマッピングする
    private Map<Integer, Dna> babyDnaMap = new HashMap<>();
    private Map<Integer, Dna> stalkDnaMap = new HashMap<>();

    /** 胎内と茎にいる子のDNA編集画面を構築します。 */
    public DnaEditorDialog(JFrame owner, Yukkuri parentBody) {
        super(owner, "胎内/茎内 DNAエディタ (" + parentBody.getNameJ() + ")", true);
        this.parentBody = parentBody;

        // --- MODIFIED: DNAリストの取得方法を修正 ---

        // 胎ゆのDNAリストをコピー
        this.babyDnaCopies = new ArrayList<>();
        if (parentBody.getBabyTypes() != null) {
            for (Dna original : parentBody.getBabyTypes()) {
                Dna copy = copyDna(original);
                this.babyDnaCopies.add(copy);
            }
        }

        // 茎ゆのDNAリストをStalkオブジェクトから取得してコピー
        this.stalkDnaCopies = new ArrayList<>();
        if (parentBody.getStalks() != null) {
            for (Stalk stalk : parentBody.getStalks()) { // 親ゆの茎取得
                if (stalk.getAttachedBabyIds() == null) {
                    continue;
                } // getAttachedBabyIds()がnullを返す可能性も考慮
                for (int i : stalk.getAttachedBabyIds()) { // 実ゆのIDリスト取得
                    Yukkuri b =
                            org.simyukkuri.util.YukkuriLookup.getYukkuriById(i);
                    if (b != null) {
                        Dna original = b.getDna();
                        if (original != null) {
                            Dna copy = copyDna(original);
                            copy.setFather(b.getFather());
                            copy.setMother(b.getMother());
                            copy.setRaperChild(b.isFatherRaper());
                            this.stalkTargets.put(this.stalkDnaCopies.size(), b);
                            this.stalkDnaCopies.add(copy);
                        }
                    }
                }
            }
        }
        // --- 修正ここまで ---

        initComponents();
        pack();
        setLocationRelativeTo(owner);
    }

    // Dnaオブジェクトを安全にコピーするためのヘルパーメソッド
    private Dna copyDna(Dna original) {
        if (original == null) {
            return null;
        }
        Dna copy =
                new Dna(
                        original.getType(),
                        original.getAttitude(),
                        original.getIntelligence(),
                        original.isRaperChild());
        copy.setFather(original.getFather());
        copy.setMother(original.getMother());
        return copy;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        // MODIFIED: コピーされたリストを使用
        tabbedPane.addTab(
                "胎ゆ (" + babyDnaCopies.size() + ")", createBabyPanel(babyDnaCopies, "胎ゆ"));
        tabbedPane.addTab(
                "茎ゆ (" + stalkDnaCopies.size() + ")", createBabyPanel(stalkDnaCopies, "茎ゆ"));

        editorPanel = createEditorPanel();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("変更を保存");
        JButton cancelButton = new JButton("キャンセル");

        saveButton.addActionListener(e -> saveChanges());
        cancelButton.addActionListener(e -> setVisible(false));

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(tabbedPane, BorderLayout.CENTER);
        add(editorPanel, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);

        // 最初の要素を選択状態にする
        if (!babyDnaCopies.isEmpty()) {
            babyList.setSelectedIndex(0);
        } else if (!stalkDnaCopies.isEmpty()) {
            tabbedPane.setSelectedIndex(1);
            stalkList.setSelectedIndex(0);
        } else {
            updateEditorPanel(null);
        }
    }

    private JPanel createBabyPanel(List<Dna> dnaList, String prefix) {
        final JPanel panel = new JPanel(new BorderLayout());
        javax.swing.DefaultListModel<String> listModel = new javax.swing.DefaultListModel<>();
        Map<Integer, Dna> dnaMap = (prefix.equals("胎ゆ")) ? babyDnaMap : stalkDnaMap;

        for (int i = 0; i < dnaList.size(); i++) {
            Dna dna = dnaList.get(i);
            listModel.addElement(
                    String.format(
                            "%s #%d (%s)",
                            prefix, i + 1, DnaEditor.getYukkuriNameByType(dna.getType())));
            dnaMap.put(i, dna);
        }

        JList<String> list = new JList<>(listModel);
        list.addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        JList<?> sourceList = (JList<?>) e.getSource();
                        int selectedIndex = sourceList.getSelectedIndex();
                        if (selectedIndex != -1) {
                            // 他のリストの選択を解除
                            if (sourceList == babyList && stalkList != null) {
                                stalkList.clearSelection();
                            }
                            if (sourceList == stalkList && babyList != null) {
                                babyList.clearSelection();
                            }

                            updateEditorPanel(dnaMap.get(selectedIndex));
                        }
                    }
                });

        if (prefix.equals("胎ゆ")) {
            this.babyList = list;
        } else {
            this.stalkList = list;
        }

        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(200, 300));
        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("DNA情報"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("種別:"), gbc);
        typeCombo = new JComboBox<>(DnaEditor.getAllYukkuriNames());
        gbc.gridx = 1;
        panel.add(typeCombo, gbc);

        // Attitude
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("性格:"), gbc);
        attitudeCombo = new JComboBox<>(Attitude.values());
        gbc.gridx = 1;
        panel.add(attitudeCombo, gbc);

        // Intelligence
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("知能:"), gbc);
        intelligenceCombo = new JComboBox<>(Intelligence.values());
        gbc.gridx = 1;
        panel.add(intelligenceCombo, gbc);

        // Raper Child
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        raperChildCheck = new JCheckBox("レイプでできた子");
        panel.add(raperChildCheck, gbc);
        gbc.gridwidth = 1;

        // Father
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("父ゆ ID:"), gbc);
        fatherSpinner = new JSpinner(new SpinnerNumberModel(0, -1, 99999, 1));
        gbc.gridx = 1;
        panel.add(fatherSpinner, gbc);

        // Mother
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(new JLabel("母ゆ ID:"), gbc);
        motherSpinner = new JSpinner(new SpinnerNumberModel(0, -1, 99999, 1));
        gbc.gridx = 1;
        panel.add(motherSpinner, gbc);

        panel.setPreferredSize(new Dimension(300, 300));
        return panel;
    }

    private void updateEditorPanel(Dna dna) {
        if (dna == null) {
            editorPanel.setEnabled(false);
            for (java.awt.Component c : editorPanel.getComponents()) {
                c.setEnabled(false);
            }
            return;
        }

        if (!editorPanel.isEnabled()) {
            editorPanel.setEnabled(true);
            for (java.awt.Component c : editorPanel.getComponents()) {
                c.setEnabled(true);
            }
        }

        typeCombo.setSelectedItem(DnaEditor.getYukkuriNameByType(dna.getType()));
        attitudeCombo.setSelectedItem(dna.getAttitude());
        intelligenceCombo.setSelectedItem(dna.getIntelligence());
        raperChildCheck.setSelected(dna.isRaperChild());
        fatherSpinner.setValue(dna.getFather());
        motherSpinner.setValue(dna.getMother());
    }

    private void saveChanges() {
        Dna selectedDnaCopy = getSelectedDna();
        if (selectedDnaCopy == null) {
            return;
        }

        // GUIから値を取得して、選択中のDna「コピー」オブジェクトを更新
        selectedDnaCopy.setType(
                DnaEditor.getYukkuriTypeByName((String) typeCombo.getSelectedItem()));
        selectedDnaCopy.setAttitude((Attitude) attitudeCombo.getSelectedItem());
        selectedDnaCopy.setIntelligence((Intelligence) intelligenceCombo.getSelectedItem());
        selectedDnaCopy.setRaperChild(raperChildCheck.isSelected());
        selectedDnaCopy.setFather((Integer) fatherSpinner.getValue());
        selectedDnaCopy.setMother((Integer) motherSpinner.getValue());

        // 編集リストのインデックスを、対応する実際の個体へマッピングします。
        synchronized (SimYukkuri.lock) {
            // 胎ゆのリストを更新
            parentBody.getBabyTypes().clear();
            parentBody.getBabyTypes().addAll(this.babyDnaCopies);

            // 茎ゆのリストを更新
            for (Map.Entry<Integer, Yukkuri> entry : stalkTargets.entrySet()) {
                Yukkuri child = entry.getValue();
                if (org.simyukkuri.util.YukkuriLookup.getYukkuriById(child.getUniqueId()) != child) {
                    continue;
                }
                Dna editedDna = stalkDnaCopies.get(entry.getKey());
                if (child.getType() != editedDna.getType()) {
                    int childId = child.getUniqueId();
                    org.simyukkuri.engine.transform.TransformationService.transform(child, editedDna.getType());
                    child = org.simyukkuri.util.YukkuriLookup.getYukkuriById(childId);
                    entry.setValue(child);
                }
                child.setAttitude(editedDna.getAttitude());
                child.setIntelligence(editedDna.getIntelligence());
                child.setFatherRaper(editedDna.isRaperChild());
                int[] parents = child.getParents().clone();
                parents[org.simyukkuri.enums.Parent.PAPA.ordinal()] = editedDna.getFather();
                parents[org.simyukkuri.enums.Parent.MAMA.ordinal()] = editedDna.getMother();
                child.setParents(parents);
            }
        }
        // --- 修正ここまで ---

        System.out.println(parentBody.getNameJ() + " の胎内/茎内DNA情報を更新しました。");
        setVisible(false);
    }

    private Dna getSelectedDna() {
        int babyIndex = babyList.getSelectedIndex();
        int stalkIndex = stalkList.getSelectedIndex();

        if (babyIndex != -1) {
            return babyDnaMap.get(babyIndex);
        } else if (stalkIndex != -1) {
            return stalkDnaMap.get(stalkIndex);
        }
        return null;
    }
}
