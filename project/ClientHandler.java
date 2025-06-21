import java.io.*;
import java.net.*;
import com.google.gson.*;
import java.util.Arrays;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private Gson gson;
    private boolean isAuthenticated;
    
    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
        this.isAuthenticated = false;
        
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("クライアントハンドラー初期化エラー: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                // ★★★ 修正 ★★★ 受信ログを追加
                System.out.println("[ClientHandler] Client '" + (username != null ? username : " GUEST") + "' から受信: " + inputLine.substring(0, Math.min(inputLine.length(), 150)));
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            // クライアントが切断した場合のエラーは静かに処理
            if (!"Socket closed".equals(e.getMessage()) && !"Connection reset".equals(e.getMessage())) {
                 System.err.println("クライアント通信エラー: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    
    private void handleMessage(String message) {
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
            if (serverMessage.getType() == null) {
                sendErrorMessage("メッセージタイプがnullです。");
                return;
            }

            // 認証が必要な操作のチェック
            boolean needsAuth = Arrays.asList("SEND_CHAT", "ADD_SCHEDULE", "CREATE_GROUP", "GET_USER_DATA").contains(serverMessage.getType());
            if (needsAuth && !isAuthenticated) {
                sendErrorMessage("認証が必要です。ログインしてください。");
                return;
            }
            
            switch (serverMessage.getType()) {
                case "LOGIN": handleLogin(serverMessage); break;
                case "REGISTER": handleRegister(serverMessage); break;
                case "SEND_CHAT": handleSendChat(serverMessage); break;
                case "ADD_SCHEDULE": handleAddSchedule(serverMessage); break;
                case "CREATE_GROUP": handleCreateGroup(serverMessage); break;
                case "GET_USER_DATA": handleGetUserData(); break;
                default: sendErrorMessage("不明なメッセージタイプです: " + serverMessage.getType());
            }
        } catch (JsonSyntaxException e) {
            sendErrorMessage("無効なJSON形式です: " + e.getMessage());
        }
    }
    
    private void handleLogin(ServerMessage message) {
        try {
            LoginRequest req = gson.fromJson(message.getData(), LoginRequest.class);
            if (server.authenticateUser(req.getUsername(), req.getPassword())) {
                this.username = req.getUsername();
                this.isAuthenticated = true;
                server.addClient(this.username, this);
                
                sendMessage("LOGIN_SUCCESS", new LoginResponse(true, "ログインに成功しました"));
                
                // ★★★ 修正 ★★★ 改善されたgetUserDataを呼び出して全データを送信
                UserData userData = server.getUserData(this.username);
                if (userData != null) {
                    sendMessage("USER_DATA", userData);
                }
            } else {
                sendMessage("LOGIN_FAILED", new LoginResponse(false, "ユーザー名またはパスワードが間違っています"));
            }
        } catch (Exception e) {
            sendErrorMessage("ログイン処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleRegister(ServerMessage message) {
        try {
            RegisterRequest req = gson.fromJson(message.getData(), RegisterRequest.class);
            if (server.registerUser(req.getUsername(), req.getPassword(), req.getEmail())) {
                sendMessage("REGISTER_SUCCESS", new RegisterResponse(true, "登録に成功しました"));
            } else {
                sendMessage("REGISTER_FAILED", new RegisterResponse(false, "そのユーザー名は既に使用されています"));
            }
        } catch (Exception e) {
            sendErrorMessage("登録処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleSendChat(ServerMessage message) {
        try {
            ChatRequest req = gson.fromJson(message.getData(), ChatRequest.class);
            server.addChatMessage(this.username, req.getGroupId(), req.getMessage());
        } catch (Exception e) {
            sendErrorMessage("チャット送信エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAddSchedule(ServerMessage message) {
        try {
            Schedule schedule = gson.fromJson(message.getData(), Schedule.class);
            server.addSchedule(this.username, schedule);
        } catch (Exception e) {
            sendErrorMessage("スケジュール追加エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleCreateGroup(ServerMessage message) {
        try {
            CreateGroupRequest req = gson.fromJson(message.getData(), CreateGroupRequest.class);
            server.createGroup(req.getGroupName(), this.username, req.getMembers());
        } catch (Exception e) {
            sendErrorMessage("グループ作成エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleGetUserData() {
        try {
            UserData userData = server.getUserData(this.username);
            if (userData != null) {
                sendMessage("USER_DATA", userData);
            }
        } catch (Exception e) {
            sendErrorMessage("ユーザーデータ取得エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void sendMessage(String type, Object data) {
        try {
            ClientMessage clientMessage = new ClientMessage(type, gson.toJsonTree(data));
            String jsonMessage = gson.toJson(clientMessage);
            // ★★★ 修正 ★★★ 送信ログを改善
            System.out.println("[ClientHandler] Client '" + this.username + "' へ送信: Type=" + type);
            writer.println(jsonMessage);
        } catch (Exception e) {
            System.err.println("メッセージ送信エラー (" + username + "宛): " + e.getMessage());
        }
    }
    
    private void sendErrorMessage(String errorMessage) {
        System.err.println("[Server-Error] Client '" + this.username + "' へのエラーメッセージ: " + errorMessage);
        sendMessage("ERROR", new ErrorResponse(errorMessage));
    }
    
    private void cleanup() {
        try {
            server.removeClient(username);
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("クライアントハンドラークリーンアップエラー: " + e.getMessage());
        }
    }
}