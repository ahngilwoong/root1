package com.nhnacademy.gameserver;

import com.nhnacademy.domain.Creatures;
import com.nhnacademy.domain.Dual;
import com.nhnacademy.domain.ServerData;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MyGameServer {
    // clientId: 클라이언트 전송용 OutputStream
    @SuppressWarnings("squid:3740")
    private final ConcurrentHashMap<String, OutputStream>
        clientOutMap = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    //게임서버를 실행해야하는데 오류...
    public static void main(String[] args) throws IOException {
        MyGameServer server = new MyGameServer();
        server.start();
    }

    @SuppressWarnings("all")
    //무한루프를 꼭 돌아야하는 곳이기에 올
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println(getTime() + " Start server " + serverSocket.getLocalSocketAddress());
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientSession client = new ClientSession(socket);
                    client.start();
                } catch (IOException e) {
                }
            }
        }
    }

    private String getTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss"));
    }

    @SuppressWarnings("squid:106")
    void joinGame(ClientSession session) throws Exception {
        clientOutMap.put(session.id, session.basicOut);
        sendListMessage(mainScreen(),session);
        System.out.println(getTime() + " " + session.id + " is joined: " + session.socket.getInetAddress());
        loggingCurrentClientCount();
    }

    void leaveGame(ClientSession session) {
        clientOutMap.remove(session.id);
        System.out.println(getTime() + " " + session.id + " is leaved: " + session.socket.getInetAddress());
        loggingCurrentClientCount();
    }

    private void loggingCurrentClientCount() {
        System.out.println(getTime() + " Currently " + clientOutMap.size() + " clients are connected.");
    }

    // 모든 유저에게 객체로 전달
    void sendObjectToAll(List<String> list) throws IOException {
        ServerData serverData = new ServerData();
        ObjectOutputStream oos;
        serverData.setList(list);
            for (OutputStream out : clientOutMap.values()) {
                oos = new ObjectOutputStream(out);
                oos.writeObject(serverData);
                oos.flush();
            }
    }

    //  한명 개인에게 리스트 객체 전달.
    @SuppressWarnings("squid:112")
    private void sendListMessage(List<String> ar, ClientSession cs) throws Exception {
        ServerData serverData = new ServerData();
        ObjectOutputStream oos;
        serverData.setList(ar);
        oos = new ObjectOutputStream(cs.out);
        oos.writeObject(serverData);
        oos.flush();
    }


    class ClientSession extends Thread {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        private final OutputStream basicOut;


        private String id;
        private int stageNum = 1;
        private int enterDungeonSelect = 0;
        private int level;
        Creatures warrior;
        @SuppressWarnings("all")
        //데미지포인트 씀..
        private int damagePoint;

        ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.basicOut = socket.getOutputStream();
            this.level = 1;
        }
        @Override
        public void run() {
            try {
                initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void initialize() throws Exception {
            this.id = in.readUTF();
            joinGame(this);
        }

        private void connect() throws IOException {
            try {
                String contents;
                warrior = new Creatures(this.id, 100,10,false);
                while (isConnect()) {
                    contents = in.readUTF();
                    System.out.println(contents +"가 입력되었음.");
                    if(contents.equals("")){ // 클라이언트가 엔터를 입력했을때
                        if(enterDungeonSelect==0){
                            sendListMessage(secondMainScreen(this),this); // 두번째 메인스크린 화면을 한번만 보여주기 위해.
                            enterDungeonSelect++;
                            continue;
                        }
                        if (stageNum == 2) {
                            sendListMessage(appearMonster("야생의 오크"),this);
                        }
                        if(stageNum==3){
                            sendListMessage(appearMonster("*보스* 드래곤"),this);
                        }

                    }else{ // 클라이언트가 숫자를 입력했을때
                        if(contents.equals("1")){
                            if(enterDungeonSelect==1){ // 던전으로 들어간 뒤 또 1번과 2번의 선택지가 나오기 때문에 한번 더 셀렉트 메세지를 보낸다.
                                sendListMessage(appearMonster("야생의 슬라임"),this);
                                enterDungeonSelect++;
                            }else{
                                versus(warrior);
                            }
                        }else if(contents.equals("2")){
                            sendListMessage(endGame(),this);
                            leaveGame(this);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        @SuppressWarnings("all")
        private void versus(Creatures warrior) throws Exception {
            Dual dual = new Dual(warrior, new Creatures("슬라임", 30, 4, false));
            if(stageNum == 1){
                sendListMessage(dual.fightUserVsMonster(),this);
            }else if(stageNum == 2){
                dual = new Dual(warrior, new Creatures("오크", 40,6,false));
                sendListMessage(dual.fightUserVsMonster(),this);
            }else if(stageNum == 3 ){

                //레벨업 상향된 스테이터스로 초기화 분리필요..
                dual = new Dual(new Creatures(this.id, 150,20,false), new Creatures("드래곤", 100,10,true));
                sendListMessage(dual.fightUserVsMonster(),this);
            }
            if(isLoseChecked(dual.winner)){ // 몬스터가 이긴 경우
                sendListMessage(endGame(),this); // 종료 메세지 전송 후 서버 연결 끊음.
                leaveGame(this);
            }else{
                if(stageNum==2)sendListMessage(levelUp(),this);
                if(stageNum!=3){ // 드래곤 스테이지(마지막 스테이지)가 아니면 계속 하시겠습니까를 출력.
                    sendListMessage(continueGame(),this);
                }else{
                    sendObjectToAll(killedDragon()); // 드래곤 처치 외침
                }
                stageNum++; // 스테이지 넘어감.
            }

        }

        private boolean isLoseChecked(String who){ // 누가 졌는지.
            return who.equals("슬라임") || who.equals("오크") || who.equals("드래곤");
        }
        private boolean isConnect() {
            return this.in != null; // null이 아니면 연결이 되있는거임.
        }

        private void disconnect() {
            leaveGame(this);
        }

        //레벨업 부분 리스트로 보내기 위해 준비
        private List<String> levelUp(){
            List<String> list = new ArrayList<>();
            this.level++;
            this.damagePoint += 10;
            list.add("레벨 "+ level+"로 상승헀다.");
            return list;
        }
        private List<String> killedDragon(){
            List<String> list = new ArrayList<>();
            list.add("[외침] 용사 {"+this.id+"}가 드래곤을 물리쳤다!");
            return list;
        }
    }


    private List<String> mainScreen(){
        List<String> ar = new ArrayList<>();
        ar.add("                 ___====-_  _-====___");
        ar.add("           _--^^^#####//      \\\\#####^^^--_");
        ar.add("        _-^##########// (    ) \\\\##########^-_");
        ar.add("       -############//  |\\^^/|  \\\\############-");
        ar.add("     _/############//   (@::@)   \\\\############\\_");
        ar.add("    /#############((     \\\\//     ))#############\\");
        ar.add("   -###############\\\\    (oo)    //###############-");
        ar.add("  -#################\\\\  / VV \\  //#################-");
        ar.add(" -###################\\\\/      \\//###################-");
        ar.add("_#/|##########/\\######(   /\\   )######/\\##########|\\#_");
        ar.add("|/ |#/\\#/\\#/\\/  \\#/\\##\\  |  |  /##/\\#/  \\/\\#/\\#/\\#| \\|");
        ar.add("`  |/  V  V  `   V  \\#\\| |  | |/#/  V   '  V  V  \\|  '");
        ar.add("\"   `   `  `      `   / | |  | | \\\\   '      '  '   '\"");
        ar.add("                    (  | |  | |  )");
        ar.add("                   __\\ | |  | | /__");
        ar.add("                  (vvv(VVV)(VVV)vvv)");
        ar.add("");
        ar.add("-- 계속 할려면 엔터를 입력해주세요. --");
        return ar;
    }
    List<String> secondMainScreen(ClientSession session){
        List<String> list = new ArrayList<>();
        list.add("용사 {"+session.id+"}님 던전에 있는 드래곤을 물리쳐주세요!!");
        list.add("1. 던전으로 들어간다.");
        list.add("2. 도망간다. (게임종료)");
        return list;
    }
    List<String> appearMonster(String name){
        List<String> list = new ArrayList<>();
        list.add(name +"(이)가 나타났다");
        list.add("1. 공격");
        list.add("2. 도망간다. (게임 종료)");
        return list;
    }
    List<String> endGame(){
        List<String> list = new ArrayList<>();
        list.add("게임을 종료합니다.");
        return list;
    }

    List<String> continueGame(){
        List<String> list = new ArrayList<>();
        list.add("-- 계속 할려면 엔터를 입력해주세요. --");
        return list;
    }

}

