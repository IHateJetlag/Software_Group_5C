# CLI/GUI実装対応表

## 機能別実装方法の整理

| 機能 | CLIでの実装 (`Client.java`の`main`メソッド) | GUIでの実装案 (新規作成する`MainFrame.java`など) | 呼び出すべきバックエンド処理 (`Client.java`) |
|------|---------------------------------------------|---------------------------------------------------|---------------------------------------------|

### ログイン機能

**CLI実装:**
```java
// ユーザー名とパスワードをコンソールから取得
System.out.print("ユーザー名: ");
String username = scanner.nextLine();
System.out.print("パスワード: ");
String password = scanner.nextLine();

// ログイン処理を呼び出し
client.login(username, password);
```

**GUI実装:**
```java
// ActionListener内でJTextFieldから値を取得
String username = usernameField.getText();
String password = new String(passwordField.getPassword());

// バックエンド処理を呼び出し
// ★通信なのでSwingWorkerを使うのがベスト
new SwingWorker<Void, Void>() {
    protected Void doInBackground() {
        client.login(username, password);
        return null;
    }
}.execute();
```

**バックエンド処理:**
```java
public boolean login(String username, String password)
```

---

### チャット送信機能

**CLI実装:**
```java
// 送信先とメッセージをコンソールから取得
System.out.print("送信先のグループID: ");
String groupId = scanner.nextLine();
System.out.print("メッセージ: ");
String message = scanner.nextLine();

// チャット送信処理を呼び出し
client.sendChat(groupId, message);
```

**GUI実装:**
```java
// ActionListener内でコンポーネントから値を取得
String groupId = (String) groupList.getSelectedValue();
String message = messageInputField.getText();

// バックエンド処理を呼び出し
client.sendChat(groupId, message);

// 入力欄をクリア
messageInputField.setText("");
```

**バックエンド処理:**
```java
public boolean sendChat(String groupId, String messageText)
```

---

### スケジュール追加機能

**CLI実装:**
```java
// 各項目をコンソールから取得
Schedule schedule = new Schedule();
System.out.print("タイトル: ");
schedule.setTitle(scanner.nextLine());
// ...以下同様...

// スケジュール追加処理を呼び出し
client.addSchedule(schedule);
```

**GUI実装:**
```java
// JDialogなどで入力フォームを作成
// 「OK」ボタンのActionListener内で...
Schedule schedule = new Schedule();
schedule.setTitle(titleField.getText());
// ...以下同様...

// バックエンド処理を呼び出し
client.addSchedule(schedule);
```

**バックエンド処理:**
```java
public boolean addSchedule(Schedule schedule)
```

---

### データ更新と再表示機能

**CLI実装:**
```java
// ユーザーが"4"を入力した時
case "4":
    client.requestUserData();
    break;

// または、他のアクション後に自動で
// handleUserData()が呼ばれ、
// displayCurrentStatus()で表示
```

**GUI実装:**
```java
// 「更新」ボタンのActionListenerで...
refreshButton.addActionListener(e -> {
    client.requestUserData();
});

// または、他のアクション後に自動で
// ClientクラスからGUIに更新通知が来て、
// 表示を更新するメソッドを呼び出す
```

**バックエンド処理:**
```java
public boolean requestUserData()
```

---

### サーバーからのデータ受信機能

**CLI実装:**
```java
// receiveMessagesスレッドで受信後、
// handleUserData() が呼ばれる
private void handleUserData(...) {
    // ...
    // コンソールに表示
    displayCurrentStatus();
}
```

**GUI実装:**
```java
// ClientクラスのhandleUserData()を改造し、
// GUIクラスの更新メソッドを呼び出す
private void handleUserData(...) {
    // ...
    // GUIに更新を通知
    if (mainFrame != null) {
        SwingUtilities.invokeLater(() -> {
            mainFrame.updateAllPanels(currentUserData);
        });
    }
}
```

**バックエンド処理:**
```java
private void handleUserData(ClientMessage message)
```

---

## 実装時の注意点

### GUI実装での重要なポイント
1. **SwingWorker の使用**: 通信処理は必ずSwingWorkerでバックグラウンド実行
2. **SwingUtilities.invokeLater()**: サーバーからの応答でGUI更新時に使用
3. **入力検証**: GUIコンポーネントからの入力値検証を忘れずに
4. **エラーハンドリング**: ユーザーに分かりやすいエラーメッセージを表示

### 設計パターン
- **Observer パターン**: サーバーからの通知をGUIに反映
- **Command パターン**: ユーザー操作を統一的に処理
- **MVC パターン**: 表示ロジックとビジネスロジックの分離