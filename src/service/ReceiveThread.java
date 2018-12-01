package service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ReceiveThread implements Runnable {

    private InetAddress ip;
    private int port;
    private DatagramSocket datagramSocket;
    private final int RevBuff = 20000;
    private volatile int rwnd = RevBuff;
    private volatile int expectedSeqNum = 0;
    private String file;
    private long startTime;
    private int packageTotal;

    public ReceiveThread(DatagramSocket datagramSocket, InetAddress ip, int port, String file, int packageTotal){
        this.datagramSocket = datagramSocket;
        this.ip = ip;
        this.port = port;
        this.file = file;
        this.packageTotal = packageTotal;
    }

    @Override
    public void run() {
        List<byte[]> datas = new ArrayList<>();
        TCPPackage replyACK = null;
        FileOutputStream outputStream = null;
        try {
            outputStream  = new FileOutputStream(new File("./folder/" + file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        startTime = System.currentTimeMillis();
        while (true) {
            TCPPackage receivePackage = null;
            try {
                receivePackage = receivePackage();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            assert receivePackage != null;
            if (receivePackage.isFIN()){
                replyACK = new TCPPackage(expectedSeqNum, true, 0, true, Functions.intToByteArray(rwnd));
                try {
                    sendPackage(replyACK);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Functions.byteToFile(outputStream, datas);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Success.");
                break;
            }
            if (receivePackage.getSeq() == expectedSeqNum) {
                rwnd--;
                System.out.println("Speed: " + expectedSeqNum * 1024/(System.currentTimeMillis() - startTime + 1) + "KB/s, Finished: " +  String.format("%.2f", ((float)(expectedSeqNum-1) * 100 / (float) packageTotal))+ "%" + ", in " + (System.currentTimeMillis() - startTime + 1) / 1000 + "s");
                datas.add(receivePackage.getData());
                replyACK = new TCPPackage(expectedSeqNum, false, 0, true, Functions.intToByteArray(rwnd));
                expectedSeqNum++;
                try {
                    replyACK.setData(Functions.intToByteArray(rwnd));
                    sendPackage(replyACK);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    assert replyACK != null;
                    replyACK.setData(Functions.intToByteArray(rwnd));
                    sendPackage(replyACK);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (rwnd == 0){
                Functions.byteToFile(outputStream, datas);
                datas.clear();
                rwnd = RevBuff;
            }
        }
    }

    private void sendPackage(TCPPackage tcpPackage) throws IOException {
        byte[] bytes = Functions.packageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip , port);
        datagramSocket.send(packet);
    }

    private TCPPackage receivePackage() throws IOException {
        byte[] buf = new byte[1400];
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
        datagramSocket.receive(datagramPacket);
        return Functions.byteToPackage(buf);
    }
}
