package org.simyukkuri.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import org.simyukkuri.SimYukkuri;

/** Opening screen retained from the custom edition. */
public final class StartupScreen {
    private StartupScreen() {}

    /** 比較元の利用確認画面を表示します。 */
    public static boolean showWarningAndConfirmDialog() {
        String title = "【重要】利用前の確認と同意";
        String message =
                "このアプリケーションは、二次創作ジャンル「ゆっくり虐待（ゆ虐）」の世界観および設定に基づいています。\n\n"
                    + "■ コンテンツの性質について\n"
                    + "・暴力、グロテスクな表現、およびキャラクターへの過酷な扱いが含まれます。\n"
                    + "・いわゆる「愛でる」系のジャンルも、その背景には上記の要素が存在します。\n"
                    + "・これらのテーマに不快感を覚える方、および18歳未満の方のご利用は固くお断りいたします。\n\n"
                    + "■ 東方Project二次創作ガイドラインとの関連について\n"
                    + "本ジャンルは、原作である「東方Project」のイメージを損なう可能性があり、\n"
                    + "二次創作ガイドラインの観点から、極めて慎重な取り扱いが求められます。\n\n"
                    + "制作者およびユーザーの皆様は、原作への敬意を払い、\n"
                    + "その存続を脅かすことのないよう、良識ある行動をお願いいたします。\n\n"
                    + "----------------------------------------------------------------------------------------------------\n"
                    + "上記すべてを理解し、自己の責任においてアプリケーションを利用することに同意しますか？\n"
                    + "「いいえ」を選択した場合、アプリケーションは起動せずに終了します。";

        int result =
                JOptionPane.showConfirmDialog(
                        null,
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /** 比較元の起動画像を読み込み、中央表示するウィンドウを作ります。 */
    public static JWindow createSplashScreen() {
        try {
            final URL imageUrl = SimYukkuri.class.getResource("/images/SimYukkuri_splash.png");
            if (imageUrl == null) {
                System.err.println("スプラッシュ画像ファイルが見つかりません: /SimYukkuri_splash.png");
                return null;
            }
            BufferedImage image = ImageIO.read(imageUrl);
            if (image == null) {
                System.err.println("スプラッシュ画像のフォーマットが不正か、読み込みに失敗しました。");
                return null;
            }
            JWindow splash = new JWindow();
            splash.getContentPane().add(new JLabel(new ImageIcon(image)));
            splash.pack();
            splash.setLocationRelativeTo(null);
            return splash;
        } catch (IOException e) {
            System.err.println("スプラッシュ画像の読み込み中にI/Oエラーが発生しました。スキップします。: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("スプラッシュ画像の作成中に予期せぬエラーが発生しました。スキップします。: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
