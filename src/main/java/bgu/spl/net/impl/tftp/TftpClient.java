package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;

public class TftpClient {
    private Socket socket;
    private BufferedReader keyboardReader;
    private InputStream in;
    private OutputStream out;
    private Thread keyboardThread;
    private Thread listeningThread;

    private int ACKnum;
    private boolean WRQdone = true;

    private final TftpProtocol protocol = new TftpProtocol();
    private final TftpEncoderDecoder encdec = new TftpEncoderDecoder();


    public TftpClient(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            keyboardReader = new BufferedReader(new InputStreamReader(System.in));
            in = socket.getInputStream();
            out = socket.getOutputStream();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        // Start keyboard thread
        keyboardThread = new Thread(() -> {
            try {
                String userInput;
                String userOp;
                while (!protocol.shouldTerminate()) {
                    String name = null;
                    if (keyboardReader.ready()) {
                        if ((userInput = keyboardReader.readLine()) != null) {
                            userOp = userInput.substring(0, userInput.indexOf(' ') > -1 ? userInput.indexOf(' ') : userInput.length());
                            if (!(userOp.equals("DIRQ") | userOp.equals("DISC")) && userInput.length() > 0)
                                name = userInput.substring(userOp.length() + 1);
                            if ((protocol.process(userInput)) != null) {
                                send(encdec.encode(userOp, name));
                                userOp = protocol.comapreCommand(userOp);
                                if (userOp.equals("WRQ")) {
                                    WRQdone = false;
                                }
                            }
                        }
                    }
//                    else{
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        keyboardThread.start();

        // Start listening thread
        listeningThread = new Thread(() -> {
            try {
                int read;
                byte[] serverInput;
                while (!protocol.shouldTerminate()) {
                    if (in.available() > 0) {
                        read = in.read();
                        if (read >= 0) {
                            serverInput = encdec.decodeNextByte((byte) read);

                            if (serverInput != null) {
                                byte[] response = protocol.process(serverInput);
                                if (response != null) {
                                    if (response.length > 2) {
                                        send(response);
                                    } else if (response.length == 2) {
                                        ACKnum = ((response[0] & 0xFF) << 8) | (response[1] & 0xFF);
                                        if (!WRQdone) sendPackets();
                                    }
                                }
                            }
                        }
                    }
//                    else {
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }

                }
                System.out.println("listener thread while end");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listeningThread.start();
    }

    private void sendPackets() {
        byte[] packet;
        if ((packet = protocol.processWRQ(ACKnum)) != null) {
            send(packet);
        } else {
            WRQdone = true;
        }
    }

    private void waitForThreads() {
        try {
            keyboardThread.join();
            listeningThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void send(byte[] msg) {
        try {
            System.out.println("#opcode is " + Opcode.getByOrdinal(msg[1]).name());

//            for(int i = 0 ; i<msg.length; i++){
//                System.out.print((msg[i] & 0xFF) + ",");
//            }
//            System.out.println();
//            System.out.println(new String(msg, StandardCharsets.UTF_8));  //IF U WANT TO SEE THE DATA U SEND RAW
//            System.out.println("#sending msg with length= " + msg.length);


            out.write(msg);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) {
        String serverAddress = "192.168.0.178"; // TODO: make sure the server adress+port is correct
        int port = 7777;
        TftpClient client = new TftpClient(serverAddress, port);
        client.start();

        client.waitForThreads();
        System.out.println("close");
        client.close();

    }


}
