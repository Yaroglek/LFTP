package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import service.Functions;
import service.Functions.Action;
import service.ReceiveThread;
import service.SendThread;
import service.TCPPackage;

public class Server {

    public static void main(String[] args) throws IOException {

        DatagramSocket socket = new DatagramSocket(9090);
        System.out.println("Listen at port 9090");
        while (true) {
            byte[] buf = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
            socket.receive(datagramPacket);
            InetAddress ip = datagramPacket.getAddress();
            int port = datagramPacket.getPort();
            TCPPackage clientAction = Functions.byteToPackage(buf);
            String information = new String(clientAction.getData());
            String[] informations = information.split("/");
            String filename = informations[0];
            int packageTotal = Integer.parseInt(informations[1]);
            if (!clientAction.getAction()){
                packageTotal = (int) Functions.getPackageTotal("./folder/" + filename);
            }
            DatagramSocket datagramSocket = Functions.getFreePort();
            information = new String(Integer.toString(datagramSocket.getLocalPort()) + '/' + packageTotal);
            TCPPackage data = new TCPPackage(0, false, 0, true, information.getBytes());
            try {
                byte[] bytes = Functions.packageToByte(data);
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip, port);
                datagramSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clientAction.getAction()){
                System.out.println("Get file " + filename + " at local port: " + datagramSocket.getLocalPort() + " from " + ip);
                ReceiveThread receiveThread = new ReceiveThread(datagramSocket, ip, port, filename, packageTotal);
                (new Thread(receiveThread)).start();
            }
            else {
                System.out.println("Send file " + filename + " at local port: " + datagramSocket.getLocalPort() + " to " + ip);
                SendThread sendThread = new SendThread(datagramSocket, port, ip, filename);
                (new Thread(sendThread)).start();
            }
        }
    }

}
