package client.gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LoginScreen {

    private static final Map<String, String> userCredentials = new HashMap<>();
    private static final Map<String, User> userInfo = new HashMap<>();

    static {
        // --- 初期ユーザー情報を設定 ---
        userCredentials.put("tomoya", "tomoya");
        userInfo.put("tomoya", new User("001", "智也", "tomoya@calender.app", new Color(135, 206, 250), true));
        
        userCredentials.put("tanaka", "pass");
        userInfo.put("tanaka", new User("002", "田中", "tanaka@example.com", new Color(220, 220, 220), false));

        userCredentials.put("suzuki", "pass");
        userInfo.put("suzuki", new User("003", "鈴木", "suzuki@example.com", new Color(220, 220, 220), false));

        userCredentials.put("sato", "pass");
        userInfo.put("sato", new User("004", "佐藤", "sato@example.com", new Color(200, 220, 200), false));
        
        userCredentials.put("test1", "test1");
        userInfo.put("test1", new User("101", "人1", "test1@calender.app", new Color(255, 182, 193), true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginScreen::createAndShowLoginGUI);
    }

    private static void createAndShowLoginGUI() {
        JFrame loginFrame = new JFrame("ログイン");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(400, 250);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ユーザーID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("パスワード:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginButton = new JButton("ログイン");
        JButton signupButton = new JButton("新規作成");
        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(buttonPanel, gbc);

        loginButton.addActionListener(e -> handleLogin(loginFrame, usernameField.getText(), new String(passwordField.getPassword())));
        passwordField.addActionListener(e -> handleLogin(loginFrame, usernameField.getText(), new String(passwordField.getPassword())));

        signupButton.addActionListener(e -> createAndShowSignUpDialog(loginFrame));

        loginFrame.add(panel);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private static void handleLogin(JFrame loginFrame, String username, String password) {
        if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
            User loggedInUser = userInfo.get(username);
            loginFrame.dispose();
            SwingUtilities.invokeLater(() -> {
                // --- ★★★ ここを修正しました ★★★ ---
                // Calenderのコンストラクタに、ログインユーザー情報と、全ユーザー情報のMapを渡します。
                Calender mainApp = new Calender(loggedInUser, userInfo);
                mainApp.createAndShowGUI();
            });
        } else {
            JOptionPane.showMessageDialog(loginFrame, "ユーザーIDまたはパスワードが違います。", "ログイン失敗", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // createAndShowSignUpDialog メソッドは変更なし
    private static void createAndShowSignUpDialog(JFrame owner) {
        JDialog dialog = new JDialog(owner, "アカウント新規作成", true);
        dialog.setSize(450, 350);
        dialog.setLayout(new BorderLayout(10, 10));

        // --- 入力パネル ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField nameField = new JTextField(20);
        JTextField idField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JPasswordField passConfirmField = new JPasswordField(20);

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("表示名:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; inputPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("ユーザーID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; inputPanel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("メールアドレス:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; inputPanel.add(emailField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(new JLabel("パスワード:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; inputPanel.add(passField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; inputPanel.add(new JLabel("パスワード(確認):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; inputPanel.add(passConfirmField, gbc);

        // --- ボタンパネル ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton registerButton = new JButton("登録");
        JButton cancelButton = new JButton("キャンセル");
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());

        registerButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String id = idField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());
            String passwordConfirm = new String(passConfirmField.getPassword());

            // バリデーション
            if (name.isEmpty() || id.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "すべての項目を入力してください。", "入力エラー", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (userCredentials.containsKey(id)) {
                JOptionPane.showMessageDialog(dialog, "そのユーザーIDは既に使用されています。", "登録エラー", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!password.equals(passwordConfirm)) {
                JOptionPane.showMessageDialog(dialog, "パスワードが一致しません。", "登録エラー", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 簡単なメールアドレス形式チェック
            if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                 JOptionPane.showMessageDialog(dialog, "有効なメールアドレスを入力してください。", "登録エラー", JOptionPane.WARNING_MESSAGE);
                 return;
            }

            // 新規ユーザー登録
            Random rand = new Random();
            Color randomColor = new Color(rand.nextInt(200), rand.nextInt(200), rand.nextInt(256));
            User newUser = new User(id, name, email, randomColor, true);

            userCredentials.put(id, password);
            userInfo.put(id, newUser);

            JOptionPane.showMessageDialog(dialog, "アカウント「" + name + "」を登録しました。\nログイン画面からログインしてください。", "登録完了", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}