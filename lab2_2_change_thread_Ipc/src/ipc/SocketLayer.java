package ipc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;

public class SocketLayer implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();
    public int dst_port;
    public int src_port;
    private Receive_Thread _revThread;

    public Receive_Thread revThread() {
        return this._revThread;
    }

    public SocketLayer(String pName) {
        // super(pName);
        pLayerName = pName;
    }

    public void setClientPort(int dstAddress) {
        this.dst_port = dstAddress; //  참고사항 IPCDlg setting 버튼을 누르면 포트 설정
    }

    public void setServerPort(int srcAddress) {
        this.src_port = srcAddress; // 참고사항 IPCDlg setting 버튼을 누르면 포트 설정
    }

    public boolean Send(byte[] input, int length) {//각 server로 넘겨줌
        try (Socket client = new Socket()) { // 클라이언트 초기화
            InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", dst_port);// 상대방포트 "127.0.0.1"은 루프백 IP = 본인 IP를 뜻함.
            // 접속 -> 해당 Socket의 host이름과
            client.connect(ipep);//연걸 시도

            try (OutputStream sender = client.getOutputStream()) {//작성하기 위해서
                sender.write(input, 0, length);//해당 위치에 작성 0~length 길이만큼
            }//작성 성공
        } catch (Throwable e) {//보내기 실패
            return false;
        }
        return true;
    }

    public boolean Receive() {//thread를 메세지를 받는 것
        this._revThread = new Receive_Thread(this.GetUpperLayer(0), src_port);
        Thread thread = new Thread(this._revThread);
        thread.start();

        return false;
    }

    @Override
    public void SetUnderLayer(BaseLayer pUnderLayer) {
        // TODO Auto-generated method stub
        if (pUnderLayer == null)
            return;
        p_UnderLayer = pUnderLayer;
    }

    @Override
    public void SetUpperLayer(BaseLayer pUpperLayer) {
        // TODO Auto-generated method stub
        if (pUpperLayer == null)
            return;
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
        // nUpperLayerCount++;
    }

    @Override
    public String GetLayerName() {
        // TODO Auto-generated method stub
        return pLayerName;
    }

    @Override
    public BaseLayer GetUnderLayer() {
        if (p_UnderLayer == null)
            return null;
        return p_UnderLayer;
    }

    @Override
    public BaseLayer GetUpperLayer(int nindex) {//제일 처음위치의 layout을 받는다
        // TODO Auto-generated method stub
        if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
            return null;
        return p_aUpperLayer.get(nindex);
    }

    @Override
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }


}

class Receive_Thread implements Runnable {
    byte[] data;
    BaseLayer UpperLayer;//layer 지정 -> 상위 계층
    int server_port;//port 번호 지정
    private Socket _client;
    private ServerSocket _server;

    public Receive_Thread(BaseLayer m_UpperLayer, int src_port) {//생성자로 해당 위치의 port랑 port 번호지정
        // TODO Auto-generated constructor stub
        UpperLayer = m_UpperLayer;
        server_port = src_port;
    }

    @Override
    public void run() {//실행 부분 -> Runable override
        try {
            this._server = new ServerSocket();
            // 서버 초기화

            InetSocketAddress ipep = new InetSocketAddress("127.0.0.1", server_port);
            this._server.bind(ipep);
            System.out.println("Initialize complate");

            // LISTEN 대기
            // reciever 스트림 받아오기
            // 자동 close
        } catch (SocketException exeption) {
            return;
        } catch (Throwable e) {
            return;
        }
        while (true) {
            try {
                _client = this._server.accept();//thread가 다른 요청을 기다림 -> 다른 client가 요청 할 때까지 기다림
            } catch (IOException e) {
                System.out.println("Server accept reject so terminate the thread");
                return;
            }
            System.out.println("client is Connected");//client연결
            try (InputStream reciever = _client.getInputStream()) {//client로 부터 입력 받음
                // 클라이언트로부터 메시지 받기
                // byte 데이터
                data = new byte[1528]; // Ethernet Maxsize + Ethernet Headersize;
                reciever.read(data, 0, data.length);//0~data의 길이 만큼 data를 받아 들임
                UpperLayer.Receive(data);//상위 에서 받아 들여짐
            } catch (IOException e) {
                return;
            }
        }
    }

    public void stop() throws IOException {//여기 오류 부분은 GUI에서 제거 해 준다
        if (!Objects.isNull(this._server)) {
            this._server.close();
            System.out.println(this._server.getLocalPort());
            System.out.println("server is terminated");
        }
        if (!Objects.isNull(this._client)) {
            this._client.close();
            System.out.println("client is terminated");
        }
    }
}