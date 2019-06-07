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
        boolean checkIsBoard = this.isBoard(input);
        if ((checkIsBoard || this.checkMyConnection(input)) && input[12] == (byte) 0x00) {
            if (input[13] == (byte) 0x01) {
                byte[] removedHeaderData = this.removeCappHeaderData(input);
                if (checkIsBoard) {
                    this.sendBroadAck(input);
                } else {
                    this.sendEhternetAck();
                }
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

    private boolean isBoard(byte[] input) {//브로드 케스팅인지 판별
        for (int indexOfInputData = 0; indexOfInputData < 6; indexOfInputData++) {
            if (input[indexOfInputData] != (byte) 0xFF) {
                return false;
            }
        }
        return !this.checkMyEthernetBoardSrcAddress(6, input);
    }

    private boolean checkMyConnection(byte[] input) {//연결된 것과 동일한지 판별
        int index = 0;
        while (index < 6) {
            if (input[index] != this.ethernetHeader.enet_srcaddr.getAddrData(index)) {
                return false;
            }
            index += 1;
        }
        return this.checkMyEthernetDstAddress(6, input);
    }

    private boolean sendBroadAck(byte[] boardData) {
        byte[] headerAddedArray = new byte[14];
        int index = 0;
        while (index < 6) {
            headerAddedArray[index] = boardData[index + 6];
            index += 1;
        }
        while (index < 12) {
            headerAddedArray[index] = (byte) 0xFF;
            index += 1;
        }
        headerAddedArray[index] = 0x00;
        headerAddedArray[index + 1] = 0x02;
        return this.GetUnderLayer().Send(headerAddedArray, 14);
    }

    private boolean sendEhternetAck() {//ack 만들어서 ack receive에 넣어준다
        byte[] headerAddedArray = new byte[14];
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
        headerAddedArray[index + 1] = 0x02;
        return this.GetUnderLayer().Send(headerAddedArray, 14);
    }

    private boolean checkMyEthernetDstAddress(int startAddressIndex, byte[] message) {
        int indexOfMyAddressByteArrayIndex = 0;
        for (int indexOfStart = startAddressIndex; indexOfStart < startAddressIndex + 6; indexOfStart++) {
            if (message[indexOfStart] != this.ethernetHeader.enet_dstaddr.getAddrData(indexOfMyAddressByteArrayIndex)) {
                return false;
            }
            indexOfMyAddressByteArrayIndex++;
        }
        return true;
    }//상대주소를 체크 나랑 연결 되어있는지를 판별해 주는 역할을 하는 메소드

    private boolean checkMyEthernetBoardSrcAddress(int startAddressIndex, byte[] message) {
        int indexOfMyAddressByteArrayIndex = 0;
        for (int indexOfStart = startAddressIndex; indexOfStart < startAddressIndex + 6; indexOfStart++) {
            if (message[indexOfStart] != this.ethernetHeader.enet_srcaddr.getAddrData(indexOfMyAddressByteArrayIndex)) {
                return false;
            }
            indexOfMyAddressByteArrayIndex++;
        }
        return true;
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

    public byte[] etherNetDst() {
        return this.ethernetHeader.enet_dstaddr.addr;
    }

    public byte[] etherNetSrc() {
        return this.ethernetHeader.enet_srcaddr.addr;
    }
}
