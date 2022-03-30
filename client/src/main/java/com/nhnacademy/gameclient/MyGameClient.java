package com.nhnacademy.gameclient;


import com.nhnacademy.domain.ServerData;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class MyGameClient {
    private final String id;
    public MyGameClient(String id) {
        this.id = id;
    }

    public static void main(String[] args) {
        if (hasNotArgs(args)) { // 아이디를 받을 코드
            System.out.println("USAGE: java MyChatClient {id}");
            return;
        }
        String id = args[0];
        MyGameClient client = new MyGameClient(id);
        client.connect("127.0.0.1", 9999);
        //서버로 컨넥트
    }

    private static boolean hasNotArgs(String[] args) {
        return args.length == 0;
    }

    @SuppressWarnings("all")
    //커넥트 호스트 매개변수는 향후에 쓸수도 있음.
    private void connect(String serverHost, int port) {
        try {
            //클라이언트가 만든 소켓. 서버와 커넥트
            Socket socket = new Socket(serverHost, port);
            System.out.println("Connected to server " + serverHost + ":" + port);
            //메세지를 보내는 스레드
            Thread sender = new Sender(socket, id);
            //메세지를 받는 스레드
            Thread receiver = new Receiver(socket);
            //시스템 인에서 입력받아서 채팅을 보내야하는 상황이기에
            sender.start();
            receiver.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Sender extends Thread {
        private final String id;
        private final DataOutputStream out;

        private Sender(Socket socket, String id) throws IOException {
            this.id = id;
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                sendMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void initialize() throws IOException {
            if (isSendable()) {
                // 맨 처음에 서버에 아이디를 보낸다.
                this.out.writeUTF(id);
            }
        }

        private boolean isSendable() {
            return this.out != null;
        }

        private void sendMessage() throws IOException {
            try (Scanner scanner = new Scanner(System.in)) {
                String contents;
                while (isSendable()) {
                    this.out.flush();// 값 비우기
                    //서버에게 보낼건데
                    contents= scanner.nextLine();
                    if (!contents.equals("")) {
                        if(contents.equals("1")){
                            this.out.writeUTF("1");
                        }else if(contents.equals("2")){
                            this.out.writeUTF("2");
                            break;
                        }else{
                            System.out.println("엔터 또는 숫자 1, 2만 입력.");
                        }
                    } else {
                        this.out.writeUTF("");
                    }
                }
            }
        }
    }

    private static class Receiver extends Thread {
        private final InputStream basicIn;
        private Receiver(Socket socket) throws IOException {
            this.basicIn = socket.getInputStream();
        }
        @Override
        public void run() {
            initialize();
            while (isObjectReceivable()){ // 현재 리시버는 객체만 받을 수 있음.
                receiveObjectMessage();
            }
        }

        private void initialize() {
            if (isObjectReceivable()) {
                receiveObjectMessage();
            }
        }
        private boolean isObjectReceivable() {
            return this.basicIn != null;
        }

        // 객체를 받는 메서드 직관적으로 보이기위해 소나링크 추천 X
        @SuppressWarnings("all")
        private void receiveObjectMessage(){
            List list;
            try {
                ObjectInputStream oIn = new ObjectInputStream(basicIn);
                ServerData d = (ServerData) oIn.readObject();
                list =  d.getList();
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(list.get(i));
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("객체 리시브 오류");
            }
        }

    }
}