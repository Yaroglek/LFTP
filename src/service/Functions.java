package service;

import java.io.*;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class Functions {

	public enum Action {lsend, lget};
    private static final int MAX_BYTE = 1024;
    private final static int BLOCK_SIZE = 1024 * 1024 * 10;
    public final static int BLOCK_PACKAGE_NUM = BLOCK_SIZE / MAX_BYTE;

    public static void printError(String error){
    	System.out.println(" - Error: " + error);
    	System.out.println(" - Usage: LFTP lsend/lget [host] [filename]");
    	System.exit(1);
    }
    
    public static List<byte[]> fileToByte(String path) {
        try {
            FileInputStream inStream = new FileInputStream(new File(path));
            List<byte[]> bytes = new ArrayList<>();
            long BytesTotal = inStream.available();
            int packageNum = (int) Math.floor(BytesTotal / MAX_BYTE);
            int leave = (int) BytesTotal % MAX_BYTE;
            if (packageNum > 0) {
                for(int i = 0; i < packageNum; i++) {
                    byte[] data;
                    data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    bytes.add(data);
                }
            }
            byte[] data = new byte[leave];
            inStream.read(data, 0, leave);
            bytes.add(data);
            inStream.close();
            return bytes;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void byteToFile(FileOutputStream outputStream, List<byte[]> datas) {
        try {
            for (byte[] data : datas) {
                outputStream.write(data);
                outputStream.flush();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getByteTotal(String path) {
        File file = new File(path);
        return  file.length();
    }

    public static int getPackageTotal(String path) {
        File file = new File(path);
        long BytesTotal = file.length();
        return (int)Math.floor(BytesTotal / MAX_BYTE) + 1;
    }

    public static List<byte[]> getByteList(int blockNum, String path, int packageTotal, long bytesTotal){
        List<byte[]> ByteList = new ArrayList<>();
        try {
            FileInputStream inStream = new FileInputStream(new File(path));
            for (int i = 0; i < blockNum; i++){
                inStream.skip(BLOCK_SIZE);
            }
            if ((blockNum + 1) * BLOCK_PACKAGE_NUM >  packageTotal) {
                int len = packageTotal % BLOCK_PACKAGE_NUM;
                for (int i = 0; i < len - 1; i++){
                    byte[] data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    ByteList.add(data);
                }
                byte[] data = new byte[(int) (bytesTotal % MAX_BYTE)];
                inStream.read(data, 0, (int) (bytesTotal % MAX_BYTE));
                ByteList.add(data);
            }
            else {
                for (int i = 0; i < BLOCK_PACKAGE_NUM; i++){
                    byte[] data = new byte[MAX_BYTE];
                    inStream.read(data, 0, MAX_BYTE);
                    ByteList.add(data);
                }
            }
            inStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ByteList;
    }
    
    public static TCPPackage byteToPackage(byte[] bytes) {
        TCPPackage tcpPackage = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            tcpPackage = (TCPPackage) objectInputStream.readObject();
            byteArrayInputStream.close();
            objectInputStream.close();
        }
        catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return tcpPackage;
    }

    public static byte[] packageToByte(TCPPackage obj) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
            bo.close();
            oo.close();
        }
        catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int i) {
        return new byte[] {(byte) ((i >> 24) & 0xFF), (byte) ((i >> 16) & 0xFF), (byte) ((i >> 8) & 0xFF), (byte) (i & 0xFF)};
    }
    
    public static DatagramSocket getFreePort() {
        DatagramSocket socket = null;
        for (int i = 5001; i < 65535; i++){
            try {
                socket = new DatagramSocket(i);
            } catch (IOException ex) {
                continue;
            }
            break;
        }
        return socket;
    }
}
