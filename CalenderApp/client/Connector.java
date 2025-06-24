package client; // clientパッケージに配置

import java.io.*;
import java.net.*;
import com.google.gson.*;
import share.LocalDateTimeAdapter; // shareパッケージのクラスをimport
import share.ServerMessage;      // メッセージ送信で使用
import share.ClientMessage;      // メッセージ受信で使用 (まだ本格的には使わない)

public class Connector {
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8080;

    private String serverHost;
    private int serverPort;

    private Socket socket;
    private BufferedReader reader; // サーバーからのメッセージを読む
    private PrintWriter writer;  // サーバーへメッセージを送る
    private Gson gson;

    private volatile boolean isConnected = false;
    private Thread messageReceiverThread;

    // GUIへサーバーからのメッセージを通知するためのコールバックインターフェース (任意・発展形)
    // public interface MessageListener {
    //     void onMessageReceived(ClientMessage message);
    //     void onError(String errorMessage);
    //     void onDisconnected();
    // }
    // private MessageListener messageListener;

    public Connector() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public Connector(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    // public void setMessageListener(MessageListener listener) {
    //     this.messageListener = listener;
    // }

    public synchronized boolean connect() {
        if (isConnected) {
            System.out.println("[Connector] 既にサーバーに接続済みです。");
            return true;
        }
        try {
            System.out.println("[Connector] サーバーに接続試行中: " + serverHost + ":" + serverPort);
            socket = new Socket(serverHost, serverPort);
            // タイムアウト設定 (任意)
            // socket.setSoTimeout(5000); // 5秒でタイムアウト

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true); // autoFlushをtrueに

            isConnected = true;

            // サーバーからのメッセージを受信するためのスレッドを開始
            messageReceiverThread = new Thread(this::receiveMessagesLoop);
            messageReceiverThread.setDaemon(true); // メインスレッド終了時に自動終了
            messageReceiverThread.start();

            System.out.println("[Connector] サーバーに接続しました。");
            return true;
        } catch (UnknownHostException e) {
            System.err.println("[Connector] 接続エラー: ホストが見つかりません - " + serverHost);
            // if (messageListener != null) messageListener.onError("ホストが見つかりません: " + serverHost);
        } catch (IOException e) {
            System.err.println("[Connector] サーバー接続エラー: " + e.getMessage());
            // if (messageListener != null) messageListener.onError("サーバー接続に失敗しました: " + e.getMessage());
        }
        isConnected = false; // 接続失敗
        return false;
    }

    public synchronized void disconnect() {
        if (!isConnected) {
            // System.out.println("[Connector] 既に切断されています。");
            return;
        }
        System.out.println("[Connector] サーバーから切断処理を開始します...");
        isConnected = false; // まずフラグをfalseにしてループを止める

        try {
            if (messageReceiverThread != null && messageReceiverThread.isAlive()) {
                messageReceiverThread.interrupt(); // 受信スレッドに割り込み
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[Connector] サーバーから切断しました。");
            // if (messageListener != null) messageListener.onDisconnected();
        } catch (IOException e) {
            System.err.println("[Connector] 切断処理中にエラーが発生しました: " + e.getMessage());
        } finally {
            // リソースをnullにしておく (任意)
            socket = null;
            reader = null;
            writer = null;
            messageReceiverThread = null;
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    // サーバーからのメッセージを継続的に受信するループ
    private void receiveMessagesLoop() {
        System.out.println("[Connector-Receiver] メッセージ受信スレッドを開始しました。");
        try {
            String serverJsonMessage;
            while (isConnected && (serverJsonMessage = reader.readLine()) != null) {
                // ★ 現状はコンソールに出力するだけ
                System.out.println("[Connector-Receiver] サーバーから受信: " + serverJsonMessage.substring(0, Math.min(serverJsonMessage.length(), 150)) + (serverJsonMessage.length() > 150 ? "..." : ""));

                // --- 将来的にここでメッセージをパースしてリスナーに通知 ---
                // try {
                //     ClientMessage clientMessage = gson.fromJson(serverJsonMessage, ClientMessage.class);
                //     if (messageListener != null) {
                //         // GUIスレッドで処理するためにSwingUtilities.invokeLaterを使う
                //         SwingUtilities.invokeLater(() -> messageListener.onMessageReceived(clientMessage));
                //     }
                // } catch (JsonSyntaxException e) {
                //     System.err.println("[Connector-Receiver] 受信メッセージのJSONパースエラー: " + e.getMessage());
                //     if (messageListener != null) {
                //         SwingUtilities.invokeLater(() -> messageListener.onError("サーバーから不正な形式のメッセージを受信しました。"));
                //     }
                // }
                // ----------------------------------------------------
            }
        } catch (SocketException e) {
            if (isConnected) { // 正常なdisconnect()呼び出し以外でのソケットクローズ
                System.err.println("[Connector-Receiver] ソケットエラー（接続が切断された可能性）: " + e.getMessage());
                // if (messageListener != null) SwingUtilities.invokeLater(() -> messageListener.onError("サーバーとの接続が切れました。"));
                disconnect(); // 強制的に切断処理
            }
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("[Connector-Receiver] メッセージ受信中にIOエラーが発生しました: " + e.getMessage());
                // if (messageListener != null) SwingUtilities.invokeLater(() -> messageListener.onError("メッセージ受信エラー。"));
                disconnect(); // 強制的に切断処理
            }
        } finally {
            if (isConnected) { // ループが抜けたが、まだisConnectedがtrueの場合（予期せぬ終了）
                System.out.println("[Connector-Receiver] 受信スレッドが予期せず終了。接続を切断します。");
                disconnect();
            } else {
                System.out.println("[Connector-Receiver] メッセージ受信スレッドを終了しました。");
            }
        }
    }

    /**
     * サーバーにメッセージを送信します。
     * @param serverMessage 送信する ServerMessage オブジェクト
     * @return 送信に成功した場合は true、失敗した場合は false
     */
    public synchronized boolean sendMessage(ServerMessage serverMessage) {
        if (!isConnected()) {
            System.err.println("[Connector] メッセージ送信失敗: サーバーに接続されていません。");
            return false;
        }
        if (writer == null) {
            System.err.println("[Connector] メッセージ送信失敗: PrintWriterが初期化されていません。");
            return false;
        }
        try {
            String jsonMessage = gson.toJson(serverMessage);
            System.out.println("[Connector] サーバーへ送信: " + jsonMessage.substring(0, Math.min(jsonMessage.length(), 150)) + (jsonMessage.length() > 150 ? "..." : ""));
            writer.println(jsonMessage);
            return !writer.checkError(); // checkError()で送信エラーを確認
        } catch (Exception e) { // JsonIOException なども考慮
            System.err.println("[Connector] メッセージ送信中にエラーが発生しました: " + e.getMessage());
            // エラーによっては接続が切れている可能性もあるので、切断処理を検討
            // disconnect();
            return false;
        }
    }

    // 簡単な動作テスト用のmainメソッド (任意)
    public static void main(String[] args) {
        Connector connector = new Connector();
        if (connector.connect()) {
            System.out.println("サーバーへの接続に成功しました。");
            // 簡単なテストメッセージ送信 (サーバー側が "PING" タイプを処理できれば)
            // ServerMessage pingMessage = new ServerMessage("PING", null);
            // connector.sendMessage(pingMessage);

            try {
                // しばらく接続を維持してメッセージ受信を待つ
                Thread.sleep(10000); // 10秒間
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            connector.disconnect();
        } else {
            System.out.println("サーバーへの接続に失敗しました。");
        }
    }
}