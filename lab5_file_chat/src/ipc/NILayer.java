package ipc;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class NILayer implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();

    int m_iNumAdapter;//네트워크 어뎁터 인덱스
    public Pcap m_AdapterObject;//네트워크 어뎁터 객체
    public PcapIf device;//네트워크 인터페이스 객체
    public List<PcapIf> m_pAdapterList;//네트워크 인터페이스 목록
    StringBuilder errbuf = new StringBuilder();//에러 버퍼
    private Receive_Thread thread;
    private Receive_Thread fileThread;

    public void setThreadIsRun(boolean isRun) {
        this.thread.setIsRun(isRun);
    }

    public NILayer(String pName) {
        this.pLayerName = pName;
        m_pAdapterList = new ArrayList<>();//Pcap을 담는 List
        m_iNumAdapter = 0;
        this.SetAdapterList();
    }

    public void SetAdapterList() {
        int r = Pcap.findAllDevs(m_pAdapterList, errbuf);
        //현재 컴퓨터에 존재하는 모든 네트워크 어뎁터 목록 가져오기
        if (r == Pcap.NOT_OK || m_pAdapterList.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
            return;
        }//네트워크 어뎁터가 하나도 존재하지 않을 경우 에러 처리
    }

    public void setAdapterNumber(int iNum) {
        this.m_iNumAdapter = iNum;//어뎁터 번호 초기화
        this.PacketStartDriver();//패킷 드라이버 시작 함수
        this.Receive();//패킷 수신함수
    }

    public void PacketStartDriver() {//패킷 드라이버 시작 함수
        int snaplen = 64 * 1024;//팻킷 캡처 길이
        int flags = Pcap.MODE_PROMISCUOUS;//모든 패킷 캡처
        int timeout = 10 * 1000;//패킷 캡처 시간
        this.m_AdapterObject = Pcap.openLive(this.m_pAdapterList.get(this.m_iNumAdapter).getName(),
                snaplen, flags, timeout, this.errbuf);//pcap 작동 시작
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
    public boolean Receive() {//쓰레드 객체 생성
        thread = new Receive_Thread(this.m_AdapterObject, this.GetUpperLayer(0));
        Thread obj = new Thread(thread);
        obj.start();

        return false;
    }

    public boolean Send(byte[] input, int length) {
        ByteBuffer buf = ByteBuffer.wrap(input);
        if (m_AdapterObject.sendPacket(buf) != Pcap.OK) {
            System.err.println(m_AdapterObject.getErr());
            return false;
        }
        return true;
    }
}

class Receive_Thread implements Runnable {
    byte[] data;
    Pcap AdapterObject;
    BaseLayer UpperLayer;
    private boolean isRun = true;

    public void setIsRun(boolean isRun) {
        this.isRun = isRun;
    }

    public Receive_Thread(Pcap m_AdapterObject, BaseLayer m_UpperLayer) {
        this.AdapterObject = m_AdapterObject;
        this.UpperLayer = m_UpperLayer;
    }//객체 초기화

    @Override
    public void run() {
        while (true) {
            if (!isRun) {
                System.out.println("Thread is terminated");
                return;
            }
            PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
                public void nextPacket(PcapPacket packet, String user) {
                    data = packet.getByteArray(0, packet.size());//패킷의 데이터 바이트배열와 패킷 크기를 알아냄
                    UpperLayer.Receive(data);//상위 객체의 receive호출
                }
            };
            AdapterObject.loop(10000, jpacketHandler, "");
        }
    }
}
