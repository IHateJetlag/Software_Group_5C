package client;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;

import share.ClientMessage;
import share.ServerMessage;
import share.LoginRequest;
import share.LoginResponse;
import share.RegisterRequest;
import share.RegisterResponse;
import share.ChatRequest;
import share.ChatMessage; // displayNewChatMessage で使用
import share.CreateGroupRequest;
import share.ErrorResponse;
import share.Schedule;    // addSchedule, displayCurrentStatus で使用
import share.Group;       // displayNewChatMessage で使用
import share.UserData;    // currentUserData フィールド、handleUserData で使用
import share.LocalDateTimeAdapter; // GsonBuilder で使用

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson;
    private volatile boolean isConnected;
    private volatile boolean isAuthenticated;
    private String username;
    
    private UserData currentUserData;
    private ExecutorService messageProcessor;
    private String clientDataDir;

    public Client() {
        gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        messageProcessor = Executors.newSingleThreadExecutor();
    }
    
    private synchronized void saveClientData() {
        if (currentUserData == null || clientDataDir == null) return;
        new File(clientDataDir).mkdirs();

        Gson fileSaverGson = new GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting().create();

        try {
            try (FileWriter fw = new FileWriter(clientDataDir + "user.json")) { fileSaverGson.toJson(currentUserData.getUser(), fw); }
            try (FileWriter fw = new FileWriter(clientDataDir + "groups.json")) { fileSaverGson.toJson(currentUserData.getGroups(), fw); }
            try (FileWriter fw = new FileWriter(clientDataDir + "schedules.json")) { fileSaverGson.toJson(currentUserData.getSchedules(), fw); }
            try (FileWriter fw = new FileWriter(clientDataDir + "chats.json")) { fileSaverGson.toJson(currentUserData.getChats(), fw); }
            System.out.println("\n[Client] ローカルデータを保存しました: " + clientDataDir);
        } catch (IOException e) {
            System.err.println("[Client] ローカルデータ保存エラー: " + e.getMessage());
        }
    }
    
    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;
            
            Thread messageReceiver = new Thread(this::receiveMessages);
            messageReceiver.setDaemon(true);
            messageReceiver.start();
            
            System.out.println("サーバーに接続しました。");
            return true;
        } catch (IOException e) {
            System.err.println("サーバー接続エラー: " + e.getMessage());
            return false;
        }
    }
    
    public void disconnect() {
        try {
            isConnected = false;
            if (messageProcessor != null) messageProcessor.shutdownNow();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("サーバーから切断しました。");
        } catch (IOException e) {
            System.err.println("切断エラー: " + e.getMessage());
        }
    }
    
    private void receiveMessages() {
        try {
            String inputLine;
            while (isConnected && (inputLine = reader.readLine()) != null) {
                System.out.println("\n[Client-Receiver] サーバーから受信: " + inputLine.substring(0, Math.min(inputLine.length(), 150)) + "...");
                final String currentInputLine = inputLine;
                if (!messageProcessor.isShutdown()) {
                    messageProcessor.execute(() -> handleServerMessage(currentInputLine));
                }
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("メッセージ受信エラー: " + e.getMessage());
                isConnected = false;
            }
        } finally {
            isAuthenticated = false;
            System.out.println("サーバーとの接続が切れました。");
        }
    }
    
    private void handleServerMessage(String message) {
        try {
            ClientMessage clientMessage = gson.fromJson(message, ClientMessage.class);
            switch (clientMessage.getType()) {
                case "LOGIN_SUCCESS": handleLoginSuccess(clientMessage); break;
                case "LOGIN_FAILED": handleLoginFailed(clientMessage); break;
                case "REGISTER_SUCCESS": handleRegisterSuccess(clientMessage); break;
                case "REGISTER_FAILED": handleRegisterFailed(clientMessage); break;
                case "USER_DATA": handleUserData(clientMessage); break;
                case "CHAT_MESSAGE": handleChatMessage(clientMessage); break; // 差分更新用（今回はUSER_DATAで全更新）
                case "ERROR": handleError(clientMessage); break;
                default: System.out.println("未対応のメッセージタイプ: " + clientMessage.getType());
            }
        } catch (JsonSyntaxException e) {
            System.err.println("無効なJSONメッセージ: " + e.getMessage());
        }
    }
    
    private void handleLoginSuccess(ClientMessage message) {
        LoginResponse response = gson.fromJson(message.getData(), LoginResponse.class);
        isAuthenticated = true;
        this.clientDataDir = "client_data/" + this.username + "/";
        System.out.println("\n[Client] ログイン成功: " + response.getMessage());
    }
    
    private void handleLoginFailed(ClientMessage message) {
        LoginResponse response = gson.fromJson(message.getData(), LoginResponse.class);
        System.out.println("\n[Client] ログイン失敗: " + response.getMessage());
    }
    
    private void handleRegisterSuccess(ClientMessage message) {
        RegisterResponse response = gson.fromJson(message.getData(), RegisterResponse.class);
        System.out.println("\n[Client] 登録成功: " + response.getMessage());
    }
    
    private void handleRegisterFailed(ClientMessage message) {
        RegisterResponse response = gson.fromJson(message.getData(), RegisterResponse.class);
        System.out.println("\n[Client] 登録失敗: " + response.getMessage());
    }
    
    // ★★★ 修正 ★★★ サーバーから送られてきた全データでローカルを上書きする
    private void handleUserData(ClientMessage message) {
        if (message.getData() == null || message.getData().isJsonNull()) {
            System.err.println("[Client] サーバーから空のユーザーデータを受信しました。");
            return;
        }
        currentUserData = gson.fromJson(message.getData(), UserData.class);
        System.out.println("[Client] サーバーから最新の全データを受信・更新しました。");
        System.out.println("  - グループ数: " + currentUserData.getGroups().size());
        System.out.println("  - スケジュール数: " + currentUserData.getSchedules().size());
        System.out.println("  - チャット数: " + currentUserData.getChats().size());
        saveClientData();
        displayCurrentStatus();
    }

    private void handleChatMessage(ClientMessage message) {
        ChatMessage chatMessage = gson.fromJson(message.getData(), ChatMessage.class);
        System.out.println("\n[Client] 新しいチャットメッセージを受信しました。");
        // 全件更新モデルでは、この差分データでなくUSER_DATAで更新される
        // ただし、リアルタイム表示のためにメモリ上のデータには追加しておく
        if (currentUserData != null) {
            currentUserData.getChats().add(chatMessage);
            displayNewChatMessage(chatMessage);
        }
    }
    
    private void handleError(ClientMessage message) {
        ErrorResponse error = gson.fromJson(message.getData(), ErrorResponse.class);
        System.err.println("\n[Client] サーバーエラー: " + error.getMessage());
    }
    
    public boolean login(String username, String password) {
        if (!isConnected) return false;
        this.username = username; 
        return sendMessage(new ServerMessage("LOGIN", gson.toJsonTree(new LoginRequest(username, password))));
    }
    
    public boolean register(String username, String password, String email) { // メソッドの引数 email はCUIの入力で受け取っている
    if (!isConnected) return false;
    return sendMessage(new ServerMessage("REGISTER", gson.toJsonTree(new RegisterRequest(username, password))));
}
    
    public boolean sendChat(String groupId, String messageText) {
        if (!isConnected || !isAuthenticated) return false;
        return sendMessage(new ServerMessage("SEND_CHAT", gson.toJsonTree(new ChatRequest(groupId, messageText))));
    }
    
    public boolean addSchedule(Schedule schedule) {
        if (!isConnected || !isAuthenticated) return false;
        return sendMessage(new ServerMessage("ADD_SCHEDULE", gson.toJsonTree(schedule)));
    }
    
    public boolean createGroup(String groupName, List<String> members) {
        if (!isConnected || !isAuthenticated) return false;
        return sendMessage(new ServerMessage("CREATE_GROUP", gson.toJsonTree(new CreateGroupRequest(groupName, members))));
    }
    
    public boolean requestUserData() {
        if (!isConnected || !isAuthenticated) return false;
        System.out.println("[Client] サーバーにデータ更新をリクエストします...");
        return sendMessage(new ServerMessage("GET_USER_DATA", null));
    }
    
    private boolean sendMessage(ServerMessage message) {
        try {
            writer.println(gson.toJson(message));
            return true;
        } catch (Exception e) {
            System.err.println("メッセージ送信エラー: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isAuthenticated() { return isAuthenticated; }
    
    private void displayCurrentStatus() {
        if (currentUserData == null) return;
        System.out.println("\n--- 現在のあなたの情報 ---");
        System.out.println("ようこそ, " + currentUserData.getUser().getUsername() + " さん");
        System.out.println("【所属グループ】: " + currentUserData.getGroups().size() + "件");
        currentUserData.getGroups().forEach(g -> System.out.println(" - " + g.getName() + " (ID: " + g.getId() + ")"));
        System.out.println("【あなたのスケジュール】: " + currentUserData.getSchedules().size() + "件");
        currentUserData.getSchedules().stream()
            .sorted(Comparator.comparing(Schedule::getStartTime))
            .forEach(s -> System.out.println(" - " + s.getStartTime() + " " + s.getTitle()));
        System.out.println("--------------------------");
    }

    private void displayNewChatMessage(ChatMessage chat) {
        Optional<Group> groupOpt = (currentUserData != null) ? currentUserData.getGroups().stream().filter(g -> g.getId().equals(chat.getGroupId())).findFirst() : Optional.empty();
        String groupName = groupOpt.isPresent() ? groupOpt.get().getName() : "不明なグループ";
        System.out.println("＜新着チャット＞ [" + groupName + "] " + chat.getSender() + ": " + chat.getMessage());
    }

    public static void main(String[] args) {
        Client client = new Client();
        if (!client.connect()) return;
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== チャット機能付きスケジュール共有ソフトウェア ===");

        while(!client.isAuthenticated()) {
            System.out.print("\n1. ログイン | 2. 新規登録 | 3. 終了 > ");
            String choice = scanner.nextLine();
            if ("1".equals(choice)) {
                System.out.print("ユーザー名: "); String username = scanner.nextLine();
                System.out.print("パスワード: "); String password = scanner.nextLine();
                client.login(username, password);
            } else if ("2".equals(choice)) {
                System.out.print("ユーザー名: "); String username = scanner.nextLine();
                System.out.print("パスワード: "); String password = scanner.nextLine();
                System.out.print("メールアドレス: "); String email = scanner.nextLine();
                client.register(username, password, email);
            } else if ("3".equals(choice)) {
                client.disconnect(); return;
            }
            try { Thread.sleep(1500); } catch (InterruptedException e) {}
        }

        while(client.isAuthenticated()) {
            System.out.print("\n=== メニュー ===\n1. チャット送信 | 2. スケジュール追加 | 3. グループ作成 | 4. データ更新・表示 | 5. 終了 > ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    System.out.print("送信先のグループID: "); String groupId = scanner.nextLine();
                    System.out.print("メッセージ: "); String message = scanner.nextLine();
                    client.sendChat(groupId, message);
                    break;
                case "2":
                    Schedule schedule = new Schedule();
                    System.out.print("タイトル: "); schedule.setTitle(scanner.nextLine());
                    System.out.print("説明: "); schedule.setDescription(scanner.nextLine());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    try {
                        System.out.print("開始日時 (yyyy-MM-dd HH:mm): "); schedule.setStartTime(LocalDateTime.parse(scanner.nextLine(), formatter));
                        System.out.print("終了日時 (yyyy-MM-dd HH:mm): "); schedule.setEndTime(LocalDateTime.parse(scanner.nextLine(), formatter));
                    } catch (Exception e) { System.err.println("日時の形式が不正です。"); continue; }
                    System.out.print("グループID (個人の予定なら空): "); String scheduleGroupId = scanner.nextLine();
                    if(scheduleGroupId != null && !scheduleGroupId.trim().isEmpty()) schedule.setGroupId(scheduleGroupId);
                    else {
                        System.out.print("非公開にしますか？(y/n): "); String p = scanner.nextLine();
                        if("y".equalsIgnoreCase(p)) schedule.setPrivate(true);
                    }
                    client.addSchedule(schedule);
                    break;
                case "3":
                    System.out.print("グループ名: "); String groupName = scanner.nextLine();
                    System.out.print("メンバーのユーザー名をカンマ区切りで入力: ");
                    List<String> members = new ArrayList<>(Arrays.asList(scanner.nextLine().split(",")));
                    client.createGroup(groupName, members);
                    break;
                case "4": client.requestUserData(); break;
                case "5": client.disconnect(); return;
                default: System.out.println("無効な選択です。");
            }
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        scanner.close();
    }
}