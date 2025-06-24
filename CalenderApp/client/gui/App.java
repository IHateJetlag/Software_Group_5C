package client.gui;

import client.gui.LoginScreen;
/**
 * アプリケーションを起動するためのメインクラス。
 * このクラスがエントリーポイントとなり、ログイン画面を呼び出す。
 */
public class App {

    /**
     * アプリケーションのメインメソッド。
     * @param args コマンドライン引数（このアプリケーションでは使用しない）
     */
    public static void main(String[] args) {
        // LoginScreenクラスが持つmainメソッドを呼び出して、
        // アプリケーションの実行を開始する。
        LoginScreen.main(args);
    }
}