package ipc;

import java.util.ArrayList;

public class EthernetLayer implements BaseLayer {

    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();
    private _ETHERNET_Frame ethernetHeader = new _ETHERNET_Frame();

    public EthernetLayer(String pName) {
        this.pLayerName = pName;
    }

    public void setDestNumber(byte[] array) {
        this.ethernetHeader.enet_dstaddr.setAddrData(array);
    }//dst 정보 저장

    public void setSrcNumber(byte[] array) {
        this.ethernetHeader.enet_srcaddr.setAddrData(array);
    }//src 정보 저장

    private class _ETHERNET_ADDR {
        private byte[] addr = new byte[6];

        public _ETHERNET_ADDR() {
            for (int indexOfAddr = 0; indexOfAddr < addr.length; ++indexOfAddr) {
                this.addr[indexOfAddr] = (byte) 0x00;
            }
        }

        public byte getAddrData(int index) {
            return this.addr[index];
        }

        public void setAddrData(byte[] data) {
            this.addr = data;
        }
    }

    private class _ETHERNET_Frame {
        _ETHERNET_ADDR enet_dstaddr;//dst 정보
        _ETHERNET_ADDR enet_srcaddr;//src 정보
        byte[] enet_type;
        byte[] enet_data;

        public _ETHERNET_Frame() {
            this.enet_dstaddr = new _ETHERNET_ADDR();
            this.enet_srcaddr = new _ETHERNET_ADDR();
            this.enet_type = new byte[2];
            this.enet_data = null;
        }
    }

    private byte[] etherNetDst() {
        return this.ethernetHeader.enet_dstaddr.addr;
    }

    private byte[] etherNetSrc() {
        return this.ethernetHeader.enet_srcaddr.addr;
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

    @Override
    public boolean Send(byte[] input, int length) {
        byte[] headerAddedArray = new byte[length + 14];
        int index = 0;
        while (index < 6) {
            headerAddedArray[index] = this.ethernetHeader.enet_dstaddr.getAddrData(index);
            index += 1;
        }
        while (index < 12) {
            headerAddedArray[index] = this.ethernetHeader.enet_srcaddr.getAddrData(index - 6);
            index += 1;
        }
        headerAddedArray[index] = 0x00;
        headerAddedArray[index + 1] = 0x01;
        System.arraycopy(input, 0, headerAddedArray, 14, input.length);
        return this.GetUnderLayer().Send(headerAddedArray, headerAddedArray.length);
    }

    @Override
    public synchronized boolean Receive(byte[] input) {
        if (!this.isMyAddress(input) && (this.isBoardData(input) || this.isMyConnectionData(input)) && input[12] == (byte) 0x00) {
            if (input[13] == (byte) 0x01) {
                byte[] removedHeaderData = this.removeCappHeaderData(input);
                this.sendEthernetAck(input);
                return ((ChatAppLayer) this.GetUpperLayer(0)).makeHeaderData(removedHeaderData);
            } else if (input[13] == (byte) 0x02) {
                ((ChatAppLayer) this.GetUpperLayer(0)).setAcceptTheAck(true);//다음 frame 보낼 수 있도록 코드 작성
                System.out.println("receive ack so I send other frame");
                return false;// ack이므로 상위 계층에 다음 frame 보내도록 준비만 해주게 한다.
            }
        }
        return false;
    }

    private byte[] removeCappHeaderData(byte[] input) {//header 제거
        byte[] removeCappHeader = new byte[14];
        System.arraycopy(input, 14, removeCappHeader, 0, 14);
        return removeCappHeader;
    }

    private boolean checkTheFrameData(byte[] myAddressData, byte[] inputFrameData, int inputDataStartIndex) {// add prarmeter 사용,
        for (int index = inputDataStartIndex; index < inputDataStartIndex + 6; index++) {
            if (inputFrameData[index] != myAddressData[index - inputDataStartIndex]) {
                return false;
            }
        }
        return true;
    }

    private boolean isBoardData(byte[] inputFrameData) {
        byte[] boardData = new byte[6];
        for (int index = 0; index < 6; index++) {
            boardData[index] = (byte) 0xFF;
        }
        return this.checkTheFrameData(boardData, inputFrameData, 0);
    }//board 확인 하는 코드

    private boolean isMyConnectionData(byte[] inputFrameData) {
        byte[] srcAddr = this.etherNetSrc();
        byte[] dstAddr = this.etherNetDst();
        return this.checkTheFrameData(dstAddr, inputFrameData, 6)
                && this.checkTheFrameData(srcAddr, inputFrameData, 0);
    }//connect가능한건지 판별

    private boolean isMyAddress(byte[] inputFrameData) {
        byte[] srcAddr = this.etherNetSrc();
        return this.checkTheFrameData(srcAddr, inputFrameData, 6);
    }//나의 주소인지 판별

    private boolean sendEthernetAck(byte[] inputData) {
        byte[] ackFrame = new byte[14];
        for (int indexOfFrame = 0; indexOfFrame < 6; indexOfFrame++) {
            ackFrame[indexOfFrame] = inputData[indexOfFrame + 6];
            ackFrame[indexOfFrame + 6] = inputData[indexOfFrame];
        }
        ackFrame[12] = 0x00;
        ackFrame[13] = 0x02;
        return this.GetUnderLayer().Send(ackFrame, 14);
    }//ack 보내는 코드
}
