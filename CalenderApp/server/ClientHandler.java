package server;

import java.io.*;
import java.net.*;
import com.google.gson.*;
import java.util.Arrays;
import java.util.List; // List を使う可能性があるので念のため

// share パッケージのクラスを個別にインポート
import share.ServerMessage;
import share.ClientMessage;
import share.LoginRequest;
import share.LoginResponse;
import share.RegisterRequest;
import share.RegisterResponse;
import share.ChatRequest;
import share.CreateGroupRequest;
import share.ErrorResponse;
import share.Schedule;
import share.UserData;
import share.User; // Serverクラス経由でUser型を扱う可能性があるため
import share.LocalDateTimeAdapter; // GsonBuilderで使用

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server; // server.Server を参照
    private BufferedReader reader;
    private PrintWriter writer;
    private String username; // ログインしたユーザーのusername
    private Gson gson;
    private boolean isAuthenticated;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, new share.LocalDateTimeAdapter()) // share.LocalDateTimeAdapter を使用
            .create();
        this.isAuthenticated = false;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("[ClientHandler-" + (socket.getRemoteSocketAddress()) + "] 初期化エラー: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String clientInfo = clientSocket.getRemoteSocketAddress().toString();
        System.out.println("[ClientHandler-" + clientInfo + "] 接続処理開始。");
        try {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.println("[ClientHandler-" + clientInfo + "] " + (username != null ? username : "GUEST") + "から受信: " + inputLine.substring(0, Math.min(inputLine.length(), 150)) + (inputLine.length() > 150 ? "..." : ""));
                handleMessage(inputLine);
            }
        } catch (SocketException e) {
            // クライアントが予期せず切断した場合 (Connection reset, Socket closedなど)
            if (isConnected()) { // isAuthenticated や username がセットされていれば、より丁寧なログも可能
                 System.out.println("[ClientHandler-" + clientInfo + "] " + (username != null ? username : "GUEST") + "との接続が切れました: " + e.getMessage());
            } else {
                 System.out.println("[ClientHandler-" + clientInfo + "] クライアントとの接続が予期せず切れました: " + e.getMessage());
            }
        } catch (IOException e) {
            if (isConnected()) {
                System.err.println("[ClientHandler-" + clientInfo + "] " + (username != null ? username : "GUEST") + "との通信エラー: " + e.getMessage());
            }
        } finally {
            cleanup(clientInfo);
        }
    }

    private void handleMessage(String message) {
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class); // share.ServerMessage
            if (serverMessage == null || serverMessage.getType() == null) {
                sendErrorMessage("無効なメッセージ形式です。");
                return;
            }

            String messageType = serverMessage.getType();
            boolean needsAuth = Arrays.asList("SEND_CHAT", "ADD_SCHEDULE", "CREATE_GROUP", "GET_USER_DATA", "LEAVE_GROUP" /*グループ脱退APIも追加想定*/).contains(messageType);

            if (needsAuth && !isAuthenticated) {
                sendErrorMessage("認証が必要です。まずログインしてください。");
                return;
            }

            switch (messageType) {
                case "LOGIN":
                    handleLogin(serverMessage);
                    break;
                case "REGISTER":
                    handleRegister(serverMessage);
                    break;
                case "SEND_CHAT":
                    handleSendChat(serverMessage);
                    break;
                case "ADD_SCHEDULE":
                    handleAddSchedule(serverMessage);
                    break;
                case "CREATE_GROUP":
                    handleCreateGroup(serverMessage);
                    break;
                case "GET_USER_DATA":
                    handleGetUserData();
                    break;
                // --- 以下、GUIからの機能追加で必要になる可能性のあるAPIハンドラ（スタブ） ---
                // case "GET_MY_GROUPS": // ログインユーザーの所属グループ一覧取得 (UserDataで代替可能か検討)
                //     handleGetMyGroups();
                //     break;
                // case "GET_SCHEDULES_FOR_DATE": //特定の日付のスケジュール取得 (UserDataで代替可能か検討)
                //     handleGetSchedulesForDate(serverMessage);
                //     break;
                // case "UPDATE_SCHEDULE":
                //     handleUpdateSchedule(serverMessage);
                //     break;
                // case "DELETE_SCHEDULE":
                //     handleDeleteSchedule(serverMessage);
                //     break;
                // case "ADD_GROUP_MEMBER":
                //     handleAddGroupMember(serverMessage);
                //     break;
                // case "LEAVE_GROUP":
                //     handleLeaveGroup(serverMessage);
                //     break;
                default:
                    sendErrorMessage("不明なメッセージタイプです: " + messageType);
            }
        } catch (JsonSyntaxException e) {
            sendErrorMessage("無効なJSON形式のメッセージです: " + e.getMessage());
        } catch (Exception e) { // 予期せぬエラーをキャッチ
            sendErrorMessage("サーバー内部エラーが発生しました: " + e.getMessage());
            System.err.println("[ClientHandler] ハンドルメッセージ中の予期せぬエラー (" + username + "): " + e.toString());
            e.printStackTrace();
        }
    }

    private void handleLogin(ServerMessage message) {
        LoginRequest req = gson.fromJson(message.getData(), LoginRequest.class); // share.LoginRequest
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            sendErrorMessage("ログイン情報が不完全です。");
            return;
        }
        share.User authenticatedUser = server.authenticateUser(req.getUsername(), req.getPassword()); // server.Server が share.User を返す

        if (authenticatedUser != null) {
            this.username = authenticatedUser.getUsername();
            this.isAuthenticated = true;
            server.addClient(this.username, this);
            sendMessage("LOGIN_SUCCESS", new LoginResponse(true, "ログインに成功しました。")); // share.LoginResponse
            handleGetUserData(); // ログイン成功後、ユーザーデータを送信
        } else {
            sendMessage("LOGIN_FAILED", new LoginResponse(false, "ユーザー名またはパスワードが間違っています。"));
        }
    }

    private void handleRegister(ServerMessage message) {
        RegisterRequest req = gson.fromJson(message.getData(), RegisterRequest.class); // share.RegisterRequest
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            sendErrorMessage("登録情報が不完全です。");
            return;
        }
        // email は User モデルから削除したので、RegisterRequest も email なしを想定
        if (server.registerUser(req.getUsername(), req.getPassword())) { // server.Server の registerUser も email 引数なしに
            sendMessage("REGISTER_SUCCESS", new RegisterResponse(true, "ユーザー登録に成功しました。"));
        } else {
            sendMessage("REGISTER_FAILED", new RegisterResponse(false, "そのユーザー名は既に使用されているか、登録に失敗しました。"));
        }
    }

    private void handleSendChat(ServerMessage message) {
        ChatRequest req = gson.fromJson(message.getData(), ChatRequest.class); // share.ChatRequest
        if (req == null || req.getGroupId() == null || req.getMessage() == null) {
            sendErrorMessage("チャット情報が不完全です。");
            return;
        }
        server.addChatMessage(this.username, req.getGroupId(), req.getMessage());
        // addChatMessage内でブロードキャストとデータ保存が行われる想定
    }

    private void handleAddSchedule(ServerMessage message) {
        Schedule schedule = gson.fromJson(message.getData(), Schedule.class); // share.Schedule
        if (schedule == null) {
            sendErrorMessage("スケジュール情報が不完全です。");
            return;
        }
        server.addSchedule(this.username, schedule);
        // addSchedule内でデータ保存と関係者への通知が行われる想定
    }

    private void handleCreateGroup(ServerMessage message) {
        CreateGroupRequest req = gson.fromJson(message.getData(), CreateGroupRequest.class); // share.CreateGroupRequest
        if (req == null || req.getGroupName() == null) {
            sendErrorMessage("グループ作成情報が不完全です。");
            return;
        }
        server.createGroup(req.getGroupName(), this.username, req.getMembers());
        // createGroup内でデータ保存と関係者への通知が行われる想定
    }

    private void handleGetUserData() {
        if (!isAuthenticated || this.username == null) {
            sendErrorMessage("ユーザーデータを取得するには認証が必要です。");
            return;
        }
        UserData userData = server.getUserData(this.username); // server.Server が share.UserData を返す
        if (userData != null) {
            sendMessage("USER_DATA", userData);
        } else {
            sendErrorMessage("ユーザーデータの取得に失敗しました。");
        }
    }

    // 他のAPIハンドラ（UPDATE_SCHEDULE, DELETE_SCHEDULE, ADD_GROUP_MEMBER, LEAVE_GROUPなど）も同様に実装していく

    public void sendMessage(String type, Object data) {
        if (writer == null || clientSocket.isClosed()) {
            System.err.println("[ClientHandler] " + (username != null ? username : "GUEST") + " へのメッセージ送信失敗: クライアント接続がありません。");
            return;
        }
        try {
            ClientMessage clientMessage = new ClientMessage(type, gson.toJsonTree(data)); // share.ClientMessage
            String jsonMessage = gson.toJson(clientMessage);
            System.out.println("[ClientHandler] " + (username != null ? username : "GUEST") + " へ送信: Type=" + type);
            writer.println(jsonMessage);
        } catch (Exception e) {
            // ここでのエラーは主にシリアライズエラーなどだが、接続が切れている場合もある
            System.err.println("[ClientHandler] メッセージ送信エラー (" + (username != null ? username : "GUEST") + "宛, Type=" + type + "): " + e.getMessage());
            // 深刻な場合は接続を切断する処理も検討
        }
    }

    private void sendErrorMessage(String errorMessage) {
        System.err.println("[ClientHandler] エラーメッセージ送信 (" + (username != null ? username : "GUEST") + "宛): " + errorMessage);
        sendMessage("ERROR", new ErrorResponse(errorMessage)); // share.ErrorResponse
    }

    private void cleanup(String clientInfo) {
        try {
            if (username != null) {
                server.removeClient(username); // Serverクラスのメソッドで管理
            }
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            System.out.println("[ClientHandler-" + clientInfo + "] クリーンアップ完了。");
        } catch (IOException e) {
            System.err.println("[ClientHandler-" + clientInfo + "] クリーンアップエラー: " + e.getMessage());
        }
        isAuthenticated = false; // 念のため
    }
    
    private boolean isConnected() {
        return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
    }

    public String getUsername() {
        return username;
    }
}