package ipc;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();
    private _CAPP_HEADER m_sHeader = new _CAPP_HEADER();

    private class _CAPP_HEADER {
        byte[] capp_totlen;
        byte capp_type;
        byte capp_unused;
        byte[] capp_data;

        public _CAPP_HEADER() {
            this.capp_totlen = new byte[2];
            this.capp_type = 0x00;
            this.capp_unused = 0x00;
            this.capp_data = null;
        }
    }//내부 클래스

    private byte[] totalLength(int lengthOfStr) {
        byte[] totalLength = new byte[2];
        totalLength[0] = (byte) ((lengthOfStr & 0xFF00) >> 8);
        totalLength[1] = (byte) (lengthOfStr & 0xFF);
        return totalLength;
    }

    public ChatAppLayer(String pName) {
        // super(pName);
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    private void ResetHeader() {//header를 모두 0으로 초기화
        for (int i = 0; i < 2; i++) {
            m_sHeader.capp_totlen[i] = (byte) 0x00;
        }
        m_sHeader.capp_type = 0x00;
        m_sHeader.capp_unused = 0x00;
        m_sHeader.capp_data = null;
    }

    @Override
    public boolean Send(byte[] input, int length) {
        int maxIndex = length / 1456;
        maxIndex = length % 1456 > 0 ? maxIndex + 1 : maxIndex;
        byte[] totalLength = this.totalLength(maxIndex * 1456);
        System.out.println(totalLength[0]);
        System.out.println(totalLength[1]);
        byte type = 0x00;
        if (length > 1456) {
            for (int nowIndex = 0; nowIndex < maxIndex; nowIndex++) {
                this.waitForOtherFrameSendAndSetEhterNetHeaderType();
                type = this.setTokenType(nowIndex, maxIndex);
                byte[] sendData = this.objectToByte(input, length, nowIndex, totalLength, type);
                this.GetUnderLayer().Send(sendData, sendData.length);
            }
        } else {
            byte[] sendData = this.objectToByte(input, length, 0, totalLength, type);
            this.waitForOtherFrameSendAndSetEhterNetHeaderType();
            this.GetUnderLayer().Send(sendData, sendData.length);
        }
        return true;
    }

    private void waitForOtherFrameSendAndSetEhterNetHeaderType() {
        EthernetLayer underLayer = (EthernetLayer) this.GetUnderLayer();
        while (!((underLayer).ethernetHeaderGetType(0) == 0x00
                && (underLayer).ethernetHeaderGetType(1) == 0x00)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        byte[] newTypeOfEthernet = new byte[2];
        newTypeOfEthernet[0] = (byte) 0x20;
        newTypeOfEthernet[1] = (byte) 0x80;
        underLayer.setEthernetHeaderType(newTypeOfEthernet);
    }

    private byte setTokenType(int nowIndex, int maxIndex) {
        if (nowIndex == 0) {
            return 0x01;
        } else if (nowIndex == maxIndex - 1) {
            return 0x03;
        } else {
            return 0x02;
        }
    }

    private byte[] objectToByte(byte[] input, int length, int nowIndex, byte[] totlen, byte type) {
        byte[] sendData = new byte[1460];
        sendData[0] = totlen[0];
        sendData[1] = totlen[1];
        sendData[2] = type;
        sendData[3] = 0x00;
        int index = 0;
        int start = 1456 * nowIndex;
        int end = 1456 * (1 + nowIndex);
        if (end > length) {
            end = length;
        }
        for (int sendDataIndex = start; sendDataIndex < end; sendDataIndex++) {
            sendData[4 + index] = input[sendDataIndex];
            index += 1;
        }
        return sendData;
    }

    public byte[] RemoveCappHeader(byte[] input, int length) {
        byte[] finalByteArray = new byte[length - 4];
        System.arraycopy(input, 4, finalByteArray, 0, length - 4);
        return finalByteArray;// 변경하세요 필요하시면
    }

    public synchronized boolean Receive(byte[] input) {
        return this.GetUpperLayer(0).Receive(input);
        // 주소설정
    }

    public boolean makeHeaderData(byte[] inputData) {
        if (inputData[2] == 0x00) {
            return this.Receive(this.RemoveCappHeader(inputData, inputData.length));
        } else if (inputData[2] == (byte) 0x01 && m_sHeader.capp_type == 0x00) {
            System.out.println("accept 1st frame");
            this.gotTheFirstFrame(inputData);
        } else if (inputData[2] == (byte) 0x02 && (m_sHeader.capp_type == (byte) 0x01 || m_sHeader.capp_type == (byte) 0x02)
                && this.m_sHeader.capp_data != null) {
            System.out.println("accept 2st frame");
            m_sHeader.capp_type = inputData[2];
            this.m_sHeader.capp_data = this.appendData(m_sHeader.capp_data, inputData);
        } else if (inputData[2] == (byte) 0x03 && this.m_sHeader.capp_data != null) {
            System.out.println("accept last frame");
            this.m_sHeader.capp_data = this.appendData(m_sHeader.capp_data, inputData);
            int totalLength = this.changeByteToIntTotalLength();
            if (this.m_sHeader.capp_data.length == totalLength) {
                boolean isReceive = this.Receive(m_sHeader.capp_data);
                this.ResetHeader();
                return isReceive;
            }
            this.ResetHeader();
        }
        return false;
    }

    private void gotTheFirstFrame(byte[] inputData) {
        this.m_sHeader.capp_totlen[0] = inputData[0];
        this.m_sHeader.capp_totlen[1] = inputData[1];
        this.m_sHeader.capp_type = inputData[2];
        this.m_sHeader.capp_data = this.RemoveCappHeader(inputData, 1460);
    }

    private int changeByteToIntTotalLength() {
        int lengthOfToTal = (((int) this.m_sHeader.capp_totlen[0]) & 0xFF) << 8;
        lengthOfToTal += ((int) this.m_sHeader.capp_totlen[1]) & 0xFF;
        return lengthOfToTal;
    }

    private byte[] appendData(byte[] data, byte[] input) {
        byte[] removeHeaderData = this.RemoveCappHeader(input, 1460);
        byte[] sumData = new byte[data.length + removeHeaderData.length];
        System.arraycopy(data, 0, sumData, 0, data.length);
        System.arraycopy(removeHeaderData, 0, sumData, data.length, removeHeaderData.length);
        return sumData;
    }

    @Override
    public String GetLayerName() {
        // TODO Auto-generated method stub
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        // TODO Auto-generated method stub
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {
        // TODO Auto-generated method stub
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        // TODO Auto-generated method stub
        if (pUnderLayer == null)
            return;
        this.p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        // TODO Auto-generated method stub
        if (pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);//layer추가
        // nUpperLayerCount++;

    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}