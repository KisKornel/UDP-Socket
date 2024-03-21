package org.example;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Component1 {
    private static final int PORT = 8081;

    public static void main(String[] args) throws Exception {
        int blockSize = 1024;
        String endMsg = "end";
        byte[] endSend = endMsg.getBytes();
        byte[] receiveData = new byte[blockSize];

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress ipAddress = InetAddress.getByName("localhost");

        System.out.println("Add meg a küldendő állomány nevét (pl. hello.txt): ");
        String sentence = inFromUser.readLine();

        byte[] file = readFile(sentence);

        String fileLengthString = "FileLength: " + file.length;
        byte[] fLength = fileLengthString.getBytes();

        DatagramPacket fileLength = new DatagramPacket(fLength, fLength.length, ipAddress, PORT);
        clientSocket.send(fileLength);

        int blockCount = (file.length + blockSize - 1) / blockSize;
        byte[] range;

        for (int i = 1; i < blockCount; i++) {
            int idx = (i - 1) * blockSize;
            range = Arrays.copyOfRange(file, idx, idx + blockSize);
            DatagramPacket sendPacket = new DatagramPacket(range, range.length, ipAddress, PORT);
            clientSocket.send(sendPacket);

            System.out.println("Chunk " + i + ": " + new String(range));
        }

        // Last chunk
        int end;
        if (file.length % blockSize == 0) {
            end = file.length;
        } else {
            end = file.length % blockSize + (blockSize * (blockCount - 1));
        }

        range = Arrays.copyOfRange(file, (blockCount - 1) * blockSize, end);
        System.out.println("Chunk last: " + new String(range));

        DatagramPacket sendPacket = new DatagramPacket(range, range.length, ipAddress, PORT);
        clientSocket.send(sendPacket);

        //küldés vége
        DatagramPacket sendEndMsg = new DatagramPacket(endSend, endSend.length, ipAddress, PORT);
        clientSocket.send(sendEndMsg);

        //visszaküldés fogadása
        System.out.println("------------");
        System.out.println("Átalakítva:");
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData(), StandardCharsets.UTF_8).substring(0, receivePacket.getLength());

            if (!modifiedSentence.equals("end")) {
                System.out.println(modifiedSentence);
            }

            receiveData = new byte[blockSize];

            if (modifiedSentence.equals("end")) {
                break;
            }
        }

        clientSocket.close();
    }

    private static byte[] readFile(String fileName) {
        File myFile = new File(fileName);
        byte[] byteArray = new byte[(int) myFile.length()];
        try (FileInputStream inputStream = new FileInputStream(myFile)) {
            inputStream.read(byteArray, 0, byteArray.length);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return byteArray;
    }

}