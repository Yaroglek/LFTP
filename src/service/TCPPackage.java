package service;

import java.io.*;

public class TCPPackage implements Serializable {
    private int ACK;
    private boolean FIN;
    private int seq;
    private boolean action;
    private byte[] data;

    public TCPPackage(int ACK, boolean FIN, int seq, boolean action, byte[] data){
        this.ACK = ACK;
        this.FIN = FIN;
        this.seq = seq;
        this.action = action;
        this.data = data;
    }

    public void setData(byte[] data) {
        this.data = new byte[data.length];
        for(int i = 0; i < data.length; i++){
            this.data[i] = data[i];
        }
    }

    public boolean getAction() {
        return action;
    }

    public byte[] getData() {
        return data;
    }

    int getACK() {
        return ACK;
    }

    int getSeq() {
        return seq;
    }

    boolean isFIN() {
        return FIN;
    }
}
