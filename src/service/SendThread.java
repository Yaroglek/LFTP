package service;

import static service.Functions.BLOCK_PACKAGE_NUM;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SendThread implements Runnable{

    private int desPort;
    private volatile int base = 0;
    private volatile int nextseqnum = 0;
    private InetAddress ip;

    /*滑动窗口协议*/
    private volatile long time;
    private volatile int rwnd = 500;
    private volatile  boolean ReSend = false;

    /*阻塞控制*/
    private static final int SLOW_START = 0;
    private static final int CONGESTION_AVOID = 1;
    private volatile int state = SLOW_START;
    private double cwnd = 1.0;
    private volatile int lastACK = -1;
    private volatile int duplicateACK = 0;
    private volatile double ssthresh = 50;
    
    /*大文件传输*/
    private int packageTotal;
    private long bytesTotal;
    private volatile int blockCur = 0;

    private volatile List<TCPPackage> tcpPackageList = new ArrayList<>();

    private DatagramSocket datagramSocket;
    private String file;

    long startTime = 0;

    public SendThread(DatagramSocket datagramSocket, int desPort, InetAddress ip, String file){
        this.datagramSocket = datagramSocket;
        this.desPort = desPort;
        this.ip = ip;
        this.file = file;
    }

    @Override
    public void run() {
        String path = "./folder/" + file;
        packageTotal = Functions.getPackageTotal(path);
        bytesTotal = Functions.getByteTotal(path);
        Thread receiveACKThread = new Thread(new ReceiveACKThread());
        receiveACKThread.start();
        Thread timeOutThread = new Thread(new TimeOut());
        timeOutThread.start();
        startTime = System.currentTimeMillis();
        for (blockCur = 0; blockCur < Math.floor(packageTotal / (float) BLOCK_PACKAGE_NUM) + 1; blockCur++){
            List<byte[]> byteList = Functions.getByteList(blockCur, path, packageTotal, bytesTotal);
            tcpPackageList.clear();
            for(int j = 0; j < byteList.size(); j++) {
                tcpPackageList.add(new TCPPackage(0, false, blockCur * BLOCK_PACKAGE_NUM + j, true, byteList.get(j)));
            }
            while (nextseqnum < tcpPackageList.size() + blockCur * BLOCK_PACKAGE_NUM) {
                if (ReSend == false && nextseqnum < base + cwnd) {
                    TCPPackage tcpPackage;
                    if (rwnd <= 0){
                        tcpPackage = new TCPPackage(0, false, -1, true, null);
                    }
                    else {
                        tcpPackage = tcpPackageList.get(nextseqnum % BLOCK_PACKAGE_NUM);
                    }
                    byte[] bytes = Functions.packageToByte(tcpPackage);
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip , desPort);
                    try {
                        datagramSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (base == nextseqnum){
                        time = System.currentTimeMillis();
                    }
                    nextseqnum++;
                }
            }
            while (lastACK < tcpPackageList.size() - 1 + blockCur * BLOCK_PACKAGE_NUM);
        }
        System.out.println("Success.");
        while (base < packageTotal);
        TCPPackage tcpPackage = new TCPPackage(0, true, nextseqnum, true, null);
        byte[] bytes = Functions.packageToByte(tcpPackage);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip , desPort);
        try {
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ReceiveACKThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                TCPPackage ACKPackage = null;
                try {
                    datagramSocket.receive(datagramPacket);
                    ACKPackage = Functions.byteToPackage(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert ACKPackage != null;
                rwnd = Functions.byteArrayToInt(ACKPackage.getData());
                if (ACKPackage.isFIN()){
                    System.out.println("Send finished.");
                    break;
                }
                if (lastACK + 1 == ACKPackage.getACK()) {
                    System.out.println("Speed: " + (lastACK + 1) * 1024 / (System.currentTimeMillis() - startTime + 1) + "KB/s, Finished: " +  String.format("%.2f", ((float) (lastACK + 1) / (float) packageTotal * 100)) + "%" + ", in " + (System.currentTimeMillis() - startTime + 1) / 1000 + "s");
                    if (state == SLOW_START) {
                        cwnd++;
                        if (cwnd > ssthresh) state = CONGESTION_AVOID;
                    }
                    else {
                        cwnd += (double)(1 / cwnd);
                    }
                    duplicateACK = 0;
                }   else {
                    duplicateACK++;
                }

                if (duplicateACK >= 3) {
                    ssthresh = cwnd / 2;
                    cwnd = ssthresh + 3;
                    state = CONGESTION_AVOID;
                }

                lastACK = ACKPackage.getACK();
                base = ACKPackage.getACK() + 1;
                if (base != nextseqnum) {
                    time = System.currentTimeMillis();
                }
                if (ACKPackage.getACK() == packageTotal - 1) {
                	break;
                }
            }
        }
    }

    class TimeOut implements Runnable {
        private long TTL = 300;
        @Override
        public void run() {
            while(true) {
                if (base >= packageTotal - 1) break;
                if (System.currentTimeMillis() - time > TTL) {
                    ssthresh = cwnd / 2 + 1;
                    cwnd = ssthresh;
                    state = SLOW_START;
                    ReSend = true;
                    int start = base;
                    int end = nextseqnum;
                    for (int i = start; i < end; i++){
                        TCPPackage tcpPackage = tcpPackageList.get(i % BLOCK_PACKAGE_NUM);
                        byte[] bytes = Functions.packageToByte(tcpPackage);
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ip , desPort);
                        try {
                            datagramSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    ReSend = false;
                }
            }
        }
    }
}