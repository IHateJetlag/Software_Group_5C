# チャット、グループへのメンバー追加、脱退など以外はできます
javac -cp "lib/gson-2.10.1.jar" -d bin server/*.java share/*.java client/gui/*.java client/*.java

java -cp "bin:lib/gson-2.10.1.jar" client.gui.App

java -cp "bin:lib/gson-2.10.1.jar" server.Server

#進捗は以下
step.txt
gamini https://aistudio.google.com/app/prompts?state=%7B%22ids%22:%5B%221IBd80S8b9g1hLZsdvJ4GqcAOVT3lghOm%22%5D,%22action%22:%22open%22,%22userId%22:%22103092520342708933183%22,%22resourceKeys%22:%7B%7D%7D&usp=sharing

## bin
binはコンパイル結果

## client
client.javaは前からあるやつ<ー削除済み
Connector.javaを使う　GUIとの繋ぎ

PCを分けてテストするときは、Connector.java12行目のlocalhostを変更。

## gui 
前のGUIと同じ

## lib 
.jarファイルの置き場
ここにデータが保存される

## server
ClientHandler.java
Server.java

## share
LoginDateTimeAdapter.java

前の
DateModels.javaとMessageModeks.javaを1ファイルずつに分けたもの



