package ipc;

import java.util.ArrayList;

public class ChatAppLayer implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();
    private _CAPP_HEADER m_sHeader = new _CAPP_HEADER();
    private boolean _acceptTheAck;

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

    public void setAcceptTheAck(boolean newAcceptTheAck) {
        this._acceptTheAck = newAcceptTheAck;
    }//ack여부 판별

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

    private byte[] objToByteSpliteData(byte[] input, int start, int last, byte[] totlen, byte type) {
        byte[] sendData = new byte[last - start + 4];
        sendData[0] = totlen[0];
        sendData[1] = totlen[1];
        sendData[2] = type;
        sendData[3] = 0x00;
        int index = 0;
        for (int sendDataIndex = start; sendDataIndex < last; sendDataIndex++) {
            sendData[4 + index] = input[sendDataIndex];
            index += 1;
        }
        return sendData;
    }

    public boolean Send(byte[] input, int length) {
        byte[] totalLength = this.totalLength(length);
        this.m_sHeader.capp_totlen = totalLength;
        System.out.println("byte length : " + totalLength[0] + ", " + totalLength[1]);
        int lengthOfToTal = (((int) this.m_sHeader.capp_totlen[0]) & 0xFF) << 8;
        lengthOfToTal += ((int) this.m_sHeader.capp_totlen[1]) & 0xFF;
        System.out.println("ChatApp length, byte length Data : " + length + ", " + lengthOfToTal);
        byte[] arrayOfAddedHadder;
        boolean isSend;
        if (length <= 10) {
            arrayOfAddedHadder = this.objToByteSpliteData(input, 0, input.length, totalLength, (byte) 0x00);
            isSend = this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length);
            if (!isSend) {
                return false;
            }
            while (isSend && !this._acceptTheAck) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
            }//ack 대기
            return true;
        }
        int indexOfLength = 0;
        arrayOfAddedHadder = this.objToByteSpliteData(input, indexOfLength, indexOfLength + 10, totalLength, (byte) 0x01);
        this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length);
        indexOfLength += 10;
        while (true) {
            if (!this._acceptTheAck) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                continue;
            }//0x02랑 0x01의 ack를 받아 들이기 위한 무한 루프
            if (length - indexOfLength > 10) {
                arrayOfAddedHadder = this.objToByteSpliteData(input, indexOfLength, indexOfLength + 10, totalLength, (byte) 0x02);
                this._acceptTheAck = false;
                if (!this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length)) {
                    return false;
                }
            } else {
                break;
            }
            indexOfLength += 10;
        }//type이 0x02인 경우

        if (length - indexOfLength == 10) {
            arrayOfAddedHadder = this.objToByteSpliteData(input, indexOfLength, indexOfLength + 10, totalLength, (byte) 0x03);
            if (!this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length)) {
                return false;
            }
        } else {
            arrayOfAddedHadder = this.objToByteSpliteData(input, indexOfLength, input.length, totalLength, (byte) 0x03);
            if (!this.GetUnderLayer().Send(arrayOfAddedHadder, arrayOfAddedHadder.length)) {
                return false;
            }
        }
        while (!this._acceptTheAck) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }

        return true;
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

    public boolean makeTheHeaderData(byte[] inputData) {
        if (this.m_sHeader.capp_data != null) {
            System.out.println("before data : " + new String(this.m_sHeader.capp_data));
        }
        System.out.println("add data : " + new String(inputData));
        if (inputData[2] == 0x00) {
            this.ResetHeader();
            return this.Receive(this.RemoveCappHeader(inputData, inputData.length));
        } else if (inputData[2] == 0x01 && m_sHeader.capp_type == 0x00) {
            System.out.println("accept 1st frame");
            this.m_sHeader.capp_totlen[0] = inputData[0];
            this.m_sHeader.capp_totlen[1] = inputData[1];
            this.m_sHeader.capp_data = this.RemoveCappHeader(inputData, 14);
            m_sHeader.capp_type = (byte) 0x01;
        } else if (inputData[2] == (byte) 0x02 && (m_sHeader.capp_type == (byte) 0x01 || m_sHeader.capp_type == (byte) 0x02) && this.m_sHeader.capp_data != null) {
            System.out.println("accept 2st frame");
            this.m_sHeader.capp_data = this.appendData(m_sHeader.capp_data, inputData);
            m_sHeader.capp_type = (byte) 0x02;
        } else if (inputData[2] == (byte) 0x03 && this.m_sHeader.capp_data != null) {
            System.out.println("accept last frame");
            this.m_sHeader.capp_data = this.appendDataLast(m_sHeader.capp_data, inputData);
            int lengthOfToTal = (((int) this.m_sHeader.capp_totlen[0]) & 0xFF) << 8;
            lengthOfToTal += ((int) this.m_sHeader.capp_totlen[1]) & 0xFF;
            boolean isReceive = false;
            System.out.println("correct length and accept" + m_sHeader.capp_data.length + ", " + lengthOfToTal);
            System.out.println("result : " + new String(m_sHeader.capp_data));
            if (lengthOfToTal <= this.m_sHeader.capp_data.length && this.m_sHeader.capp_data.length < lengthOfToTal + 10) {//오차 범위가 생김
                isReceive = this.Receive(m_sHeader.capp_data);
                System.out.println("correct length and accept" + m_sHeader.capp_data.length + ", " + lengthOfToTal);
            } else {
                System.out.println("fail corret length");
            }
            this.ResetHeader();
            return isReceive;
        }
        return false;
    }

    private byte[] appendData(byte[] data, byte[] input) {
        System.out.println("datalength : " + data.length);
        System.out.println("before remove header length: " + input.length);
        byte[] removeHeaderData = this.RemoveCappHeader(input, 14);
        byte[] sumData = new byte[data.length + removeHeaderData.length];
        System.out.println("after remove header length , changeData length : " + removeHeaderData.length + ", " + sumData.length);
        System.arraycopy(data, 0, sumData, 0, data.length);
        System.arraycopy(removeHeaderData, 0, sumData, data.length, removeHeaderData.length);
        return sumData;
    }

    private byte[] appendDataLast(byte[] data, byte[] input) {
        byte[] removeHeaderData;
        if (input.length > 14) {
            removeHeaderData = this.RemoveCappHeader(input, 14);
        } else {
            removeHeaderData = this.RemoveCappHeader(input, input.length);
        }
        byte[] sumData = new byte[data.length + removeHeaderData.length];
        System.out.println("after remove header length , changeData length : " + removeHeaderData.length + ", " + sumData.length);
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