package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TftpClient {
    private Socket socket;
    private BufferedReader keyboardReader;
    private InputStream in;
    private OutputStream out;
    private Thread keyboardThread;
    private Thread listeningThread;

    private int ACKnum;
    private int packetSent;
    private boolean WRQdone = false;

    private TftpProtocol protocol = new TftpProtocol();
    private TftpEncoderDecoder encdec = new TftpEncoderDecoder();


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

    public void start() {
        // Start keyboard thread
        keyboardThread = new Thread(() -> {
            try {
                String userInput;
                String userOp;
                String name;
                while (!protocol.shouldTerminate()) {
                    if ((userInput = keyboardReader.readLine()) != null) {
                        userOp = userInput.substring(0, userInput.indexOf(' '));
                        name = userInput.substring(userOp.length()+1);
                        if ((protocol.process(userInput)) != null) {
                            userOp = protocol.comapreCommand(userOp);
                            send(encdec.encode(userInput, name));
                            if (userOp.equals("WRQ")) {
                                WRQdone = false;
                                sendPackets();
                            }
                        }
                    }

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
                while (!protocol.shouldTerminate() && (read = in.read()) > 0) {
                    System.out.println(read);
                    serverInput = encdec.decodeNextByte((byte) read);
                    if (serverInput != null) {
                        byte[] response = protocol.process(serverInput);
                        if (response != null) {
                            if (response.length > 2) {
                                out.write(response);
                                out.flush();
                            } else {
                                ACKnum = ((response[0] & 0xFF) << 8) | (response[1] & 0xFF);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listeningThread.start();
    }

    private void sendPackets() {
        byte[] packet;
        while (!WRQdone) {
            if ((packet = protocol.processWRQ(ACKnum)) !=null) {
                send(packet);
            }
            else{
                WRQdone = true;
            }
        }
    }

    private void send(byte[] msg) {
        try {
            System.out.println(new String(msg, StandardCharsets.UTF_8));
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) throws IOException {
        String serverAddress = "192.168.0.178"; // TODO: make sure the server adress+port is correct
        int port = 7777;
        TftpClient client = new TftpClient(serverAddress, port);
        client.start();
    }
}
