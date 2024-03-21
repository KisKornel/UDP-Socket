package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Component2 {

    public static void main(String[] args) throws Exception {
        int port;
        int blockSize = 1024;
        String endMsg = "end";
        byte[] endSend = endMsg.getBytes();
        byte[] combined;
        ByteBuffer buffer = null;

        DatagramSocket serverSocket = new DatagramSocket(8081);
        InetAddress ipAddress;

        byte[] bytesReceived = new byte[blockSize];

        while (true){
            DatagramPacket receivePacket = new DatagramPacket(bytesReceived, bytesReceived.length);
            // itt várakozik ameddig adat jön a 8080-as porton
            serverSocket.receive(receivePacket);

            String msg = new String(receivePacket.getData(), StandardCharsets.UTF_8).substring(0, receivePacket.getLength());

            if (msg.startsWith("FileLength: ")) {
                String[] s = msg.split("\\s");
                int fLength = Integer.parseInt(s[1]);
                System.out.println("File length: " + fLength);
                combined = new byte[fLength];
                buffer = ByteBuffer.wrap(combined);
            } else if (!msg.equals("end") && buffer != null) {
                System.out.println(msg);
                buffer.put(msg.getBytes());
            }

            ipAddress = receivePacket.getAddress();
            port = receivePacket.getPort();

            bytesReceived = new byte[blockSize];

            if (msg.equals("end")) {
                System.out.println("------------");
                break;
            }
        }

        combined = buffer.array();

        int blockCount = (combined.length + blockSize - 1) / blockSize;
        byte[] range;

        for (int i = 1; i < blockCount; i++) {
            int idx = (i - 1) * blockSize;
            range = Arrays.copyOfRange(combined, idx, idx + blockSize);
            String upperCase = new String(range).toUpperCase();
            byte[] bytesSent = upperCase.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(bytesSent, bytesSent.length, ipAddress, port);
            serverSocket.send(sendPacket);

            System.out.println("Chunk " + i + ": " + upperCase);
        }

        // Last chunk
        int end;
        if (combined.length % blockSize == 0) {
            end = combined.length;
        } else {
            end = combined.length % blockSize + (blockSize * (blockCount - 1));
        }

        range = Arrays.copyOfRange(combined, (blockCount - 1) * blockSize, end);
        String upperCase = new String(range).toUpperCase();
        byte[] bytesSent = upperCase.getBytes();
        System.out.println("Chunk last: " + upperCase);

        DatagramPacket sendPacket = new DatagramPacket(bytesSent, bytesSent.length, ipAddress, port);
        serverSocket.send(sendPacket);

        DatagramPacket sendEndMsg = new DatagramPacket(endSend, endSend.length, ipAddress, port);
        serverSocket.send(sendEndMsg);

        serverSocket.close();
    }
}
