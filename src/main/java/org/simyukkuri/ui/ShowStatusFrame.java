package org.simyukkuri.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.draw.Dimension4y;
import org.simyukkuri.draw.MyPane;
import org.simyukkuri.draw.Point4y;
import org.simyukkuri.draw.Translate;
import org.simyukkuri.entity.core.Entity;
import org.simyukkuri.entity.core.attachment.Attachment;
import org.simyukkuri.entity.core.living.yukkuri.Dna;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.entity.core.world.bodylinked.Stalk;
import org.simyukkuri.enums.AgeState;
import org.simyukkuri.enums.CoreAnkoState;
import org.simyukkuri.enums.FavItemType;
import org.simyukkuri.enums.Pain;
import org.simyukkuri.enums.TakeoutItemType;
import org.simyukkuri.enums.YukkuriType;
import org.simyukkuri.system.Sprite;
import org.simyukkuri.util.GameText;
import org.simyukkuri.util.YukkuriUtil;

/** ゆっくりのステータスをカード型GUIで表示するFrame. */
public class ShowStatusFrame extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = -5118988467314035506L;
    private static final Color BG = new Color(242, 245, 248);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(202, 211, 222);
    private static final Color TEXT = new Color(38, 45, 55);
    private static final Color MUTED = new Color(102, 113, 128);
    private static final Color ACCENT = new Color(63, 124, 214);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private static final Font SECTION_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font BODY_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    private final JPanel contentPane;
    private final FacePanel facePanel;
    private final JTextField idField;
    private final JLabel nameLabel;
    private final JLabel subLabel;
    private final JLabel statusLabel;
    private final Map<String, JLabel> values = new LinkedHashMap<>();
    private final JTextArea relationArea = new JTextArea();
    private final JTextArea stateArea = new JTextArea();
    private final JTabbedPane childTabs = new JTabbedPane();
    private final JPanel familyStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final JProgressBar hpBar = createBar(new Color(88, 177, 105));
    private final JProgressBar hungryBar = createBar(new Color(237, 168, 58));
    private final JProgressBar shitBar = createBar(new Color(147, 104, 75));
    private final JProgressBar stressBar = createBar(new Color(214, 83, 83));
    private Yukkuri currentBody;

    private static final ShowStatusFrame instance = new ShowStatusFrame();

    private ShowStatusFrame() {
        setTitle(GameText.read("command_status_title"));
        setBounds(100, 100, 860, 560);
        setMinimumSize(new Dimension(760, 500));
        addWindowListener(this);

        contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBackground(BG);
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(contentPane);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 0));
        mainPanel.setOpaque(false);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        JPanel leftPanel = card(new BorderLayout(12, 10));
        leftPanel.setPreferredSize(new Dimension(350, 10));
        mainPanel.add(leftPanel, BorderLayout.WEST);

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        facePanel = new FacePanel();
        header.add(facePanel, BorderLayout.WEST);

        JPanel titlePanel = new JPanel(new BorderLayout(4, 4));
        titlePanel.setOpaque(false);
        nameLabel = new JLabel("-");
        nameLabel.setFont(TITLE_FONT);
        nameLabel.setForeground(TEXT);
        subLabel = new JLabel("-");
        subLabel.setFont(BODY_FONT);
        subLabel.setForeground(MUTED);
        statusLabel = new JLabel("-");
        statusLabel.setFont(BODY_FONT);
        statusLabel.setForeground(ACCENT);
        titlePanel.add(nameLabel, BorderLayout.NORTH);
        titlePanel.add(subLabel, BorderLayout.CENTER);
        titlePanel.add(statusLabel, BorderLayout.SOUTH);
        header.add(titlePanel, BorderLayout.CENTER);

        JPanel idPanel = new JPanel(new BorderLayout(6, 6));
        idPanel.setOpaque(false);
        idField = new JTextField(7);
        JButton reload = button(GameText.read("command_status_new"));
        reload.addActionListener(e -> showById());
        idPanel.add(new JLabel(GameText.read("command_status_id")), BorderLayout.NORTH);
        idPanel.add(idField, BorderLayout.CENTER);
        idPanel.add(reload, BorderLayout.SOUTH);
        header.add(idPanel, BorderLayout.EAST);
        leftPanel.add(header, BorderLayout.NORTH);

        leftPanel.add(overviewPanel(), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(GameText.read("command_status_purpose"), detailPanel());
        tabs.addTab(GameText.read("command_status_child"), relationPanel());
        tabs.addTab(GameText.read("command_status_equip"), statePanel());
        JPanel rightPanel = new JPanel(new BorderLayout(12, 0));
        rightPanel.setOpaque(false);
        JPanel rightCore = new JPanel(new BorderLayout(0, 8));
        rightCore.setOpaque(false);
        familyStrip.setOpaque(false);
        rightCore.add(familyStrip, BorderLayout.NORTH);
        rightCore.add(childTabs, BorderLayout.CENTER);
        rightPanel.add(rightCore, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel overviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        JPanel bars = card(new GridBagLayout());
        addBar(bars, 0, GameText.read("command_status_hp"), hpBar);
        addBar(bars, 1, GameText.read("command_status_stomach"), hungryBar);
        addBar(bars, 2, "うんうん", shitBar);
        addBar(bars, 3, "ストレス", stressBar);
        panel.add(bars, BorderLayout.NORTH);

        JPanel grid = card(new GridBagLayout());
        addValue(grid, "age", GameText.read("command_status_age"), 0, 0);
        addValue(grid, "naming", GameText.read("command_status_naming"), 1, 0);
        addValue(grid, "rank", "Rank", 0, 1);
        addValue(grid, "attitude", GameText.read("command_attitudepoint"), 1, 1);
        addValue(grid, "nyd", GameText.read("command_status_nyd"), 0, 2);
        addValue(grid, "canMove", GameText.read("command_status_canmove"), 1, 2);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel detailPanel() {
        JPanel grid = card(new GridBagLayout());
        addValue(grid, "purpose", GameText.read("command_status_purpose"), 0, 0);
        addValue(grid, "target", GameText.read("command_status_purposecoordinate"), 1, 0);
        addValue(grid, "event", GameText.read("command_status_event"), 0, 1);
        addValue(grid, "mold", GameText.read("command_status_moldlevel"), 1, 1);
        addValue(grid, "weight", GameText.read("command_status_weight"), 0, 2);
        addValue(grid, "bodyAmount", "うんうん", 1, 2);
        addValue(grid, "footBake", GameText.read("command_status_footbakelevel"), 0, 3);
        addValue(grid, "fav", GameText.read("command_status_favs"), 1, 3);
        addValue(grid, "ants", GameText.read("command_status_ants"), 0, 4);
        addValue(grid, "blocked", GameText.read("command_status_blockcount"), 1, 4);
        addValue(grid, "takeout", GameText.read("command_status_conveyitem"), 0, 5);
        return grid;
    }

    private JPanel relationPanel() {
        final JPanel panel = card(new BorderLayout(8, 8));
        relationArea.setEditable(false);
        relationArea.setLineWrap(true);
        relationArea.setWrapStyleWord(true);
        relationArea.setFont(BODY_FONT);
        panel.add(new JScrollPane(relationArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel statePanel() {
        final JPanel panel = card(new BorderLayout(8, 8));
        stateArea.setEditable(false);
        stateArea.setLineWrap(true);
        stateArea.setWrapStyleWord(true);
        stateArea.setFont(BODY_FONT);
        panel.add(new JScrollPane(stateArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel navigationPanel() {
        JPanel panel = card(new GridBagLayout());
        panel.setPreferredSize(new Dimension(146, 10));
        String[] labels = {
            GameText.read("command_status_first"),
            GameText.read("command_status_beforeid"),
            GameText.read("command_status_nextid"),
            GameText.read("command_status_last"),
            GameText.read("command_status_random"),
            GameText.read("command_status_partner"),
            GameText.read("command_status_child"),
            GameText.read("command_status_eldersister"),
            GameText.read("command_status_littlesister"),
            GameText.read("command_status_father"),
            GameText.read("command_status_mother")
        };
        Runnable[] actions = {
            this::showFirst,
            this::showPrevious,
            this::showNext,
            this::showLast,
            this::showRandom,
            this::showPartner,
            this::showChild,
            this::showElderSister,
            this::showLittleSister,
            this::showFather,
            this::showMother
        };
        for (int i = 0; i < labels.length; i++) {
            JButton btn = button(labels[i]);
            final Runnable action = actions[i];
            btn.addActionListener(e -> action.run());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 6, 0);
            panel.add(btn, gbc);
        }
        return panel;
    }

    private void addBar(JPanel panel, int row, String label, JProgressBar bar) {
        JLabel l = new JLabel(label);
        l.setForeground(TEXT);
        l.setFont(BODY_FONT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 4, 5, 10);
        panel.add(l, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 4);
        panel.add(bar, gbc);
    }

    private void addValue(JPanel panel, String key, String label, int col, int row) {
        JPanel item = new JPanel(new BorderLayout(4, 3));
        item.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(SECTION_FONT);
        name.setForeground(MUTED);
        JLabel value = new JLabel("-");
        value.setFont(BODY_FONT);
        value.setForeground(TEXT);
        item.add(name, BorderLayout.NORTH);
        item.add(value, BorderLayout.CENTER);
        values.put(key, value);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);
        panel.add(item, gbc);
    }

    /**
     * ゆっくりの情報を表示する.
     *
     * @param b ゆっくり
     */
    public void giveYukkuriInfo(Yukkuri b) {
        if (b == null) {
            return;
        }
        currentBody = b;
        idField.setText(String.valueOf(b.getUniqueId()));
        nameLabel.setText(b.toString());
        subLabel.setText(
                (org.simyukkuri.util.GameLocale.isJapanese() ? b.getNameJ() : b.getNameE())
                        + " / "
                        + b.getAgeState());
        statusLabel.setText(stateSummary(b));
        facePanel.setBody(b);

        values.get("age").setText(String.valueOf(b.getAge()));
        values.get("naming").setText(b.getMyName());
        values.get("rank")
                .setText(String.valueOf(b.getRank()) + " / " + String.valueOf(b.getPublicRank()));
        values.get("attitude").setText(String.valueOf(b.getAttitudePoint()));
        values.get("nyd").setText(String.valueOf(b.getNonYukkuriDiseaseTolerance()));
        values.get("canMove")
                .setText(
                        b.isLockmove() || b.isDead()
                                ? GameText.read("command_status_cantmove")
                                : GameText.read("command_status_canmove"));
        values.get("purpose").setText(purposeText(b));
        values.get("target").setText(moveTargetText(b));
        values.get("event")
                .setText(
                        b.getCurrentEvent() == null
                                ? GameText.read("command_status_nothing")
                                : b.getCurrentEvent().toString());
        values.get("mold").setText(String.valueOf(b.getSickPeriod()));
        values.get("weight").setText(String.valueOf(b.getWeight()));
        values.get("bodyAmount").setText(b.getShit() + " / " + b.getShitLimit());
        values.get("footBake").setText(footBakeText(b));
        values.get("fav").setText(favText(b));
        values.get("ants").setText(String.valueOf(b.getAntCount()));
        values.get("blocked").setText(String.valueOf(b.getBlockedTicks()));
        values.get("takeout").setText(takeoutText(b));

        setBar(hpBar, b.getDamageLimit() - b.getDamage(), b.getDamageLimit());
        setBar(hungryBar, b.getHungry(), b.getHungryLimit());
        setBar(shitBar, b.getShit(), b.getShitLimit());
        setBar(stressBar, b.getStress(), b.getStressLimit());
        relationArea.setText(relationText(b));
        stateArea.setText(stateText(b));
        rebuildFamilyStrip(b);
        rebuildChildTabs(b);
        MyPane.setSelectedYukkuri(b);
    }

    private void rebuildFamilyStrip(Yukkuri b) {
        familyStrip.removeAll();
        familyStrip.add(new JLabel("親"));
        addFamilyIcon(org.simyukkuri.util.YukkuriLookup.getYukkuriById(b.getFather()));
        addFamilyIcon(org.simyukkuri.util.YukkuriLookup.getYukkuriById(b.getMother()));
        familyStrip.add(new JLabel("兄弟"));
        for (int id : b.getElderSisters()) {
            addFamilyIcon(org.simyukkuri.util.YukkuriLookup.getYukkuriById(id));
        }
        for (int id : b.getSisters()) {
            addFamilyIcon(org.simyukkuri.util.YukkuriLookup.getYukkuriById(id));
        }
        familyStrip.revalidate();
        familyStrip.repaint();
    }

    private void addFamilyIcon(Yukkuri body) {
        MiniBodyButton button = new MiniBodyButton(body);
        button.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (body != null) {
                            giveYukkuriInfo(body);
                        }
                    }
                });
        familyStrip.add(button);
    }

    private void rebuildChildTabs(Yukkuri b) {
        childTabs.removeAll();
        boolean added = false;
        for (int id : b.getChildren()) {
            Yukkuri child = org.simyukkuri.util.YukkuriLookup.getYukkuriById(id);
            if (child != null) {
                childTabs.addTab("#" + child.getUniqueId(), childPanel(child));
                added = true;
            }
        }
        if (b.getBabyTypes() != null && b.getBabyTypes().size() > 0) {
            childTabs.addTab("おなか", unbornPanel(b, true));
            added = true;
        }
        if (hasStalkChildren(b)) {
            childTabs.addTab("茎", unbornPanel(b, false));
            added = true;
        }
        if (!added) {
            JTextArea empty = new JTextArea("子供はいません。");
            empty.setEditable(false);
            empty.setFont(BODY_FONT);
            childTabs.addTab(GameText.read("command_status_child"), new JScrollPane(empty));
        }
    }

    private JPanel childPanel(Yukkuri child) {
        final JPanel panel = card(new BorderLayout(10, 8));
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFont(BODY_FONT);
        text.setText(
                "ID: "
                        + child.getUniqueId()
                        + "\n"
                        + "名前: "
                        + child.toString()
                        + "\n"
                        + "種類: "
                        + (org.simyukkuri.util.GameLocale.isJapanese()
                                ? child.getNameJ()
                                : child.getNameE())
                        + "\n"
                        + "年齢: "
                        + child.getAge()
                        + " / "
                        + child.getAgeState()
                        + "\n"
                        + "HP: "
                        + (child.getDamageLimit() - child.getDamage())
                        + " / "
                        + child.getDamageLimit()
                        + "\n"
                        + "満腹: "
                        + child.getHungry()
                        + " / "
                        + child.getHungryLimit()
                        + "\n"
                        + "うんうん: "
                        + child.getShit()
                        + " / "
                        + child.getShitLimit()
                        + "\n"
                        + "状態: "
                        + stateSummary(child));
        panel.add(new JScrollPane(text), BorderLayout.CENTER);
        BodyImagePanel image = new BodyImagePanel(child, 150);
        image.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (child.isUnBirth()) {
                            showPregnancyDialog(currentBody, child.getBindStalk() == null);
                        } else {
                            giveYukkuriInfo(child);
                        }
                    }
                });
        panel.add(image, BorderLayout.EAST);
        return panel;
    }

    private JPanel unbornPanel(Yukkuri parent, boolean womb) {
        final JPanel panel = card(new BorderLayout(10, 8));
        final List<Dna> list = womb ? parent.getBabyTypes() : getStalkDnas(parent);
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFont(BODY_FONT);
        StringBuilder sb = new StringBuilder();
        sb.append(womb ? "おなかの赤ゆ" : "茎の実ゆ").append("\n");
        for (int i = 0; i < list.size(); i++) {
            Dna dna = list.get(i);
            sb.append(i + 1).append(": ").append(dnaName(dna)).append("\n");
        }
        if (list.isEmpty()) {
            sb.append("なし\n");
        }
        text.setText(sb.toString());
        panel.add(new JScrollPane(text), BorderLayout.CENTER);
        BodyImagePanel image = new BodyImagePanel(createPreviewBody(firstDna(list)), 150);
        image.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        showPregnancyDialog(parent, womb);
                    }
                });
        panel.add(image, BorderLayout.EAST);
        return panel;
    }

    private void showPregnancyDialog(Yukkuri parent, boolean womb) {
        List<Dna> babies = womb ? parent.getBabyTypes() : getStalkDnas(parent);
        final JDialog dialog = new JDialog(this, womb ? "おなかの赤ゆ" : "茎の実ゆ", false);
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(BG);
        root.add(pregnancyCard(babies, womb), BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.setSize(300, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel pregnancyCard(List<Dna> babies, boolean womb) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(80, 88, 98), 2));
        JPanel drawing = new PregnancyDrawingPanel(babies, womb);
        panel.add(drawing, BorderLayout.CENTER);
        return panel;
    }

    private boolean hasStalkChildren(Yukkuri b) {
        return !getStalkDnas(b).isEmpty() || (b.getStalks() != null && b.getStalks().size() > 0);
    }

    private List<Dna> getStalkDnas(Yukkuri b) {
        List<Dna> ret = new LinkedList<>();
        if (b.getStalkBabyTypes() != null) {
            for (Dna dna : b.getStalkBabyTypes()) {
                if (dna != null) {
                    ret.add(dna);
                }
            }
        }
        if (b.getStalks() != null) {
            for (Stalk stalk : b.getStalks()) {
                if (stalk == null || stalk.getAttachedBabyIds() == null) {
                    continue;
                }
                for (Integer id : stalk.getAttachedBabyIds()) {
                    Yukkuri child =
                            id == null
                                    ? null
                                    : org.simyukkuri.util.YukkuriLookup.getYukkuriById(id);
                    if (child != null) {
                        Dna dna = new Dna();
                        dna.setType(child.getType());
                        dna.setFather(child.getFather());
                        dna.setMother(child.getMother());
                        dna.setAttitude(child.getAttitude());
                        dna.setIntelligence(child.getIntelligence());
                        ret.add(dna);
                    }
                }
            }
        }
        return ret;
    }

    private Dna firstDna(List<Dna> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private String dnaName(Dna dna) {
        if (dna == null) {
            return "から";
        }
        for (YukkuriType type : YukkuriType.values()) {
            if (type == dna.getType()) {
                return type.getNameJ();
            }
        }
        return String.valueOf(dna.getType());
    }

    private Yukkuri createPreviewBody(Dna dna) {
        if (dna == null) {
            return null;
        }
        return createPreviewBody(dna.getType(), AgeState.BABY);
    }

    private static Yukkuri createPreviewBody(Yukkuri source) {
        if (source == null) {
            return null;
        }
        return createPreviewBody(source.getType(), source.getAgeState());
    }

    private static Yukkuri createPreviewBody(YukkuriType type, AgeState ageState) {
        String className = type.getClassName();
        if (className == null) {
            return null;
        }
        try {
            Class<?> cls =
                    Class.forName("org.simyukkuri.entity.core.living.yukkuri.impl." + className);
            Method loadImages =
                    cls.getMethod(
                            "loadImages", ClassLoader.class, java.awt.image.ImageObserver.class);
            loadImages.invoke(
                    null,
                    ShowStatusFrame.class.getClassLoader(),
                    ShowStatusFrame.getInstance().facePanel);
            Constructor<?> constructor = cls.getConstructor();
            Yukkuri body = (Yukkuri) constructor.newInstance();
            Field boundaryField = cls.getDeclaredField("boundary");
            Field braidBoundaryField = cls.getDeclaredField("braidBoundary");
            boundaryField.setAccessible(true);
            braidBoundaryField.setAccessible(true);
            body.setBoundary(
                    (Dimension4y[]) boundaryField.get(null),
                    (Dimension4y[]) braidBoundaryField.get(null));
            body.setAgeState(ageState);
            body.setShadowVisible(false);
            return body;
        } catch (Exception ex) {
            return null;
        }
    }

    private String purposeText(Yukkuri b) {
        switch (b.getPurposeOfMoving()) {
            case BED:
                return GameText.read("command_status_wantsleep");
            case FOOD:
                return GameText.read("command_status_wanteat");
            case NONE:
                return GameText.read("command_status_noespecially");
            case SHIT:
                return GameText.read("command_status_wantunun");
            case STEAL:
                return GameText.read("command_status_wantsteal");
            case SUKKIRI:
                return GameText.read("command_status_wantsukkiri");
            case TAKEOUT:
                return GameText.read("command_status_wanttakeout");
            default:
                return GameText.read("command_status_wantyukkuri");
        }
    }

    private String moveTargetText(Yukkuri b) {
        Entity o = b.takeMappedObj(b.getMoveTargetId());
        if (o == null) {
            return GameText.read("command_status_nothing");
        }
        return o.getX() + "," + o.getY();
    }

    private String footBakeText(Yukkuri b) {
        switch (b.getFootBakeLevel()) {
            case CRITICAL:
                return GameText.read("command_status_fatal");
            case MEDIUM:
                return GameText.read("command_status_middle");
            default:
                return GameText.read("command_status_nothing");
        }
    }

    private String favText(Yukkuri b) {
        if (b.getFavoriteItems().size() == 0) {
            return GameText.read("command_status_nothing");
        }
        String fav = "";
        for (Map.Entry<FavItemType, Integer> entry : b.getFavoriteItems().entrySet()) {
            fav += entry.getKey() + ",";
        }
        return fav.substring(0, fav.length() - 1);
    }

    private String takeoutText(Yukkuri b) {
        if (b.getCarryItems().size() == 0) {
            return GameText.read("command_status_nothing");
        }
        String bring = GameText.read("command_status_nothing");
        for (TakeoutItemType item : b.getCarryItems().keySet()) {
            switch (item) {
                case FOOD:
                    bring = GameText.read("command_status_food");
                    break;
                case SHIT:
                    bring = GameText.read("command_status_unun");
                    break;
                case TOY:
                    bring = GameText.read("command_status_toys");
                    break;
                default:
                    bring = GameText.read("command_status_children");
            }
        }
        return bring;
    }

    private String relationText(Yukkuri b) {
        return GameText.read("command_status_partner")
                + ": "
                + bodyIdOrNone(b.getPartner())
                + "\n"
                + GameText.read("command_status_father")
                + ": "
                + bodyIdOrNone(b.getFather())
                + "\n"
                + GameText.read("command_status_mother")
                + ": "
                + bodyIdOrNone(b.getMother())
                + "\n"
                + GameText.read("command_status_eldersister")
                + ": "
                + idList(b.getElderSisters())
                + "\n"
                + GameText.read("command_status_littlesister")
                + ": "
                + idList(b.getSisters())
                + "\n"
                + GameText.read("command_status_child")
                + ": "
                + idList(b.getChildren());
    }

    private String stateText(Yukkuri b) {
        String attach = "";
        if (b.getAttach() != null && b.getAttach().size() != 0) {
            for (Attachment a : b.getAttach()) {
                attach += a.toString() + " / ";
            }
        }
        if (attach.length() == 0) {
            attach = GameText.read("command_status_nothing");
        } else {
            attach = attach.substring(0, attach.length() - 3);
        }
        return GameText.read("command_status_status")
                + ": "
                + stateSummary(b)
                + "\n"
                + GameText.read("command_status_equip")
                + ": "
                + attach
                + "\n"
                + GameText.read("command_status_trauma")
                + ": "
                + traumaText(b)
                + "\n"
                + GameText.read("command_status_conveyitem")
                + ": "
                + takeoutText(b);
    }

    private String stateSummary(Yukkuri b) {
        String emo = "";
        if (b.isAngry()) {
            emo += GameText.read("command_status_anger");
        }
        if (b.isScare()) {
            emo += GameText.read("command_status_scare");
        }
        if (b.getPainState() != Pain.NONE) {
            emo += GameText.read("command_status_hurt");
        }
        if (b.isSad()) {
            emo += GameText.read("command_status_sad");
        }
        if (b.isBlind()) {
            emo += GameText.read("command_status_blind");
        }
        if (b.isPurupuru()) {
            emo += GameText.read("command_status_shake");
        }
        if (b.isDirty()) {
            emo += GameText.read("command_status_dirty");
        }
        if (b.isExciting()) {
            emo += GameText.read("command_status_hatsujou");
        }
        if (b.isHappy()) {
            emo += GameText.read("command_status_pleasure");
        }
        if (b.isMelt()) {
            emo += GameText.read("command_status_melt");
        }
        if (b.isOnlyAmaama()) {
            emo += GameText.read("command_status_tastedestruct");
        }
        if (b.isRelax()) {
            emo += GameText.read("command_status_doyukkuri");
        }
        if (b.isSilent()) {
            emo += GameText.read("command_status_silent");
        }
        if (b.isSleepy()) {
            emo += GameText.read("command_status_sleepy");
        }
        if (b.isStaying()) {
            emo += GameText.read("command_status_wait");
        }
        if (b.isVain()) {
            emo += GameText.read("command_status_vain");
        }
        if (b.isWet()) {
            emo += GameText.read("command_status_wet");
        }
        if (b.isYunnyaa()) {
            emo += GameText.read("command_status_yunyaa");
        }
        if (emo.length() == 0) {
            return GameText.read("command_status_nostate");
        }
        return emo.substring(0, Math.max(0, emo.length() - 2));
    }

    private String traumaText(Yukkuri b) {
        switch (b.getTrauma()) {
            case Factory:
                return GameText.read("command_status_plant");
            case Ubuse:
                return GameText.read("command_status_abuse");
            default:
                return GameText.read("command_status_nothing");
        }
    }

    private String idList(List<Integer> ids) {
        if (ids == null || ids.size() == 0) {
            return GameText.read("command_status_nothing");
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private String bodyIdOrNone(int id) {
        return org.simyukkuri.util.YukkuriLookup.getYukkuriById(id) == null
                ? GameText.read("command_status_nothing")
                : String.valueOf(id);
    }

    private void showById() {
        int id = parseCurrentId();
        Yukkuri yukkuri = SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().get(id);
        if (yukkuri == null) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        giveYukkuriInfo(yukkuri);
    }

    private void showFirst() {
        List<Yukkuri> sorted = sortedBodies(false);
        if (sorted.size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        giveYukkuriInfo(sorted.get(0));
    }

    private void showLast() {
        List<Yukkuri> sorted = sortedBodies(true);
        if (sorted.size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        giveYukkuriInfo(sorted.get(0));
    }

    private void showNext() {
        int id = parseCurrentId();
        List<Yukkuri> sorted = sortedBodies(false);
        if (sorted.size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        for (Yukkuri b : sorted) {
            if (b.getUniqueId() > id) {
                giveYukkuriInfo(b);
                return;
            }
        }
        giveYukkuriInfo(sorted.get(0));
    }

    private void showPrevious() {
        int id = parseCurrentId();
        List<Yukkuri> sorted = sortedBodies(true);
        if (sorted.size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        for (Yukkuri b : sorted) {
            if (b.getUniqueId() < id) {
                giveYukkuriInfo(b);
                return;
            }
        }
        giveYukkuriInfo(sorted.get(0));
    }

    private void showRandom() {
        if (SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        List<Yukkuri> bodies = new LinkedList<>();
        for (Map.Entry<Integer, Yukkuri> entry :
                SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().entrySet()) {
            bodies.add(entry.getValue());
        }
        giveYukkuriInfo(bodies.get(SimYukkuri.RND.nextInt(bodies.size())));
    }

    private void showPartner() {
        showRelated(currentBody == null ? -1 : currentBody.getPartner());
    }

    private void showChild() {
        if (currentBody == null || currentBody.getChildren().size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        showRelated(currentBody.getChildren().get(0));
    }

    private void showElderSister() {
        if (currentBody == null || currentBody.getElderSisters().size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        showRelated(currentBody.getElderSisters().get(0));
    }

    private void showLittleSister() {
        if (currentBody == null || currentBody.getSisters().size() == 0) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        showRelated(currentBody.getSisters().get(0));
    }

    private void showFather() {
        showRelated(currentBody == null ? -1 : currentBody.getFather());
    }

    private void showMother() {
        showRelated(currentBody == null ? -1 : currentBody.getMother());
    }

    private void showRelated(int id) {
        Yukkuri body = org.simyukkuri.util.YukkuriLookup.getYukkuriById(id);
        if (body == null) {
            showError(GameText.read("command_status_noexistyukkurierror"));
            return;
        }
        giveYukkuriInfo(body);
    }

    private int parseCurrentId() {
        try {
            return Integer.parseInt(idField.getText());
        } catch (Exception ex) {
            showError(GameText.read("command_status_numericerror"));
            return -1;
        }
    }

    private List<Yukkuri> sortedBodies(boolean reverse) {
        Comparator<Yukkuri> comparator = Comparator.comparingInt(Yukkuri::getUniqueId);
        if (reverse) {
            comparator = comparator.reversed();
        }
        return new LinkedList<Yukkuri>(
                        SimYukkuri.world.getCurrentWorldState().getYukkuriRegistry().values())
                .stream().sorted(comparator).collect(Collectors.toList());
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD);
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 12, 12, 12)));
        return panel;
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setFont(BODY_FONT);
        return button;
    }

    private static JProgressBar createBar(Color color) {
        JProgressBar bar = new JProgressBar(SwingConstants.HORIZONTAL);
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setBackground(new Color(229, 234, 240));
        bar.setBorder(BorderFactory.createLineBorder(BORDER));
        return bar;
    }

    private static void setBar(JProgressBar bar, int value, int max) {
        max = Math.max(1, max);
        value = Math.max(0, Math.min(value, max));
        bar.setMaximum(max);
        bar.setValue(value);
        bar.setString(value + " / " + max);
    }

    /** 個体選択時の入力エラーを表示します。 */
    public static void showError(String s) {
        JLabel label = new JLabel(s);
        label.setForeground(Color.RED);
        JOptionPane.showMessageDialog(ShowStatusFrame.getInstance(), label);
    }

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {}

    public static ShowStatusFrame getInstance() {
        return instance;
    }

    private static class FacePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Yukkuri body;

        FacePanel() {
            setPreferredSize(new Dimension(118, 118));
            setOpaque(false);
        }

        void setBody(Yukkuri body) {
            this.body = createPreviewBody(body);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(232, 238, 246));
                g2.fillOval(4, 4, 108, 108);
                g2.setColor(ACCENT);
                g2.drawOval(4, 4, 108, 108);
                if (body == null) {
                    g2.setColor(TEXT);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
                    g2.drawString("?", 54, 64);
                    return;
                }
                BufferedImage image = renderBodyImage(body);
                Rectangle bounds = findOpaqueBounds(image);
                g2.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.setClip(6, 6, 104, 104);
                int box = 88;
                int drawW = box;
                int drawH = box;
                if (bounds.width > 0 && bounds.height > 0) {
                    float scale = Math.min(box / (float) bounds.width, box / (float) bounds.height);
                    drawW = Math.max(1, Math.round(bounds.width * scale));
                    drawH = Math.max(1, Math.round(bounds.height * scale));
                }
                int dx = 58 - (drawW / 2);
                int dy = 58 - (drawH / 2);
                g2.drawImage(
                        image,
                        dx,
                        dy,
                        dx + drawW,
                        dy + drawH,
                        bounds.x,
                        bounds.y,
                        bounds.x + bounds.width,
                        bounds.y + bounds.height,
                        this);
                g2.setClip(null);
            } finally {
                g2.dispose();
            }
        }

        private BufferedImage renderBodyImage(Yukkuri target) {
            AppearanceSnapshot snapshot = new AppearanceSnapshot(target);
            try {
                snapshot.applyNeutral();
                target.updateSpriteSize();
                Sprite base = target.getSpriteSetite();
                Sprite expand = target.getExpandedSpriteSet();
                Sprite braid = target.getBraidSprite();
                Point4y origin = new Point4y(180, 190);
                calcDrawBodyPosition(origin, base);
                calcDrawBodyPosition(origin, expand);
                calcDrawBodyPosition(origin, braid);

                BufferedImage image = new BufferedImage(360, 260, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                try {
                    YukkuriUtil.drawYukkuri(g2, this, target);
                } finally {
                    g2.dispose();
                }
                return image;
            } finally {
                snapshot.restore();
            }
        }

        private Rectangle findOpaqueBounds(BufferedImage image) {
            int minX = image.getWidth();
            int minY = image.getHeight();
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (((image.getRGB(x, y) >>> 24) & 0xff) > 8) {
                        if (x < minX) {
                            minX = x;
                        }
                        if (y < minY) {
                            minY = y;
                        }
                        if (x > maxX) {
                            maxX = x;
                        }
                        if (y > maxY) {
                            maxY = y;
                        }
                    }
                }
            }
            if (maxX < minX || maxY < minY) {
                return new Rectangle(0, 0, image.getWidth(), image.getHeight());
            }
            return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }

        private void calcDrawBodyPosition(Point4y origin, Sprite spr) {
            int sizeW = Translate.transSize(spr.getImageW());
            int sizeH = Translate.transSize(spr.getImageH());
            int pivX = Translate.transSize(spr.getPivotX());
            int pivY = Translate.transSize(spr.getPivotY());
            spr.calcScreenRect(origin, pivX, pivY, sizeW, sizeH);
        }
    }

    private static class BodyImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        protected final Yukkuri body;
        private final int size;

        BodyImagePanel(Yukkuri body, int size) {
            this.body = createPreviewBody(body);
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int oval = Math.min(getWidth(), getHeight()) - 8;
                int ox = (getWidth() - oval) / 2;
                int oy = (getHeight() - oval) / 2;
                g2.setColor(new Color(232, 238, 246));
                g2.fillOval(ox, oy, oval, oval);
                g2.setColor(ACCENT);
                g2.drawOval(ox, oy, oval, oval);
                if (body == null) {
                    g2.setColor(MUTED);
                    g2.drawString("-", getWidth() / 2 - 3, getHeight() / 2 + 4);
                    return;
                }
                BufferedImage image = renderBodyImage(body);
                Rectangle bounds = findOpaqueBounds(image);
                int box = Math.max(1, oval - 18);
                float scale = Math.min(box / (float) bounds.width, box / (float) bounds.height);
                int drawW = Math.max(1, Math.round(bounds.width * scale));
                int drawH = Math.max(1, Math.round(bounds.height * scale));
                int dx = getWidth() / 2 - drawW / 2;
                int dy = getHeight() / 2 - drawH / 2;
                g2.setClip(ox + 2, oy + 2, oval - 4, oval - 4);
                g2.drawImage(
                        image,
                        dx,
                        dy,
                        dx + drawW,
                        dy + drawH,
                        bounds.x,
                        bounds.y,
                        bounds.x + bounds.width,
                        bounds.y + bounds.height,
                        this);
                g2.setClip(null);
            } finally {
                g2.dispose();
            }
        }

        protected BufferedImage renderBodyImage(Yukkuri target) {
            AppearanceSnapshot snapshot = new AppearanceSnapshot(target);
            try {
                snapshot.applyNeutral();
                target.updateSpriteSize();
                Sprite base = target.getSpriteSetite();
                Sprite expand = target.getExpandedSpriteSet();
                Sprite braid = target.getBraidSprite();
                Point4y origin = new Point4y(180, 190);
                calcDrawBodyPosition(origin, base);
                calcDrawBodyPosition(origin, expand);
                calcDrawBodyPosition(origin, braid);
                BufferedImage image = new BufferedImage(360, 260, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                try {
                    YukkuriUtil.drawYukkuri(g2, this, target);
                } finally {
                    g2.dispose();
                }
                return image;
            } finally {
                snapshot.restore();
            }
        }

        private Rectangle findOpaqueBounds(BufferedImage image) {
            int minX = image.getWidth();
            int minY = image.getHeight();
            int maxX = -1;
            int maxY = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (((image.getRGB(x, y) >>> 24) & 0xff) > 8) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < minX || maxY < minY) {
                return new Rectangle(0, 0, image.getWidth(), image.getHeight());
            }
            return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }

        private void calcDrawBodyPosition(Point4y origin, Sprite spr) {
            int sizeW = Translate.transSize(spr.getImageW());
            int sizeH = Translate.transSize(spr.getImageH());
            int pivX = Translate.transSize(spr.getPivotX());
            int pivY = Translate.transSize(spr.getPivotY());
            spr.calcScreenRect(origin, pivX, pivY, sizeW, sizeH);
        }
    }

    private static class MiniBodyButton extends BodyImagePanel {
        private static final long serialVersionUID = 1L;

        MiniBodyButton(Yukkuri body) {
            super(body, 48);
            setToolTipText(body == null ? "-" : body.toString());
        }
    }

    private static class PregnancyDrawingPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final List<Dna> babies;
        private final boolean womb;

        PregnancyDrawingPanel(List<Dna> babies, boolean womb) {
            this.babies = babies;
            this.womb = womb;
            setPreferredSize(new Dimension(250, 170));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.setColor(new Color(70, 76, 86));
                drawHeader(g2);
                if (womb) {
                    drawWomb(g2);
                } else {
                    drawStalk(g2);
                }
            } finally {
                g2.dispose();
            }
        }

        private void drawHeader(Graphics2D g2) {
            g2.drawRect(14, 15, 28, 28);
            g2.drawOval(20, 20, 16, 16);
            g2.drawLine(52, 20, 101, 20);
            g2.drawLine(52, 31, 88, 31);
            g2.drawLine(22, 50, 40, 50);
            g2.drawLine(22, 58, 33, 58);
            if (!womb) {
                g2.setFont(BODY_FONT.deriveFont(Font.BOLD, 15f));
                g2.drawString("<>", getWidth() - 48, 30);
            }
        }

        private void drawWomb(Graphics2D g2) {
            g2.drawOval(62, 54, 140, 78);
            drawBabyDots(g2, 88, 75, 4, 26, 25);
            g2.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            g2.drawString("たい内", 15, getHeight() - 18);
        }

        private void drawStalk(Graphics2D g2) {
            int lineY = 55;
            g2.drawLine(50, lineY, getWidth() - 33, lineY);
            g2.drawLine(58, lineY + 2, getWidth() - 51, lineY + 43);
            int max = Math.max(1, babies.size());
            for (int i = 0; i < max; i++) {
                int x = 76 + i * 30;
                int stemTop = lineY + 18 + i * 5;
                int fruitY = stemTop + 26;
                if (x > getWidth() - 45) {
                    break;
                }
                g2.drawLine(x, stemTop, x, fruitY);
                g2.drawOval(x - 10, fruitY, 20, 20);
            }
            g2.setFont(BODY_FONT.deriveFont(Font.BOLD, 13f));
            g2.drawString("くき", 15, getHeight() - 18);
        }

        private void drawBabyDots(Graphics2D g2, int x, int y, int count, int stepX, int stepY) {
            int max = Math.max(1, babies.size());
            for (int i = 0; i < max; i++) {
                int dx = x + (i % count) * stepX;
                int dy = y + (i / count) * stepY;
                g2.drawOval(dx, dy, 17, 17);
            }
        }
    }

    private static class AppearanceSnapshot {
        private static final String[] BOOLEAN_FIELDS = {
            "dead",
            "crushed",
            "burned",
            "dirty",
            "stubbornlyDirty",
            "wet",
            "melt",
            "pealed",
            "packed",
            "blind",
            "bNeedled",
            "sleeping",
            "angry",
            "scare",
            "furifuri",
            "strike",
            "eating",
            "eatingShit",
            "peropero",
            "sukkiri",
            "beVain",
            "pikopiko",
            "purupuru",
            "callingParents",
            "yunnyaa",
            "nobinobi",
            "exciting",
            "bForceExciting",
            "relax"
        };
        private final Yukkuri body;
        private final Map<String, Object> saved = new LinkedHashMap<>();

        AppearanceSnapshot(Yukkuri body) {
            this.body = body;
            save("damage");
            save("sickPeriod");
            save("footBakePeriod");
            save("extForce");
            save("unyoForceH");
            save("unyoForceW");
            save("forceFace");
            save("criticalDamege");
            save("eCoreAnkoState");
            for (String field : BOOLEAN_FIELDS) {
                save(field);
            }
        }

        void applyNeutral() {
            set("damage", 0);
            set("sickPeriod", 0);
            set("footBakePeriod", 0);
            set("extForce", 0);
            set("unyoForceH", 0);
            set("unyoForceW", 0);
            set("forceFace", -1);
            set("criticalDamege", null);
            set("eCoreAnkoState", CoreAnkoState.NORMAL);
            for (String field : BOOLEAN_FIELDS) {
                set(field, Boolean.FALSE);
            }
        }

        void restore() {
            for (Map.Entry<String, Object> entry : saved.entrySet()) {
                set(entry.getKey(), entry.getValue());
            }
        }

        private void save(String name) {
            try {
                saved.put(name, field(name).get(body));
            } catch (ReflectiveOperationException ex) {
                // Missing fields are ignored so status rendering never breaks the window.
            }
        }

        private void set(String name, Object value) {
            try {
                field(name).set(body, value);
            } catch (ReflectiveOperationException ex) {
                // Ignore and continue rendering with the original value.
            }
        }

        private Field field(String name) throws NoSuchFieldException {
            Class<?> type = body.getClass();
            while (type != null) {
                try {
                    Field f = type.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ex) {
                    type = type.getSuperclass();
                }
            }
            throw new NoSuchFieldException(name);
        }
    }
}
