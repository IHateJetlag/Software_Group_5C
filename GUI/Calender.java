package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class Calender {

    // --- 定数 ---
    private static final int CALENDAR_ROWS = 6;
    private static final int CALENDAR_COLS = 7;
    private static final String MY_PAGE_ID = "MY_PAGE";

    // --- UIコンポーネントなど ---
    private JFrame frame;
    private final List<JPanel> datePanels = new ArrayList<>();
    private JPanel monthDisplayPanel;
    private JPanel centerCardPanel;
    private CardLayout centerCardLayout;
    private JPanel sidebarPanel;
    private final Map<LocalDate, List<Appointment>> appointments = new HashMap<>();
    private LocalDate currentDate;
    private final List<Group> groups = new ArrayList<>();
    private User myUser;
    private Group currentGroup;

    // ★ コンストラクタは User 型を直接受け取る
    public Calender(User user) {
        this.myUser = user;
    }
    
    // (以降のメソッドは、以前の完全なコードと同じです)
    public void createAndShowGUI() {
        currentDate = LocalDate.now();
        initializeData();
        frame = new JFrame("Group-based Application - ログインユーザー: " + myUser.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        JPanel mainPanel = createMainLayout();
        frame.add(mainPanel);
        updateHeader();
        updateCalendar();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        startConsoleListener();
    }
    private void initializeData() {
        Group group1 = new Group("G01", "技術部", '技');
        User userTanaka = new User("002", "田中", new Color(220, 220, 220), false);
        User userSato = new User("004", "佐藤", new Color(200, 220, 200), false);
        group1.addUser(myUser);
        group1.addUser(userTanaka);
        group1.addUser(userSato);
        Group group2 = new Group("G02", "営業部", '営');
        User userSuzuki = new User("003", "鈴木", new Color(220, 220, 220), false);
        group2.addUser(myUser);
        group2.addUser(userSuzuki);
        group2.addUser(userTanaka);
        groups.add(group1);
        groups.add(group2);
        LocalDate today = LocalDate.now();
        addOrUpdateAppointment(today.withDayOfMonth(10), new Appointment("個人タスク", "レポート作成", myUser));
        addOrUpdateAppointment(today.withDayOfMonth(10), new Appointment("【共有】進捗確認", "定例の進捗確認", group1));
        addOrUpdateAppointment(today.withDayOfMonth(12), new Appointment("田中さんタスク", "サーバ移行", userTanaka));
        addOrUpdateAppointment(today.withDayOfMonth(20), new Appointment("営業部MTG", "戦略会議", group2));
    }
    private JPanel createMainLayout() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        sidebarPanel = createSidebarPanel();
        centerCardLayout = new CardLayout();
        centerCardPanel = new JPanel(centerCardLayout);
        centerCardPanel.add(createMyPagePanel(), MY_PAGE_ID);
        for (Group group : groups) {
            centerCardPanel.add(createChatPanel(group), group.getId());
        }
        JPanel calenderPanel = createCalendarView();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridx = 0; gbc.weightx = 1.0; mainPanel.add(sidebarPanel, gbc);
        gbc.gridx = 1; gbc.weightx = 8.0; mainPanel.add(centerCardPanel, gbc);
        gbc.gridx = 2; gbc.weightx = 11.0; mainPanel.add(calenderPanel, gbc);
        centerCardLayout.show(centerCardPanel, MY_PAGE_ID);
        currentGroup = null;
        return mainPanel;
    }
    private JPanel createSidebarPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Groups"));
        panel.setBackground(new Color(240, 240, 240));
        panel.add(createGroupIcon(MY_PAGE_ID, "マイページ", myUser.getName().charAt(0)));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        for (Group group : groups) {
            panel.add(createGroupIcon(group.getId(), group.getName(), group.getIconChar()));
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }
    private Component createGroupIcon(String id, String tooltip, char iconChar) {
        JLabel iconLabel = new JLabel(String.valueOf(iconChar), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isSelected = id.equals(MY_PAGE_ID) ? currentGroup == null : (currentGroup != null && id.equals(currentGroup.getId()));
                if (isSelected) g2.setColor(new Color(66, 133, 244));
                else g2.setColor(Color.LIGHT_GRAY);
                g2.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        Dimension iconSize = new Dimension(50, 50);
        iconLabel.setPreferredSize(iconSize);
        iconLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, iconSize.height));
        iconLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setToolTipText(tooltip);
        iconLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (id.equals(MY_PAGE_ID)) {
                    currentGroup = null;
                    centerCardLayout.show(centerCardPanel, MY_PAGE_ID);
                } else {
                    currentGroup = findGroupById(id);
                    centerCardLayout.show(centerCardPanel, id);
                }
                sidebarPanel.repaint();
                updateCalendar();
            }
        });
        return iconLabel;
    }
    private JPanel createMyPagePanel() {
        JPanel myPagePanel = new JPanel(new GridBagLayout());
        myPagePanel.setBackground(Color.WHITE);
        myPagePanel.setBorder(BorderFactory.createTitledBorder("My Page"));
        JLabel nameLabel = new JLabel(myUser.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        myPagePanel.add(nameLabel);
        return myPagePanel;
    }
    private JPanel createChatPanel(Group group) {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder(group.getName() + " - Chat"));
        JPanel messageArea = new JPanel();
        messageArea.setLayout(new BoxLayout(messageArea, BoxLayout.Y_AXIS));
        messageArea.setBackground(Color.WHITE);
        group.setMessageArea(messageArea);
        for (ChatMessage message : group.getChatHistory()) {
            addMessageToUI(message, group);
        }
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("送信");
        Action sendMessageAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = inputField.getText();
                if (!text.trim().isEmpty()) {
                    ChatMessage message = new ChatMessage(myUser, text, LocalDateTime.now());
                    group.addMessage(message);
                    addMessageToUI(message, group);
                    inputField.setText("");
                }
            }
        };
        inputField.addActionListener(sendMessageAction);
        sendButton.addActionListener(sendMessageAction);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        return chatPanel;
    }
    private void addMessageToUI(ChatMessage message, Group group) {
        JPanel messageArea = group.getMessageArea();
        if (messageArea == null) return;
        JPanel messageBubble = createMessageBubble(message);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(2, 5, 2, 5));
        if (message.getSender().isMe()) {
            wrapper.add(messageBubble, BorderLayout.EAST);
        } else {
            wrapper.add(messageBubble, BorderLayout.WEST);
        }
        messageArea.add(wrapper);
        messageArea.revalidate();
        messageArea.repaint();
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, messageArea);
        if (scrollPane != null) {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
        }
    }
    private JPanel createMessageBubble(ChatMessage message) {
        JPanel bubble = new JPanel(new BorderLayout(5, 2));
        bubble.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel senderLabel = new JLabel(message.getSender().getName());
        senderLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        senderLabel.setForeground(Color.DARK_GRAY);
        JTextArea messageText = new JTextArea(message.getText());
        messageText.setEditable(false);
        messageText.setLineWrap(true);
        messageText.setWrapStyleWord(true);
        messageText.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageText.setOpaque(false);
        if (message.getSender().isMe()) {
            bubble.setBackground(myUser.getColor());
            bubble.add(messageText, BorderLayout.CENTER);
        } else {
            bubble.setBackground(message.getSender().getColor());
            bubble.add(senderLabel, BorderLayout.NORTH);
            bubble.add(messageText, BorderLayout.CENTER);
        }
        JPanel wrapper = new JPanel();
        wrapper.add(bubble);
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(350, Short.MAX_VALUE));
        return wrapper;
    }
    private Group findGroupById(String id) {
        return groups.stream().filter(g -> g.getId().equals(id)).findFirst().orElse(null);
    }
    private JPanel createCalendarView() {
        JPanel calendarContainer = new JPanel(new BorderLayout(0, 10));
        calendarContainer.setBorder(new EmptyBorder(5, 5, 5, 5));
        calendarContainer.add(createHeaderPanel(), BorderLayout.NORTH);
        calendarContainer.add(createCalendarGridPanel(), BorderLayout.CENTER);
        calendarContainer.add(createFooterPanel(), BorderLayout.SOUTH);
        return calendarContainer;
    }
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JButton prevButton = new JButton("<");
        prevButton.addActionListener(e -> {
            currentDate = currentDate.minusMonths(1);
            updateHeader();
            updateCalendar();
        });
        JButton nextButton = new JButton(">");
        nextButton.addActionListener(e -> {
            currentDate = currentDate.plusMonths(1);
            updateHeader();
            updateCalendar();
        });
        monthDisplayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.add(prevButton, BorderLayout.WEST);
        panel.add(monthDisplayPanel, BorderLayout.CENTER);
        panel.add(nextButton, BorderLayout.EAST);
        return panel;
    }
    private void updateHeader() {
        monthDisplayPanel.removeAll();
        int year = currentDate.getYear();
        int month = currentDate.getMonthValue();
        JLabel label = new JLabel(String.format("%d年 %d月", year, month));
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthDisplayPanel.add(label);
        monthDisplayPanel.revalidate();
        monthDisplayPanel.repaint();
    }
    private Component createCalendarGridPanel() {
        JPanel gridContainer = new JPanel(new BorderLayout());
        JPanel dayOfWeekPanel = new JPanel(new GridLayout(1, CALENDAR_COLS));
        String[] days = {"日", "月", "火", "水", "木", "金", "土"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day, JLabel.CENTER);
            if (day.equals("日")) dayLabel.setForeground(Color.RED);
            if (day.equals("土")) dayLabel.setForeground(Color.BLUE);
            dayOfWeekPanel.add(dayLabel);
        }
        JPanel dateGridPanel = new JPanel(new GridLayout(CALENDAR_ROWS, CALENDAR_COLS, 2, 2));
        datePanels.clear();
        for (int i = 0; i < CALENDAR_ROWS * CALENDAR_COLS; i++) {
            JPanel dateCell = new JPanel(new BorderLayout());
            dateCell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            dateGridPanel.add(dateCell);
            datePanels.add(dateCell);
        }
        gridContainer.add(dayOfWeekPanel, BorderLayout.NORTH);
        gridContainer.add(dateGridPanel, BorderLayout.CENTER);
        return gridContainer;
    }
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        footerPanel.add(new JLabel("月:"));
        JTextField monthField = new JTextField(2);
        footerPanel.add(monthField);
        footerPanel.add(new JLabel("日:"));
        JTextField dayField = new JTextField(2);
        footerPanel.add(dayField);
        footerPanel.add(new JLabel("タイトル:"));
        JTextField titleField = new JTextField(15);
        footerPanel.add(titleField);
        JButton addButton = new JButton("追加/更新");
        addButton.addActionListener(e -> handleAddAppointmentFromFooter(monthField, dayField, titleField));
        footerPanel.add(addButton);
        return footerPanel;
    }
    private void updateCalendar() {
        YearMonth yearMonth = YearMonth.from(currentDate);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; 
        for (int i = 0; i < datePanels.size(); i++) {
            JPanel dateCell = datePanels.get(i);
            dateCell.removeAll();
            dateCell.setBackground(Color.WHITE);
            dateCell.setName(null);
            int day = i - startDayOfWeek + 1;
            if (day > 0 && day <= daysInMonth) {
                LocalDate cellDate = LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), day);
                buildDateCell(dateCell, cellDate);
                if (cellDate.equals(LocalDate.now())) {
                    dateCell.setBackground(new Color(220, 240, 255));
                }
                updateDateCellView(cellDate, currentGroup);
            }
            dateCell.revalidate();
            dateCell.repaint();
        }
    }
    private void buildDateCell(JPanel dateCell, LocalDate cellDate) {
        dateCell.setName(cellDate.toString());
        JLabel dateLabel = new JLabel(String.valueOf(cellDate.getDayOfMonth()), SwingConstants.RIGHT);
        dateLabel.setBorder(new EmptyBorder(2, 0, 0, 4));
        dateCell.add(dateLabel, BorderLayout.NORTH);
        JPanel appointmentContainer = new JPanel();
        appointmentContainer.setName("appointmentContainer");
        appointmentContainer.setLayout(new BoxLayout(appointmentContainer, BoxLayout.Y_AXIS));
        appointmentContainer.setOpaque(false);
        dateCell.add(appointmentContainer, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        JButton addButton = new JButton("+");
        addButton.setMargin(new Insets(0, 0, 0, 0));
        addButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        addButton.setFocusable(false);
        SwingUtilities.invokeLater(() -> {
            int height = addButton.getPreferredSize().height;
            if (height > 0) {
                addButton.setPreferredSize(new Dimension(height, height));
                buttonPanel.revalidate();
            }
        });
        addButton.addActionListener(e -> {
            Object owner = (currentGroup != null) ? (Object) currentGroup : (Object) myUser;
            showAppointmentDialog(cellDate, null, owner);
        });
        buttonPanel.add(addButton);
        dateCell.add(buttonPanel, BorderLayout.SOUTH);
    }
    private void handleAddAppointmentFromFooter(JTextField monthField, JTextField dayField, JTextField titleField) {
        try {
            int month = Integer.parseInt(monthField.getText());
            int day = Integer.parseInt(dayField.getText());
            String title = titleField.getText();
            if (title.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "タイトルを入力してください。", "入力エラー", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LocalDate targetDate = LocalDate.of(currentDate.getYear(), month, day);
            Object owner = (currentGroup != null) ? (Object) currentGroup : (Object) myUser;
            addOrUpdateAppointment(targetDate, new Appointment(title, "", owner));
            monthField.setText(""); dayField.setText(""); titleField.setText("");
            if (currentDate.getMonthValue() != month) {
                currentDate = currentDate.withMonth(month);
                updateHeader();
                updateCalendar();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "月日やタイトルを正しく入力してください。\n(例: 2月30日は無効です)", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }
    public void addOrUpdateAppointment(LocalDate date, Appointment appointment) {
        List<Appointment> appointmentList = appointments.computeIfAbsent(date, k -> new ArrayList<>());
        boolean updated = false;
        for (int i = 0; i < appointmentList.size(); i++) {
            Appointment existingApp = appointmentList.get(i);
            if (Objects.equals(existingApp.getOwner(), appointment.getOwner()) && existingApp.getTitle().equals(appointment.getTitle())) {
                appointmentList.set(i, appointment);
                updated = true;
                break;
            }
        }
        if (!updated) {
            appointmentList.add(appointment);
        }
        updateDateCellView(date, currentGroup);
    }
    public void removeAppointment(LocalDate date, Appointment appointment) {
        List<Appointment> appointmentList = appointments.get(date);
        if (appointmentList != null) {
            appointmentList.remove(appointment);
            if (appointmentList.isEmpty()) {
                appointments.remove(date);
            }
        }
        updateDateCellView(date, currentGroup);
    }
    private void updateDateCellView(LocalDate date, Group contextGroup) {
        for (JPanel dateCell : datePanels) {
            if (date.toString().equals(dateCell.getName())) {
                Component[] components = dateCell.getComponents();
                JPanel appointmentContainer = null;
                for (Component c : components) {
                    if (c instanceof JPanel && "appointmentContainer".equals(c.getName())) {
                        appointmentContainer = (JPanel) c;
                        break;
                    }
                }
                if (appointmentContainer == null) return;
                appointmentContainer.removeAll();
                List<Appointment> appointmentList = appointments.get(date);
                if (appointmentList != null) {
                    for (Appointment app : appointmentList) {
                        Object owner = app.getOwner();
                        boolean shouldDisplay = false;
                        if (contextGroup == null) {
                            if (owner.equals(myUser)) {
                                shouldDisplay = true;
                            } else if (owner instanceof Group) {
                                if (((Group) owner).getUsers().contains(myUser)) {
                                    shouldDisplay = true;
                                }
                            }
                        } else {
                            if (owner.equals(myUser)) {
                                shouldDisplay = true;
                            } else if (owner.equals(contextGroup)) {
                                shouldDisplay = true;
                            } else if (owner instanceof User) {
                                if (contextGroup.getUsers().contains(owner)) {
                                    shouldDisplay = true;
                                }
                            }
                        }
                        if (shouldDisplay) {
                            JButton appButton = new JButton("<html>" + app.getTitle() + "</html>");
                            if (owner instanceof User) {
                                appButton.setBackground(((User) owner).getColor());
                            } else if (owner instanceof Group) {
                                appButton.setBackground(new Color(144, 238, 144));
                            }
                            appButton.setOpaque(true);
                            appButton.addActionListener(e -> showAppointmentDialog(date, app, app.getOwner()));
                            appointmentContainer.add(appButton);
                        }
                    }
                }
                appointmentContainer.revalidate();
                appointmentContainer.repaint();
                break;
            }
        }
    }
    private void showAppointmentDialog(LocalDate date, Appointment appointment, Object owner) {
        boolean isNew = (appointment == null);
        Appointment targetApp = isNew ? new Appointment("", "", owner) : appointment;
        AppointmentDialog dialog = new AppointmentDialog(frame, targetApp, isNew);
        dialog.setVisible(true);
        int result = dialog.getResult();
        if (result == AppointmentDialog.OPTION_SAVE) {
            if(!isNew) {
                removeAppointment(date, appointment);
            }
            addOrUpdateAppointment(date, dialog.getAppointment());
        } else if (result == AppointmentDialog.OPTION_DELETE) {
            removeAppointment(date, appointment);
        }
    }
    public void startConsoleListener() {
        Thread consoleListenerThread = new Thread(() -> {
            System.out.println("\n--- コンソールコマンド入力待機中 ---");
            System.out.println("コマンド一覧:");
            System.out.println("  chat <相手名> <メッセージ>           ... 指定した相手としてチャット送信");
            System.out.println("  appoint <相手名> <月> <日> <タイトル> ... 指定した相手の予定を追加");
            System.out.println("  exit                               ... 入力を終了");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String groupName = (this.currentGroup != null) ? this.currentGroup.getName() : "マイページ";
                System.out.print("\n[" + groupName + "] コマンド > ");
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("exit")) break;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(" ", 2);
                String command = parts[0].toLowerCase();
                SwingUtilities.invokeLater(() -> {
                    if (parts.length < 2) {
                        System.out.println("エラー: 引数が不足しています。");
                        return;
                    }
                    switch (command) {
                        case "chat": handleChatCommand(parts[1]); break;
                        case "appoint": handleAppointmentCommand(parts[1]); break;
                        default: System.out.println("エラー: 不明なコマンドです。'" + command + "'"); break;
                    }
                });
            }
            scanner.close();
            System.out.println("コンソールリスナーを終了しました。");
        });
        consoleListenerThread.setDaemon(true);
        consoleListenerThread.start();
    }
    private void handleChatCommand(String argsString) {
        if (this.currentGroup == null) {
            System.out.println("エラー: チャットを送信するグループを選択してください。");
            return;
        }
        String[] args = argsString.split(" ", 2);
        if (args.length < 2) {
            System.out.println("エラー: 引数が不足しています。例: chat 田中 こんにちは");
            return;
        }
        String opponentName = args[0];
        String message = args[1];
        User opponent = findUserInCurrentGroup(opponentName);
        if (opponent != null) {
            ChatMessage chatMessage = new ChatMessage(opponent, message, LocalDateTime.now());
            this.currentGroup.addMessage(chatMessage);
            this.addMessageToUI(chatMessage, this.currentGroup);
            System.out.println("（" + opponent.getName() + "としてメッセージを送信しました）");
        } else {
            System.out.println("エラー: グループ内に「" + opponentName + "」さんはいません。");
        }
    }
    private void handleAppointmentCommand(String argsString) {
        String[] args = argsString.split(" ", 4);
        if (args.length < 4) {
            System.out.println("エラー: 引数が不足しています。例: appoint 田中 12 25 準備");
            return;
        }
        try {
            String opponentName = args[0];
            int month = Integer.parseInt(args[1]);
            int day = Integer.parseInt(args[2]);
            String title = args[3];
            User opponent = findUserInCurrentGroup(opponentName);
            if (opponent == null) {
                System.out.println("エラー: グループ内に「" + opponentName + "」さんはいません。");
                return;
            }
            LocalDate targetDate = LocalDate.of(currentDate.getYear(), month, day);
            Appointment app = new Appointment(title, "", opponent);
            addOrUpdateAppointment(targetDate, app);
            System.out.println("（" + opponent.getName() + "の予定として" + month + "月" + day + "日に「" + title + "」を追加しました）");
            if (currentDate.getMonthValue() != month) {
                currentDate = currentDate.withMonth(month);
                updateCalendar();
            }
        } catch (Exception e) {
            System.out.println("エラー: 無効な日付またはコマンドです。");
        }
    }
    private User findUserInCurrentGroup(String name) {
        if (currentGroup == null) return null;
        return currentGroup.getUsers().stream()
                .filter(user -> name.equals(user.getName()))
                .findFirst()
                .orElse(null);
    }
    
    // =================================================================
    // 内部クラス定義
    // =================================================================

    static class Group {
        private final String id, name;
        private final char iconChar;
        private final List<User> users = new ArrayList<>();
        private final List<ChatMessage> chatHistory = new ArrayList<>();
        private JPanel messageArea;
        public Group(String id, String name, char iconChar) { this.id = id; this.name = name; this.iconChar = iconChar; }
        public String getId() { return id; }
        public String getName() { return name; }
        public char getIconChar() { return iconChar; }
        public List<User> getUsers() { return users; }
        public List<ChatMessage> getChatHistory() { return chatHistory; }
        public void addUser(User user) { users.add(user); }
        public void addMessage(ChatMessage message) { chatHistory.add(message); }
        public JPanel getMessageArea() { return messageArea; }
        public void setMessageArea(JPanel messageArea) { this.messageArea = messageArea; }
    }
    static class ChatMessage {
        private final User sender;
        private final String text;
        private final LocalDateTime timestamp;
        public ChatMessage(User sender, String text, LocalDateTime timestamp) { this.sender = sender; this.text = text; this.timestamp = timestamp; }
        public User getSender() { return sender; }
        public String getText() { return text; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    static class Appointment {
        private String title, details;
        private Object owner;
        public Appointment(String title, String details, Object owner) { this.title = title; this.details = details; this.owner = owner; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public Object getOwner() { return owner; }
        public void setOwner(Object owner) { this.owner = owner; }
    }
    static class AppointmentDialog extends JDialog {
        public static final int OPTION_SAVE = 1, OPTION_DELETE = 2, OPTION_CANCEL = 0;
        private int result = OPTION_CANCEL;
        private JTextField titleField;
        private JTextArea detailsArea;
        private Object owner;

        public AppointmentDialog(Frame ownerFrame, Appointment appointment, boolean isNew) {
            super(ownerFrame, true);
            this.owner = appointment.getOwner();
            setTitle(isNew ? "予定の新規作成" : "予定の編集/削除");
            titleField = new JTextField(appointment.getTitle(), 20);
            detailsArea = new JTextArea(appointment.getDetails(), 5, 20);
            detailsArea.setLineWrap(true);
            detailsArea.setWrapStyleWord(true);
            
            JLabel ownerLabel = new JLabel();
            ownerLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            ownerLabel.setForeground(Color.GRAY);
            if (this.owner instanceof User) {
                ownerLabel.setText("所有者: " + ((User) this.owner).getName() + " (個人)");
            } else if (this.owner instanceof Group) {
                ownerLabel.setText("所有者: " + ((Group) this.owner).getName() + " (グループ共有)");
            } else {
                ownerLabel.setText("所有者: 不明");
            }
            
            JButton saveButton = new JButton("保存");
            JButton deleteButton = new JButton("削除");
            JButton cancelButton = new JButton("キャンセル");
            deleteButton.setVisible(!isNew);
            saveButton.addActionListener(e -> { result = OPTION_SAVE; dispose(); });
            deleteButton.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(this, "この予定を削除しますか？", "確認", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    result = OPTION_DELETE;
                    dispose();
                }
            });
            cancelButton.addActionListener(e -> { result = OPTION_CANCEL; dispose(); });
            
            JPanel textPanel = new JPanel(new BorderLayout(5, 5));
            textPanel.add(new JLabel("タイトル:"), BorderLayout.NORTH);
            textPanel.add(titleField, BorderLayout.CENTER);
            
            JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            mainPanel.add(textPanel, BorderLayout.NORTH);
            JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
            centerPanel.add(new JLabel("詳細:"), BorderLayout.NORTH);
            centerPanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
            mainPanel.add(centerPanel, BorderLayout.CENTER);
            
            JPanel footerPanel = new JPanel(new BorderLayout());
            footerPanel.add(ownerLabel, BorderLayout.WEST);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(saveButton);
            if (!isNew) buttonPanel.add(deleteButton);
            buttonPanel.add(cancelButton);
            footerPanel.add(buttonPanel, BorderLayout.EAST);
            
            getContentPane().add(mainPanel, BorderLayout.CENTER);
            getContentPane().add(footerPanel, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(ownerFrame);
        }
        public int getResult() { return result; }
        public Appointment getAppointment() { return new Appointment(titleField.getText(), detailsArea.getText(), this.owner); }
    }
}