package ipc;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();

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

    _CAPP_HEADER m_sHeader = new _CAPP_HEADER();

    public ChatAppLayer(String pName) {
        // super(pName);
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        ResetHeader();
    }

    public void ResetHeader() {//header를 모두 0으로 초기화
        for (int i = 0; i < 2; i++) {
            m_sHeader.capp_totlen[i] = (byte) 0x00;
        }
    }

    public byte[] ObjToByte(_CAPP_HEADER Header, byte[] input, int length) {
        byte[] buf = new byte[length + 4];

        buf[0] = Header.capp_totlen[0];
        buf[1] = Header.capp_totlen[1];
        buf[2] = Header.capp_type;
        buf[3] = Header.capp_unused;

        for (int i = 0; i < length; i++) {
            buf[4 + i] = input[i];
        }
        return buf;
    }

    public boolean Send(byte[] input, int length) {
        byte[] arrayOfAddedHadder = this.ObjToByte(this.m_sHeader, input, length);
        return this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length);
    }

    public byte[] RemoveCappHeader(byte[] input, int length) {
        byte[] finalByteArray = new byte[length - 4];
        System.arraycopy(input, 4, finalByteArray, 0, length - 4);
        return finalByteArray;// 변경하세요 필요하시면
    }

    public synchronized boolean Receive(byte[] input) {
       /*
        if (input[0] != this.m_sHeader.capp_totlen[0] || input[1] != this.m_sHeader.capp_totlen[1]
                || input[2] != this.m_sHeader.capp_type || input[3] != this.m_sHeader.capp_unused) {
            System.out.println("fail At ChatApp");
            return false;
        }*/
        byte[] data;
        data = RemoveCappHeader(input, input.length);
        System.out.println("can take At ChatApp");

        return this.GetUpperLayer(0).Receive(data);
        // 주소설정
    }

    byte[] intToByte4(int value) {//다음 과제 내역
        byte[] temp = new byte[4];
        temp[0] |= (byte) ((value & 0xFF000000) >> 24);
        temp[1] |= (byte) ((value & 0xFF0000) >> 16);
        temp[2] |= (byte) (value & 0xFF00);//8bit 저장
        temp[3] |= (byte) ((value) & 0xFF);

        return temp;
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