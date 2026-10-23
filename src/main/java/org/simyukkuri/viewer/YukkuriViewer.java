package org.simyukkuri.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.simyukkuri.Const;
import org.simyukkuri.SimYukkuri;
import org.simyukkuri.draw.Dimension4y;
import org.simyukkuri.draw.MyPane;
import org.simyukkuri.draw.Point4y;
import org.simyukkuri.draw.Translate;
import org.simyukkuri.engine.World;
import org.simyukkuri.entity.core.living.yukkuri.Yukkuri;
import org.simyukkuri.enums.AgeState;
import org.simyukkuri.enums.Direction;
import org.simyukkuri.system.Sprite;
import org.simyukkuri.util.YukkuriUtil;

/** Standalone preview that uses the simulator's BodyRenderer drawing path. */
public class YukkuriViewer extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final String ACTION_NORMAL = "通常";
    private static final String ACTION_UNYO = "うにょ";
    private static final String ACTION_NOBINOBI = "のびのび";
    private static final String ACTION_SUKKIRI = "すっきり";
    private static final String ACTION_PULL = "体を伸ばす";
    private static final String ACTION_PUSH = "押しつぶし";
    private static final File LOG_FILE = new File("yukkuri_viewer_error.log");

    private final DefaultComboBoxModel<YukkuriEntry> yukkuriModel =
            new DefaultComboBoxModel<YukkuriEntry>();
    private final JComboBox<YukkuriEntry> yukkuriCombo = new JComboBox<YukkuriEntry>(yukkuriModel);
    private final JComboBox<AgeState> ageCombo =
            new JComboBox<AgeState>(new AgeState[] {AgeState.ADULT, AgeState.CHILD, AgeState.BABY});
    private final JComboBox<Direction> directionCombo =
            new JComboBox<Direction>(Direction.values());
    private final JComboBox<String> actionCombo =
            new JComboBox<String>(
                    new String[] {
                        ACTION_NORMAL,
                        ACTION_UNYO,
                        ACTION_NOBINOBI,
                        ACTION_SUKKIRI,
                        ACTION_PULL,
                        ACTION_PUSH
                    });
    private final JCheckBox animate = new JCheckBox("アニメーション");
    private final JSlider zoomSlider = new JSlider(100, 500, 300);
    private final JLabel statusLabel = new JLabel("Ready");
    private final SimulatorPreview preview = new SimulatorPreview();
    private final JTextArea diagnosticsArea = new JTextArea(8, 32);
    private final Timer timer = new Timer(120, e -> preview.nextFrame());

    /** キャラクターの表情と伸縮を確認する画面を構築します。 */
    public YukkuriViewer() {
        super("Yukkuri Simulator Preview");
        SimYukkuri.UNYO = true;
        if (SimYukkuri.world == null) {
            SimYukkuri.world = new World(0, 1);
        }
        Translate.setCanvasSize(1200, 1200, 100, 100, new float[] {1.0f});
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 640));
        setContentPane(createContent());
        preview.setDiagnosticsArea(diagnosticsArea);
        loadEntries();
        wireEvents();
        setLocationRelativeTo(null);
        updateBody();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBorder(BorderFactory.createTitledBorder("本体描画テスト"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        addRow(controls, c, "ゆっくり", yukkuriCombo);
        addRow(controls, c, "年齢", ageCombo);
        addRow(controls, c, "向き", directionCombo);
        addRow(controls, c, "アクション", actionCombo);
        addRow(controls, c, "動作", animate);
        addRow(controls, c, "ズーム", zoomSlider);

        root.add(controls, BorderLayout.NORTH);
        root.add(preview, BorderLayout.CENTER);
        diagnosticsArea.setEditable(false);
        diagnosticsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        diagnosticsArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane diagnosticsScroll = new JScrollPane(diagnosticsArea);
        diagnosticsScroll.setBorder(BorderFactory.createTitledBorder("伸縮チェック"));
        root.add(diagnosticsScroll, BorderLayout.EAST);
        root.add(statusLabel, BorderLayout.SOUTH);
        return root;
    }

    private void addRow(JPanel panel, GridBagConstraints c, String label, Component component) {
        c.gridx = 0;
        c.weightx = 0.0;
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(80, 24));
        panel.add(l, c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(component, c);
        c.gridy++;
    }

    private void loadEntries() {
        yukkuriCombo.setRenderer(
                new DefaultListCellRenderer() {
                    private static final long serialVersionUID = 1L;

                    public Component getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus) {
                        super.getListCellRendererComponent(
                                list, value, index, isSelected, cellHasFocus);
                        if (value instanceof YukkuriEntry) {
                            setText(((YukkuriEntry) value).label);
                        }
                        return this;
                    }
                });
        yukkuriModel.addElement(
                new YukkuriEntry("れいむ", "org.simyukkuri.entity.core.living.yukkuri.impl.Reimu"));
        yukkuriModel.addElement(
                new YukkuriEntry("まりさ", "org.simyukkuri.entity.core.living.yukkuri.impl.Marisa"));
        yukkuriModel.addElement(
                new YukkuriEntry("ありす", "org.simyukkuri.entity.core.living.yukkuri.impl.Alice"));
        yukkuriModel.addElement(
                new YukkuriEntry("ぱちゅりー", "org.simyukkuri.entity.core.living.yukkuri.impl.Patch"));
        yukkuriModel.addElement(
                new YukkuriEntry("ちぇん", "org.simyukkuri.entity.core.living.yukkuri.impl.Chen"));
        yukkuriModel.addElement(
                new YukkuriEntry("みょん", "org.simyukkuri.entity.core.living.yukkuri.impl.Myon"));
        yukkuriModel.addElement(
                new YukkuriEntry("れみりゃ", "org.simyukkuri.entity.core.living.yukkuri.impl.Remirya"));
        yukkuriModel.addElement(
                new YukkuriEntry("ふらん", "org.simyukkuri.entity.core.living.yukkuri.impl.Fran"));
    }

    private void wireEvents() {
        yukkuriCombo.addActionListener(e -> updateBody());
        ageCombo.addActionListener(e -> updateBody());
        directionCombo.addActionListener(e -> updateBody());
        actionCombo.addActionListener(e -> updateBody());
        zoomSlider.addChangeListener(e -> preview.setZoom(zoomSlider.getValue() / 100.0));
        animate.addActionListener(
                e -> {
                    if (animate.isSelected()) {
                        timer.start();
                    } else {
                        timer.stop();
                        preview.resetFrame();
                    }
                });
    }

    private void updateBody() {
        try {
            YukkuriEntry entry = (YukkuriEntry) yukkuriCombo.getSelectedItem();
            if (entry == null) {
                return;
            }
            Yukkuri body = createBody(entry.className);
            body.setAgeState((AgeState) ageCombo.getSelectedItem());
            body.setDirection((Direction) directionCombo.getSelectedItem());
            body.setShadowVisible(false);
            preview.setBody(body, (String) actionCombo.getSelectedItem());
            statusLabel.setText("YukkuriUtil.drawYukkuri 使用: " + entry.label);
        } catch (Exception ex) {
            Throwable cause = unwrap(ex);
            logError("Failed to create body preview", cause);
            statusLabel.setText(
                    "生成失敗: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    private Throwable unwrap(Exception ex) {
        if (ex instanceof InvocationTargetException
                && ((InvocationTargetException) ex).getTargetException() != null) {
            return ((InvocationTargetException) ex).getTargetException();
        }
        return ex;
    }

    private static void logError(String message, Throwable t) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(
                    "["
                            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                            + "] "
                            + message);
            if (t != null) {
                t.printStackTrace(writer);
            }
            writer.println();
        } catch (IOException ignored) {
            // Logging must never hide the original preview error from the UI.
        }
    }

    private Yukkuri createBody(String className) throws Exception {
        Class<?> cls = Class.forName(className);
        Method loadImages =
                cls.getMethod("loadImages", ClassLoader.class, java.awt.image.ImageObserver.class);
        loadImages.invoke(null, YukkuriViewer.class.getClassLoader(), preview.getImageObserver());

        Yukkuri body = (Yukkuri) cls.getConstructor().newInstance();
        Field boundaryField = cls.getDeclaredField("boundary");
        Field braidBoundaryField = cls.getDeclaredField("braidBoundary");
        boundaryField.setAccessible(true);
        braidBoundaryField.setAccessible(true);
        body.setBoundary(
                (Dimension4y[]) boundaryField.get(null),
                (Dimension4y[]) braidBoundaryField.get(null));
        return body;
    }

    /** 独立したキャラクタープレビューを起動します。 */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception ignored) {
                            // Swing default is acceptable for this diagnostic viewer.
                        }
                        new YukkuriViewer().setVisible(true);
                    }
                });
    }

    private static final class YukkuriEntry {
        final String label;
        final String className;

        YukkuriEntry(String label, String className) {
            this.label = label;
            this.className = className;
        }
    }

    private static final class SimulatorPreview extends JPanel {
        private static final long serialVersionUID = 1L;
        private final MyPane imageObserver = new MyPane();
        private Yukkuri body;
        private String action = ACTION_NORMAL;
        private AgeState ageState = AgeState.ADULT;
        private JTextArea diagnosticsArea;
        private double zoom = 3.0;
        private int frame;

        SimulatorPreview() {
            setPreferredSize(new Dimension(760, 480));
            setBackground(new Color(46, 48, 52));
        }

        MyPane getImageObserver() {
            return imageObserver;
        }

        void setBody(Yukkuri body, String action) {
            this.body = body;
            this.action = action == null ? ACTION_NORMAL : action;
            this.ageState = body.getAgeState();
            this.frame = 0;
            repaint();
        }

        void setDiagnosticsArea(JTextArea diagnosticsArea) {
            this.diagnosticsArea = diagnosticsArea;
        }

        void setZoom(double zoom) {
            this.zoom = zoom;
            repaint();
        }

        void nextFrame() {
            frame++;
            repaint();
        }

        void resetFrame() {
            frame = 0;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                paintChecker(g2);
                if (body == null) {
                    paintCenteredText(g2, "No body");
                    return;
                }
                BufferedImage image = renderSimulatorBody();
                int w = Math.max(1, (int) Math.round(image.getWidth() * zoom));
                int h = Math.max(1, (int) Math.round(image.getHeight() * zoom));
                int x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;
                g2.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(
                        image, x, y, x + w, y + h, 0, 0, image.getWidth(), image.getHeight(), null);
                g2.setColor(new Color(255, 255, 255, 210));
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString(action + " / YukkuriUtil.drawYukkuri", 12, 22);
            } catch (Exception ex) {
                logError("Failed to render simulator body", ex);
                paintCenteredText(g2, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            } finally {
                g2.dispose();
            }
        }

        private BufferedImage renderSimulatorBody() {
            applyAction();
            body.updateSpriteSize();
            Sprite base = body.getSpriteSetite();
            Sprite expand = body.getExpandedSpriteSet();
            Sprite braid = body.getBraidSprite();
            Point4y origin = new Point4y(180, 190);
            calcDrawBodyPosition(origin, base);
            calcDrawBodyPosition(origin, expand);
            calcDrawBodyPosition(origin, braid);
            updateStretchDiagnostics(base, expand, braid);

            BufferedImage image = new BufferedImage(360, 260, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            try {
                YukkuriUtil.drawYukkuri(g2, this, body);
            } finally {
                g2.dispose();
            }
            return image;
        }

        private void applyAction() {
            body.setNobinobi(false);
            body.setSukkiri(false);
            body.setExternalPressure(0);
            body.setUnyoOffsetH(0);
            body.setUnyoOffsetW(0);
            body.setAge(actionAgeBase() + frame);
            if (ACTION_UNYO.equals(action)) {
                int v = (frame % 18) - 9;
                body.setUnyoOffsetH(-v);
                body.setUnyoOffsetW(v);
            } else if (ACTION_NOBINOBI.equals(action)) {
                body.setNobinobi(true);
            } else if (ACTION_SUKKIRI.equals(action)) {
                body.setSukkiri(true);
            } else if (ACTION_PULL.equals(action)) {
                body.setExternalPressure(
                        Const.EXT_FORCE_PULL_LIMIT[body.getAgeState().ordinal()] / 2);
            } else if (ACTION_PUSH.equals(action)) {
                body.setExternalPressure(
                        Const.EXT_FORCE_PUSH_LIMIT[body.getAgeState().ordinal()] / 2);
            }
        }

        private void updateStretchDiagnostics(Sprite base, Sprite expand, Sprite braid) {
            if (diagnosticsArea == null) {
                return;
            }
            final boolean widthChanged = expand.getImageW() != base.getImageW();
            final boolean heightChanged = expand.getImageH() != base.getImageH();
            StringBuilder text = new StringBuilder();
            text.append("action: ").append(action).append('\n');
            text.append("base   : ")
                    .append(base.getImageW())
                    .append('x')
                    .append(base.getImageH())
                    .append('\n');
            text.append("expand : ")
                    .append(expand.getImageW())
                    .append('x')
                    .append(expand.getImageH())
                    .append('\n');
            text.append("braid  : ")
                    .append(braid.getImageW())
                    .append('x')
                    .append(braid.getImageH())
                    .append('\n');
            text.append('\n');
            if (!widthChanged && !heightChanged) {
                text.append("伸縮なし: 現在のアクションではexpand矩形がbaseと同じです。\n");
            } else {
                text.append("伸縮検出: ");
                if (widthChanged) {
                    text.append("W ");
                }
                if (heightChanged) {
                    text.append("H ");
                }
                text.append('\n');
                text.append("OK   body/face/hair/effect/accessory : expand矩形で描画\n");
                text.append("注意 braid            : braid専用矩形。胴体伸縮とは別管理\n");
                text.append('\n');
                text.append("判定: 注意行のパーツが画面上で浮く/ずれる場合、BodyUtilの描画先矩形をexpand基準へ変更してください。\n");
            }
            diagnosticsArea.setText(text.toString());
            diagnosticsArea.setCaretPosition(0);
        }

        private int actionAgeBase() {
            if (ageState == AgeState.ADULT) {
                return body.getChildLimitBase();
            }
            if (ageState == AgeState.CHILD) {
                return body.getBabyLimitBase();
            }
            return 0;
        }

        private void calcDrawBodyPosition(Point4y origin, Sprite spr) {
            int sizeW = Translate.transSize(spr.getImageW());
            int sizeH = Translate.transSize(spr.getImageH());
            int pivX = Translate.transSize(spr.getPivotX());
            int pivY = Translate.transSize(spr.getPivotY());
            spr.calcScreenRect(origin, pivX, pivY, sizeW, sizeH);
        }

        private void paintChecker(Graphics2D g2) {
            int size = 16;
            Color a = new Color(70, 72, 76);
            Color b = new Color(58, 60, 64);
            for (int y = 0; y < getHeight(); y += size) {
                for (int x = 0; x < getWidth(); x += size) {
                    g2.setColor(((x / size) + (y / size)) % 2 == 0 ? a : b);
                    g2.fillRect(x, y, size, size);
                }
            }
        }

        private void paintCenteredText(Graphics2D g2, String text) {
            g2.setColor(new Color(230, 230, 230));
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            int width = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (getWidth() - width) / 2, getHeight() / 2);
        }
    }
}
