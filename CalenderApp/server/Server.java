package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter; // このファイルでは直接使っていない

// share パッケージのクラスをインポート
import share.User;
import share.Group;
import share.Schedule;
import share.ChatMessage;
import share.UserData;
import share.LocalDateTimeAdapter;
import share.ErrorResponse;

public class Server {
    private static final int PORT = 8080;
    private static final String DATA_DIR = "data/"; // データ保存ディレクトリ
    private static final String USERS_FILE = DATA_DIR + "users.json";
    private static final String GROUPS_FILE = DATA_DIR + "groups.json";
    private static final String SCHEDULES_FILE = DATA_DIR + "schedules.json";
    private static final String CHATS_FILE = DATA_DIR + "chats.json";

    private ServerSocket serverSocket;
    private Map<String, ClientHandler> connectedClients; // 接続中のクライアント (username -> ClientHandler)
    private Map<String, User> users;                     // 全ユーザー (username -> User)
    private Map<String, Group> groups;                   // 全グループ (groupId -> Group)
    private List<Schedule> schedules;                  // 全スケジュール
    private List<ChatMessage> chatMessages;            // 全チャットメッセージ
    private Gson gson;
    private ExecutorService threadPool; // クライアント処理用スレッドプール

    public Server() {
        connectedClients = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        schedules = Collections.synchronizedList(new ArrayList<>());
        chatMessages = Collections.synchronizedList(new ArrayList<>());
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new share.LocalDateTimeAdapter()) // share.LocalDateTimeAdapter
            .setPrettyPrinting()
            .create();
        threadPool = Executors.newCachedThreadPool();

        new File(DATA_DIR).mkdirs(); // データディレクトリ作成
        loadData();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("サーバーがポート " + PORT + " で起動しました。クライアントの接続を待機中...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("新しいクライアントが接続しました: " + clientSocket.getRemoteSocketAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this); // server.ClientHandler
                    threadPool.execute(clientHandler);
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("サーバーソケットが閉じられたため、新規接続の受付を停止します。");
                        break;
                    }
                    System.err.println("クライアント接続受付エラー: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("サーバー起動エラー (ポート " + PORT + "): " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        System.out.println("サーバーを停止処理中...");
        try {
            // 全ての接続中クライアントにサーバーシャットダウンを通知（任意）
            for (ClientHandler handler : connectedClients.values()) {
                handler.sendMessage("SERVER_SHUTDOWN", "サーバーがシャットダウンします。");
            }
            connectedClients.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            saveData();
            System.out.println("サーバーが停止しました。");
        } catch (IOException e) {
            System.err.println("サーバー停止中のエラー: " + e.getMessage());
        }
    }

    private void loadData() {
        System.out.println("データをファイルから読み込んでいます...");
        users = loadFromFile(USERS_FILE, new TypeToken<ConcurrentHashMap<String, User>>(){}.getType(), new ConcurrentHashMap<>());
        groups = loadFromFile(GROUPS_FILE, new TypeToken<ConcurrentHashMap<String, Group>>(){}.getType(), new ConcurrentHashMap<>());
        
        List<Schedule> loadedSchedules = loadFromFile(SCHEDULES_FILE, new TypeToken<ArrayList<Schedule>>(){}.getType(), new ArrayList<>());
        if (loadedSchedules != null) schedules.addAll(loadedSchedules);
        
        List<ChatMessage> loadedChats = loadFromFile(CHATS_FILE, new TypeToken<ArrayList<ChatMessage>>(){}.getType(), new ArrayList<>());
        if (loadedChats != null) chatMessages.addAll(loadedChats);
        
        System.out.println("データの読み込み完了。 User:" + users.size() + " Group:" + groups.size() + " Schedule:" + schedules.size() + " Chat:" + chatMessages.size());
    }
    
