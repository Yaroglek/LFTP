package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import service.Functions;
import service.ReceiveThread;
import service.SendThread;
import service.TCPPackage;
import service.Functions.Action;

public class Client {

    private static InetAddress ip;
    private static DatagramSocket datagramSocket;

    public Client(String[] args) throws IOException {
        Action action = null;
        String information = "";
        if (args.length != 3) {
        	Functions.printError("Wrong number of arguments.");
        }
        if (args[0].equals("lsend")) {
            action = Action.lsend;
        }
        else if (args[0].equals("lget")) {
            action = Action.lget;
        }
        else {
        	Functions.printError("Unrecognized operation.");
        }
        try {
            ip = InetAddress.getByName(args[1]);
        } catch (Exception e){
            Functions.printError("Invalid host.");
        }
        int packageTotal = 0;
        String filename = args[2];
        if (action == Action.lsend) {
            packageTotal = (int) Functions.getPackageTotal("./folder/" + filename);
        }
        information = filename + "/" + packageTotal;
        datagramSocket = Functions.getFreePort();
        TCPPackage data = new TCPPackage(0, false, 0, action == Action.lsend, information.getBytes());
        sendPackage(data, ip, 9090);
        if (action == Action.lget){
            System.out.println("Send file " + filename + " at local port: " + datagramSocket.getLocalPort() + " to " + ip);
            TCPPackage receivePackage = receivePackage();
            String portAndPackageTotal = new String(receivePackage.getData());
            int desPort = Integer.parseInt(portAndPackageTotal.split("/")[0]);
            System.out.println("Get Server port: " + desPort);
            System.out.println("Start to send file");
            SendThread sendThread = new SendThread(datagramSocket, desPort, ip, filename);
            Thread thread = new Thread(sendThread);
            thread.start();
        }
        else if (action == Action.lget) {
            System.out.println("Get file " + filename + " at local port: " + datagramSocket.getLocalPort() + " from " + ip);
            TCPPackage receivePackage = receivePackage();
            String portAndPackageTotal = new String(receivePackage.getData());
            int desPort = Integer.parseInt(portAndPackageTotal.split("/")[0]);
            packageTotal = Integer.parseInt(portAndPackageTotal.split("/")[1]);
            System.out.println("Get Server port: " + desPort);
            System.out.println("Start to get file");
            ReceiveThread receiveThread = new ReceiveThread(datagramSocket, ip, desPort, filename, packageTotal);
            Thread thread = new Thread(receiveThread);
            thread.start();
        }
    }

    private static TCPPackage receivePackage() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(datagramPacket);
        return Functions.byteToPackage(buf);
    }

    private static void sendPackage(TCPPackage tcpPackage, InetAddress IP, int port) throws IOException {
        byte[] bytes = Functions.packageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, IP , port);
        datagramSocket.send(packet);
    }
}
