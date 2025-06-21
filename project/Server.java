import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Server {
    private static final int PORT = 8080;
    private static final String DATA_DIR = "data/";
    private static final String USERS_FILE = DATA_DIR + "users.json";
    private static final String GROUPS_FILE = DATA_DIR + "groups.json";
    private static final String SCHEDULES_FILE = DATA_DIR + "schedules.json";
    private static final String CHATS_FILE = DATA_DIR + "chats.json";
    
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> connectedClients;
    private Map<String, User> users;
    private Map<String, Group> groups;
    private List<Schedule> schedules;
    private List<ChatMessage> chatMessages;
    private Gson gson;
    private ExecutorService threadPool;
    
    public Server() {
        connectedClients = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        schedules = Collections.synchronizedList(new ArrayList<>());
        chatMessages = Collections.synchronizedList(new ArrayList<>());
        gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting() // ★★★ 修正 ★★★ 保存されるJSONを読みやすくする
            .create();
        threadPool = Executors.newCachedThreadPool();
        
        new File(DATA_DIR).mkdirs();
        loadData();
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("サーバーが開始されました。ポート: " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("サーバーエラー: " + e.getMessage());
        }
    }
    
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            saveData();
        } catch (IOException e) {
            System.err.println("サーバー停止エラー: " + e.getMessage());
        }
    }
    
    private void loadData() {
        try {
            File usersFile = new File(USERS_FILE);
            if (usersFile.exists()) {
                Type userMapType = new TypeToken<Map<String, User>>(){}.getType();
                users = gson.fromJson(new FileReader(usersFile), userMapType);
                if (users == null) users = new ConcurrentHashMap<>();
            }
            
            File groupsFile = new File(GROUPS_FILE);
            if (groupsFile.exists()) {
                Type groupMapType = new TypeToken<Map<String, Group>>(){}.getType();
                groups = gson.fromJson(new FileReader(groupsFile), groupMapType);
                if (groups == null) groups = new ConcurrentHashMap<>();
            }
            
            File schedulesFile = new File(SCHEDULES_FILE);
            if (schedulesFile.exists()) {
                Type scheduleListType = new TypeToken<List<Schedule>>(){}.getType();
                List<Schedule> loadedSchedules = gson.fromJson(new FileReader(schedulesFile), scheduleListType);
                if (loadedSchedules != null) schedules.addAll(loadedSchedules);
            }
            
            File chatsFile = new File(CHATS_FILE);
            if (chatsFile.exists()) {
                Type chatListType = new TypeToken<List<ChatMessage>>(){}.getType();
                List<ChatMessage> loadedChats = gson.fromJson(new FileReader(chatsFile), chatListType);
                if (loadedChats != null) chatMessages.addAll(loadedChats);
            }
            System.out.println("データが読み込まれました。");
        } catch (IOException e) {
            System.err.println("データ読み込みエラー: " + e.getMessage());
        }
    }
    
    public synchronized void saveData() {
        try {
            try (FileWriter writer = new FileWriter(USERS_FILE)) { gson.toJson(users, writer); }
            try (FileWriter writer = new FileWriter(GROUPS_FILE)) { gson.toJson(groups, writer); }
            try (FileWriter writer = new FileWriter(SCHEDULES_FILE)) { gson.toJson(schedules, writer); }
            try (FileWriter writer = new FileWriter(CHATS_FILE)) { gson.toJson(chatMessages, writer); }
            System.out.println("データが保存されました。");
        } catch (IOException e) {
            System.err.println("データ保存エラー: " + e.getMessage());
        }
    }
    
    public synchronized boolean authenticateUser(String username, String password) {
        User user = users.get(username.toLowerCase());
        return user != null && user.getPassword().equals(password);
    }
    
    public synchronized boolean registerUser(String username, String password, String email) {
        String lowerCaseUsername = username.toLowerCase();
        if (users.containsKey(lowerCaseUsername)) {
            return false;
        }
        User newUser = new User(username, password, email);
        users.put(lowerCaseUsername, newUser);
        saveData();
        return true;
    }
    
    public synchronized void addClient(String username, ClientHandler clientHandler) {
        connectedClients.put(username.toLowerCase(), clientHandler);
        System.out.println("ユーザー " + username + " が接続しました。(" + connectedClients.size() + "人接続中)");
    }
    
    public synchronized void removeClient(String username) {
        if (username != null) {
            connectedClients.remove(username.toLowerCase());
            System.out.println("ユーザー " + username + " の接続が切断されました。(" + connectedClients.size() + "人接続中)");
        }
    }
    
    // ★★★ 修正 ★★★ ユーザーの全データを取得するロジックを改善
    public synchronized UserData getUserData(String username) {
        final String lowerCaseUsername = username.toLowerCase();
        User user = users.get(lowerCaseUsername);
        if (user == null) {
            System.err.println("[Server-getUserData] エラー: ユーザーが見つかりません: " + username);
            return null;
        }

        // ユーザーが所属するグループを取得
        final List<Group> userGroups = groups.values().stream()
            .filter(g -> g.getMembers().stream().anyMatch(m -> m.equalsIgnoreCase(lowerCaseUsername)))
            .collect(Collectors.toList());

        // ユーザーに関連するスケジュールを取得 (個人スケジュール + 所属グループのスケジュール)
        final Set<String> userGroupIds = userGroups.stream().map(Group::getId).collect(Collectors.toSet());
        final List<Schedule> userSchedules = schedules.stream()
            .filter(s -> 
                // 自分が参加者に含まれるか
                s.getParticipants().stream().anyMatch(p -> p.equalsIgnoreCase(lowerCaseUsername)) ||
                // または、所属するグループのスケジュールか
                (s.getGroupId() != null && userGroupIds.contains(s.getGroupId()))
            )
            .distinct()
            .collect(Collectors.toList());

        // ユーザーが所属するグループのチャットをすべて取得
        final List<ChatMessage> userChats = chatMessages.stream()
            .filter(c -> c.getGroupId() != null && userGroupIds.contains(c.getGroupId()))
            .collect(Collectors.toList());
        
        System.out.println("[Server-getUserData] ユーザー '" + username + "' のデータを集計しました: ");
        System.out.println("  - グループ数: " + userGroups.size());
        System.out.println("  - スケジュール数: " + userSchedules.size());
        System.out.println("  - チャット数: " + userChats.size());
        
        return new UserData(user, userGroups, userSchedules, userChats);
    }
    
    public synchronized void addChatMessage(String senderUsername, String groupId, String message) {
        Group targetGroup = groups.get(groupId);
        if (targetGroup == null) {
            System.err.println("[Server-addChat] エラー: グループが見つかりません: " + groupId);
            return;
        }

        ChatMessage chatMessage = new ChatMessage(UUID.randomUUID().toString(), senderUsername, groupId, message, LocalDateTime.now());
        chatMessages.add(chatMessage);
        saveData();
        
        // グループメンバー全員に新しいチャットメッセージをブロードキャスト
        broadcastToUsers(targetGroup.getMembers(), "CHAT_MESSAGE", chatMessage);
    }
    
    // ★★★ 修正 ★★★ スケジュール追加のロジックを改善
    public synchronized void addSchedule(String createdByUsername, Schedule clientSchedule) {
        Schedule newSchedule = new Schedule();
        newSchedule.setId(UUID.randomUUID().toString());
        newSchedule.setTitle(clientSchedule.getTitle());
        newSchedule.setDescription(clientSchedule.getDescription());
        newSchedule.setStartTime(clientSchedule.getStartTime());
        newSchedule.setEndTime(clientSchedule.getEndTime());
        newSchedule.setCreatedBy(createdByUsername);
        newSchedule.setCreatedAt(LocalDateTime.now());
        
        List<String> participants = new ArrayList<>();
        String groupId = clientSchedule.getGroupId();

        // グループスケジュールの場合
        if (groupId != null && !groupId.trim().isEmpty()) {
            Group group = groups.get(groupId);
            if (group != null) {
                newSchedule.setGroupId(groupId);
                participants.addAll(group.getMembers()); // 参加者はグループメンバー全員
                newSchedule.setPrivate(false); // グループスケジュールは常に公開
                System.out.println("[Server-addSchedule] グループスケジュール '" + newSchedule.getTitle() + "' を作成。参加者: " + group.getMembers().size() + "人");
            } else {
                System.out.println("[Server-addSchedule] 警告: グループID '" + groupId + "' が見つかりません。個人スケジュールとして扱います。");
                participants.add(createdByUsername); // 作成者のみ参加
                newSchedule.setPrivate(clientSchedule.isPrivate());
            }
        } 
        // 個人スケジュールの場合
        else {
            participants.add(createdByUsername); // 作成者のみ参加
            newSchedule.setPrivate(clientSchedule.isPrivate());
            System.out.println("[Server-addSchedule] 個人スケジュール '" + newSchedule.getTitle() + "' を作成。");
        }
        
        newSchedule.setParticipants(participants.stream().distinct().collect(Collectors.toList()));
        schedules.add(newSchedule);
        saveData();

        // ★★★ 修正 ★★★ 関係者全員に新しいスケジュール情報を送信 (差分更新)
        // broadcastToUsers(newSchedule.getParticipants(), "SCHEDULE_ADDED", newSchedule);
        
        // ★★★ 修正 ★★★ 関係者全員に、更新された全ユーザーデータを送信 (全件更新)
        // これにより、クライアント側のデータ同期がより確実になります。
        System.out.println("[Server-addSchedule] 関係者に最新のユーザーデータを送信します。対象者: " + newSchedule.getParticipants());
        notifyUserDataUpdate(newSchedule.getParticipants());
    }
    
    // ★★★ 修正 ★★★ グループ作成ロジックを改善
    public synchronized void createGroup(String groupName, String createdBy, List<String> members) {
        String groupId = UUID.randomUUID().toString();
        
        // 作成者もメンバーに含め、全員のユーザー名を小文字に統一し、重複を排除
        List<String> lowerCaseMembers = members.stream()
                                               .map(String::toLowerCase)
                                               .collect(Collectors.toList());
        if(!lowerCaseMembers.contains(createdBy.toLowerCase())) {
            lowerCaseMembers.add(createdBy.toLowerCase());
        }
        
        Group group = new Group(groupId, groupName, createdBy, lowerCaseMembers.stream().distinct().collect(Collectors.toList()), LocalDateTime.now());
        groups.put(groupId, group);
        saveData();
        
        System.out.println("[Server-createGroup] グループ '" + groupName + "' が作成されました。メンバー: " + group.getMembers());
        
        // ★★★ 修正 ★★★ 関係者全員に、更新された全ユーザーデータを送信
        notifyUserDataUpdate(group.getMembers());
    }

    // ★★★ 追加 ★★★ 指定したユーザーリストに最新のUserDataを送信するヘルパーメソッド
    public void notifyUserDataUpdate(List<String> usernames) {
        Set<String> distinctUsernames = new HashSet<>(usernames);
        for (String username : distinctUsernames) {
            ClientHandler client = connectedClients.get(username.toLowerCase());
            if (client != null) {
                System.out.println("[Server-notify] オンラインユーザー '" + username + "' に最新データを送信します。");
                UserData userData = getUserData(username);
                if (userData != null) {
                    client.sendMessage("USER_DATA", userData);
                }
            } else {
                 System.out.println("[Server-notify] ユーザー '" + username + "' はオフラインのためスキップします。");
            }
        }
    }
    
    private void broadcastToUsers(List<String> usernames, String messageType, Object data) {
        System.out.println("[Server-Broadcast] '" + messageType + "' をブロードキャストします。対象者: " + usernames);
        Set<String> distinctUsernames = new HashSet<>(usernames);
        for (String username : distinctUsernames) {
            ClientHandler client = connectedClients.get(username.toLowerCase());
            if (client != null) {
                client.sendMessage(messageType, data);
            }
        }
    }
    
    public static void main(String[] args) {
        Server server = new Server();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("サーバーを停止中...");
            server.stop();
        }));
        server.start();
    }
}