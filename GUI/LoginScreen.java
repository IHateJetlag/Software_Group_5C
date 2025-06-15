package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ログイン画面を表示し、ユーザー認証を行うクラス。
 */
public class LoginScreen {

    private static final Map<String, String> userCredentials = new HashMap<>();
    private static final Map<String, User> userInfo = new HashMap<>();

    static {
        // --- アカウント1: 智也 ---
        userCredentials.put("tomoya", "tomoya");
        userInfo.put("tomoya", new User("001", "智也", new Color(135, 206, 250), true));

        // --- アカウント2: test1 ---
        userCredentials.put("test1", "test1");
        userInfo.put("test1", new User("101", "人1", new Color(255, 182, 193), true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginScreen::createAndShowLoginGUI);
    }

    private static void createAndShowLoginGUI() {
        JFrame loginFrame = new JFrame("ログイン");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(350, 200);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ユーザー名:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        JTextField usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("パスワード:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        JPasswordField passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton loginButton = new JButton("ログイン");
        panel.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (userCredentials.containsKey(username) && userCredentials.get(username).equals(password)) {
                User loggedInUser = userInfo.get(username);
                loginFrame.dispose();
                SwingUtilities.invokeLater(() -> {
                    Calender mainApp = new Calender(loggedInUser);
                    mainApp.createAndShowGUI();
                });
            } else {
                JOptionPane.showMessageDialog(loginFrame, "ユーザー名またはパスワードが違います。", "ログイン失敗", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginFrame.add(panel);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }
}