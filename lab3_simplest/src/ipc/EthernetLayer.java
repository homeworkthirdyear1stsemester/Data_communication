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
        headerAddedArray[index + 1] = 0x00;
        System.arraycopy(input, 0, headerAddedArray, 14, input.length);
        return this.GetUnderLayer().Send(headerAddedArray, input.length);
    }

    @Override
    public boolean Receive(byte[] input) {
        System.out.println("Accept data");
        boolean isBroad = true;
        for (int indextemp = 0; indextemp < 6; indextemp++) {
            if (input[indextemp] != (byte) 0xFF) {
                isBroad = false;
            }
        }
        if (isBroad && input[12] == this.ethernetHeader.enet_type[0] && input[13] == this.ethernetHeader.enet_type[1]) {
            byte[] removeCapHeader = new byte[input.length - 14];
            System.arraycopy(input, 14, removeCapHeader, 0, removeCapHeader.length);
            return this.GetUpperLayer(0).Receive(removeCapHeader);
        }

        int index = 0;
        while (index < 6) {
            if (input[index] != this.ethernetHeader.enet_srcaddr.getAddrData(index)) {
                System.out.println("fail At EtherNet");
                return false;
            }
            index += 1;
        }
        while (index < 12) {
            if (input[index] != this.ethernetHeader.enet_dstaddr.getAddrData(index - 6)) {
                System.out.println("fail At EtherNet");
                return false;
            }
            index += 1;
        }
        if (input[index] != this.ethernetHeader.enet_type[0] || input[index + 1] != this.ethernetHeader.enet_type[1]) {
            System.out.println("fail At EtherNet");
            return false;
        }
        System.out.println("can get At EtherNet");
        byte[] removeCapHeader = new byte[input.length - 14];
        System.arraycopy(input, 14, removeCapHeader, 0, removeCapHeader.length);
        return this.GetUpperLayer(0).Receive(removeCapHeader);
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