    private <T> T loadFromFile(String filePath, Type type, T defaultValue) {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                T data = gson.fromJson(reader, type);
                return data != null ? data : defaultValue;
            } catch (IOException | JsonSyntaxException e) {
                System.err.println("ファイル読み込みエラー (" + filePath + "): " + e.getMessage());
            }
        }
        return defaultValue;
    }

    public synchronized void saveData() {
        System.out.println("データをファイルに保存しています...");
        saveToFile(USERS_FILE, users);
        saveToFile(GROUPS_FILE, groups);
        saveToFile(SCHEDULES_FILE, schedules);
        saveToFile(CHATS_FILE, chatMessages);
        System.out.println("データの保存完了。");
    }

    private synchronized void saveToFile(String filePath, Object data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("ファイル保存エラー (" + filePath + "): " + e.getMessage());
        }
    }

    public synchronized User authenticateUser(String username, String password) { // 戻り値を share.User に
        User user = users.get(username.toLowerCase()); // share.User
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public synchronized boolean registerUser(String username, String password) { // email引数を削除
        String lowerCaseUsername = username.toLowerCase();
        if (users.containsKey(lowerCaseUsername)) {
            return false; // ユーザー名が既に存在
        }
        User newUser = new User(username, password); // share.User (emailなしコンストラクタ)
        users.put(lowerCaseUsername, newUser);
        saveData(); // データ変更後に保存
        return true;
    }

    public synchronized void addClient(String username, ClientHandler clientHandler) {
        connectedClients.put(username.toLowerCase(), clientHandler);
        System.out.println("ユーザー '" + username + "' がシステムに接続しました。現在 " + connectedClients.size() + " 人がオンラインです。");
    }

    public synchronized void removeClient(String username) {
        if (username != null) {
            ClientHandler removed = connectedClients.remove(username.toLowerCase());
            if (removed != null) {
                System.out.println("ユーザー '" + username + "' がシステムから切断しました。現在 " + connectedClients.size() + " 人がオンラインです。");
            }
        }
    }

    public synchronized UserData getUserData(String username) { // 戻り値が share.UserData
        String lowerCaseUsername = username.toLowerCase();
        User user = users.get(lowerCaseUsername); // share.User
        if (user == null) {
            System.err.println("[Server.getUserData] ユーザーが見つかりません: " + username);
            return null; // または空のUserDataを返すか、エラーをスロー
        }

        List<Group> userGroups = groups.values().stream() // share.Group
            .filter(g -> g.getMembers().stream().anyMatch(memberUsername -> memberUsername.equalsIgnoreCase(lowerCaseUsername)))
            .collect(Collectors.toList());

        Set<String> userGroupIds = userGroups.stream().map(Group::getId).collect(Collectors.toSet());

        List<Schedule> userSchedules = schedules.stream() // share.Schedule
            .filter(s -> 
                // 自分が作成者か、自分が参加者リストに含まれるか
                s.getCreatedBy().equalsIgnoreCase(lowerCaseUsername) ||
                (s.getParticipants() != null && s.getParticipants().stream().anyMatch(p -> p.equalsIgnoreCase(lowerCaseUsername))) ||
                // または、自分が所属するグループのスケジュールか (groupIdが設定されていれば)
                (s.getGroupId() != null && !s.getGroupId().isEmpty() && userGroupIds.contains(s.getGroupId()))
            )
            .distinct() // 重複を排除
            .collect(Collectors.toList());

        List<ChatMessage> userChats = chatMessages.stream() // share.ChatMessage
            .filter(c -> c.getGroupId() != null && userGroupIds.contains(c.getGroupId())) // 所属グループのチャットのみ
            .sorted(Comparator.comparing(ChatMessage::getTimestamp)) // 時系列順にソート
            .collect(Collectors.toList());
            
        return new UserData(user, userGroups, userSchedules, userChats); // share.UserData
    }

    public synchronized void addChatMessage(String senderUsername, String groupId, String message) {
        Group targetGroup = groups.get(groupId); // share.Group
        if (targetGroup == null) {
            ClientHandler senderHandler = connectedClients.get(senderUsername.toLowerCase());
            if (senderHandler != null) {
                senderHandler.sendMessage("ERROR", new ErrorResponse("指定されたグループID '" + groupId + "' は存在しません。"));
            }
            System.err.println("[Server.addChatMessage] 送信者 '" + senderUsername + "' のチャット送信エラー: グループID '" + groupId + "' が見つかりません。");
            return;
        }
        if (!targetGroup.getMembers().stream().anyMatch(m -> m.equalsIgnoreCase(senderUsername))) {
            ClientHandler senderHandler = connectedClients.get(senderUsername.toLowerCase());
            if (senderHandler != null) {
                senderHandler.sendMessage("ERROR", new ErrorResponse("あなたはこのグループ '" + targetGroup.getName() + "' のメンバーではありません。"));
            }
            System.err.println("[Server.addChatMessage] 送信者 '" + senderUsername + "' はグループ '" + targetGroup.getName() + "' のメンバーではありません。");
            return;
        }

        ChatMessage chatMessage = new ChatMessage(UUID.randomUUID().toString(), senderUsername, groupId, message, LocalDateTime.now()); // share.ChatMessage
        chatMessages.add(chatMessage);
        saveData(); // データ変更後に保存

        // 該当グループのオンラインメンバーに新しいチャットメッセージをブロードキャスト
        broadcastToUsers(targetGroup.getMembers(), "CHAT_MESSAGE", chatMessage);
    }

    public synchronized void addSchedule(String createdByUsername, Schedule clientScheduleData) { // 引数が share.Schedule
        Schedule newSchedule = new Schedule(); // share.Schedule
        newSchedule.setId(UUID.randomUUID().toString());
        newSchedule.setTitle(clientScheduleData.getTitle());
        newSchedule.setDescription(clientScheduleData.getDescription());
        newSchedule.setStartTime(clientScheduleData.getStartTime());
        newSchedule.setEndTime(clientScheduleData.getEndTime());
        newSchedule.setAllDay(clientScheduleData.isAllDay()); // isAllDayフラグをセット
        newSchedule.setPrivate(clientScheduleData.isPrivate()); // isPrivateフラグをセット
        newSchedule.setCreatedBy(createdByUsername);
        // createdAt は share.Schedule のコンストラクタやsetterで設定される想定 (もしなければここで LocalDateTime.now())
        // newSchedule.setCreatedAt(LocalDateTime.now()); // Scheduleクラス側で設定されていなければ

        List<String> participants = new ArrayList<>();
        String groupId = clientScheduleData.getGroupId();

        if (groupId != null && !groupId.trim().isEmpty()) { // グループスケジュールの場合
            Group group = groups.get(groupId); // share.Group
            if (group != null && group.getMembers().stream().anyMatch(m -> m.equalsIgnoreCase(createdByUsername))) {
                newSchedule.setGroupId(groupId);
                participants.addAll(group.getMembers()); // グループメンバー全員が参加者
                newSchedule.setPrivate(false); // グループスケジュールは強制的に公開
            } else {
                // グループが存在しない、または作成者がメンバーでない場合は個人スケジュール扱い
                System.err.println("[Server.addSchedule] グループID '" + groupId + "' が無効か、作成者がメンバーではありません。個人スケジュールとして登録します。");
                participants.add(createdByUsername);
            }
        } else { // 個人スケジュールの場合
            participants.add(createdByUsername);
        }
        newSchedule.setParticipants(participants.stream().distinct().collect(Collectors.toList()));

        schedules.add(newSchedule);
        saveData(); // データ変更後に保存

        // 関係するオンラインユーザーに最新のUserDataを送信して同期
        notifyUserDataUpdateToRelevantUsers(newSchedule);
    }

     public synchronized String createGroup(String groupName, String createdByUsername, List<String> memberUsernames) {
        String groupId = "grp_" + UUID.randomUUID().toString().substring(0, 8);

        // 作成者をメンバーリストに追加し、全員のユーザー名を小文字に統一し、重複を排除
        Set<String> finalMemberSet = new HashSet<>();
        if (memberUsernames != null) {
            memberUsernames.stream()
                .filter(Objects::nonNull) // nullチェックを追加
                .map(String::toLowerCase)
                .forEach(finalMemberSet::add);
        }
        finalMemberSet.add(createdByUsername.toLowerCase());

        // 存在するユーザーか確認し、有効なメンバーのリストを作成
        List<String> validMembers = finalMemberSet.stream()
                                              .filter(username -> users.containsKey(username)) // usersはMap<String, share.User>
                                              .collect(Collectors.toList());

        // 有効なメンバーがいない、または作成者が有効なメンバーに含まれていない場合はエラー
        if (validMembers.isEmpty() || !validMembers.contains(createdByUsername.toLowerCase())) {
            System.err.println("[Server.createGroup] グループ作成失敗: 作成者 '" + createdByUsername + "' または有効なメンバーがいません。入力されたメンバー: " + memberUsernames + ", 有効なメンバー候補: " + finalMemberSet + ", 最終的な有効メンバー: " + validMembers);
            ClientHandler handler = connectedClients.get(createdByUsername.toLowerCase());
            if (handler != null) {
                // ★★★ ここを修正 ★★★
                // ClientHandlerのsendMessageを使ってErrorResponseを送信
                handler.sendMessage("ERROR", new share.ErrorResponse("グループ作成失敗: 有効なメンバーが見つからないか、作成者がメンバーに含まれていません。"));
            }
            return null; // グループ作成失敗
        }

        // share.Group オブジェクトを作成
        Group newGroup = new Group(groupId, groupName, createdByUsername, new ArrayList<>(validMembers));
        // newGroup.setCreatedAt(LocalDateTime.now()); // Groupコンストラクタで設定するなら不要

        groups.put(groupId, newGroup); // groupsはMap<String, share.Group>
        saveData(); // データ変更後に保存

        System.out.println("[Server] グループ '" + groupName + "' (ID: " + groupId + ") がユーザー '" + createdByUsername + "' によって作成されました。メンバー: " + validMembers);

        // 関係するオンラインユーザーに最新のUserDataを送信して同期
        notifyUserDataUpdate(validMembers);
        return groupId; // 作成されたグループのIDを返す
    }

    // 特定のユーザーリストに最新のUserDataを送信する
    public void notifyUserDataUpdate(List<String> usernamesToNotify) {
        if (usernamesToNotify == null) return;
        Set<String> distinctUsernames = new HashSet<>(usernamesToNotify.stream().map(String::toLowerCase).collect(Collectors.toList()));

        for (String username : distinctUsernames) {
            ClientHandler clientHandler = connectedClients.get(username);
            if (clientHandler != null) {
                UserData userData = getUserData(username); // サーバー側の最新データを取得
                if (userData != null) {
                    System.out.println("[Server.notifyUserDataUpdate] ユーザー '" + username + "' にUserDataを送信します。");
                    clientHandler.sendMessage("USER_DATA", userData);
                }
            }
        }
    }
    
    // スケジュールに関係する全ユーザーにUserData更新を通知
    private void notifyUserDataUpdateToRelevantUsers(Schedule schedule) {
        Set<String> relevantUsernames = new HashSet<>();
        if (schedule.getCreatedBy() != null) {
            relevantUsernames.add(schedule.getCreatedBy().toLowerCase());
        }
        if (schedule.getParticipants() != null) {
            schedule.getParticipants().stream().map(String::toLowerCase).forEach(relevantUsernames::add);
        }
        if (schedule.getGroupId() != null && groups.containsKey(schedule.getGroupId())) {
            groups.get(schedule.getGroupId()).getMembers().stream().map(String::toLowerCase).forEach(relevantUsernames::add);
        }
        notifyUserDataUpdate(new ArrayList<>(relevantUsernames));
    }


    // 特定のユーザーリストに特定のメッセージをブロードキャストする
    private void broadcastToUsers(List<String> usernames, String messageType, Object data) {
        if (usernames == null) return;
        System.out.println("[Server.broadcastToUsers] '" + messageType + "' をユーザー " + usernames + " にブロードキャストします。");
        Set<String> distinctUsernames = new HashSet<>(usernames.stream().map(String::toLowerCase).collect(Collectors.toList()));
        for (String username : distinctUsernames) {
            ClientHandler clientHandler = connectedClients.get(username);
            if (clientHandler != null) {
                clientHandler.sendMessage(messageType, data);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        // シャットダウンフック: Ctrl+C などで終了した際にデータを保存する
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("シャットダウンフック: サーバーを安全に停止します...");
            server.stop();
        }));
        server.start(); // サーバー処理を開始
    }
}