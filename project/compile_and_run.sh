#!/bin/bash

# Gson ライブラリのダウンロード（必要に応じて）
echo "Gsonライブラリを確認中..."
GSON_JAR="gson-2.10.1.jar"
if [ ! -f "$GSON_JAR" ]; then
    echo "Gsonライブラリをダウンロード中..."
    wget https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
fi

echo "コンパイル中..."

# 全てのJavaファイルをコンパイル
javac -cp ".:$GSON_JAR" *.java

if [ $? -eq 0 ]; then
    echo "コンパイル成功！"
    echo ""
    echo "使用方法:"
    echo "1. サーバーを起動: java -cp \".:$GSON_JAR\" Server"
    echo "2. クライアントを起動: java -cp \".:$GSON_JAR\" Client"
    echo ""
    echo "サーバーを起動しますか？ (y/n)"
    read -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "サーバーを起動中..."
        java -cp ".:$GSON_JAR" Server
    fi
else
    echo "コンパイルエラーが発生しました。"
    exit 1
fi