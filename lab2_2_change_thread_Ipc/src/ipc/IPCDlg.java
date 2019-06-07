package ipc;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class IPCDlg extends JFrame implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
    BaseLayer UnderLayer;

    private static LayerManager m_LayerMgr = new LayerManager();

    private JTextField ChattingWrite;

    Container contentPane;//프레임에 연결된 컨텐트팬을 알아냄

    JTextArea ChattingArea;//화면 보여주는 위치
    JTextArea srcAddress;
    JTextArea dstAddress;

    JLabel lblsrc;//label 설정 -> 제목 같은거 설정
    JLabel lbldst;

    JButton Setting_Button;//port 번호를 입력 받은 후 입력 완료 버튼 설정
    JButton Chat_send_Button;//체팅 완료 후 입력 된 data를 완료 됬다고 확인 하는 버튼

    static JComboBox<String> NICComboBox;

    int adapterNumber = 0;

    String Text;

    public static void main(String[] args) {
        m_LayerMgr.AddLayer(new SocketLayer("Socket"));
        m_LayerMgr.AddLayer(new ChatAppLayer("ChatAppLayer"));
        m_LayerMgr.AddLayer(new IPCDlg("GUI"));
        m_LayerMgr.ConnectLayers(" Socket ( *ChatAppLayer ( *GUI ) +GUI )");
    }

    public IPCDlg(String pName) {
        pLayerName = pName;

        setTitle("IPC");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//Jpanel 에있는 거
        setBounds(250, 250, 644, 425);//Jpanel 에 존재
        contentPane = new JPanel();//객체 생성
        ((JComponent) contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));//마진 넣는거->다운 케스팅
        setContentPane(contentPane);//content pane으로지정
        contentPane.setLayout(null);

        JPanel chattingPanel = new JPanel();// chatting panel
        chattingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "chatting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        chattingPanel.setBounds(10, 5, 360, 276);
        contentPane.add(chattingPanel);
        chattingPanel.setLayout(null);

        JPanel chattingEditorPanel = new JPanel();// chatting write panel
        chattingEditorPanel.setBounds(10, 15, 340, 210);
        chattingPanel.add(chattingEditorPanel);
        chattingEditorPanel.setLayout(null);

        ChattingArea = new JTextArea();//입력 받는 위치
        ChattingArea.setEditable(false);
        ChattingArea.setBounds(0, 0, 340, 210);
        chattingEditorPanel.add(ChattingArea);// chatting edit

        JPanel chattingInputPanel = new JPanel();// chatting write panel
        chattingInputPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        chattingInputPanel.setBounds(10, 230, 250, 20);
        chattingPanel.add(chattingInputPanel);
        chattingInputPanel.setLayout(null);

        ChattingWrite = new JTextField();//객체 생성 - > 입력 받는 부분
        ChattingWrite.setBounds(2, 2, 250, 20);// 249
        chattingInputPanel.add(ChattingWrite);
        ChattingWrite.setColumns(10);// writing area

        JPanel settingPanel = new JPanel();//setting을 위한 위치 지정
        settingPanel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "setting",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        settingPanel.setBounds(380, 5, 236, 371);
        contentPane.add(settingPanel);
        settingPanel.setLayout(null);

        JPanel sourceAddressPanel = new JPanel();
        sourceAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        sourceAddressPanel.setBounds(10, 96, 170, 20);
        settingPanel.add(sourceAddressPanel);
        sourceAddressPanel.setLayout(null);

        lblsrc = new JLabel("Source Address");
        lblsrc.setBounds(10, 75, 170, 20);//위치와 높이지정
        settingPanel.add(lblsrc);//panel 추가

        srcAddress = new JTextArea();
        srcAddress.setBounds(2, 2, 170, 20);
        sourceAddressPanel.add(srcAddress);// src address

        JPanel destinationAddressPanel = new JPanel();//입력 받는 위치의 GUI 생성
        destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        destinationAddressPanel.setBounds(10, 212, 170, 20);
        settingPanel.add(destinationAddressPanel);
        destinationAddressPanel.setLayout(null);

        lbldst = new JLabel("Destination Address");
        lbldst.setBounds(10, 187, 190, 20);
        settingPanel.add(lbldst);

        dstAddress = new JTextArea();
        dstAddress.setBounds(2, 2, 170, 20);
        destinationAddressPanel.add(dstAddress);// dst address

        Setting_Button = new JButton("Setting");// setting
        Setting_Button.setBounds(80, 270, 100, 20);
        Setting_Button.addActionListener(new setAddressListener());
        settingPanel.add(Setting_Button);// setting

        Chat_send_Button = new JButton("Send");
        Chat_send_Button.setBounds(270, 230, 80, 20);
        Chat_send_Button.addActionListener(new setAddressListener());
        chattingPanel.add(Chat_send_Button);// chatting send button

        setVisible(true);
    }

    class setAddressListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            /*
             * 과제 Setting 버튼과 Send 버튼을 누를 시 행동
             * Setting 버튼 누를 시 SocketLayer에서 포트 설정
             */
            ChatAppLayer tempChatAppLayer = (ChatAppLayer) GetUnderLayer();
            SocketLayer tempSocket = (SocketLayer) tempChatAppLayer.GetUnderLayer();
            if (e.getSource() == Setting_Button) {
                if (e.getActionCommand().equals("Setting")) {
                    int src = Integer.parseInt((srcAddress.getText()).trim());
                    int dst = Integer.parseInt((dstAddress.getText()).trim());
                    tempChatAppLayer.SetEnetDstAddress(dst);
                    tempChatAppLayer.SetEnetSrcAddress(src);
                    tempSocket.setServerPort(src);
                    tempSocket.setClientPort(dst);
                    tempSocket.Receive();//입력받은 것을 다시 받아 들여야 한다
                    ((JButton) e.getSource()).setText("Reset");
                    srcAddress.setEnabled(false);
                    dstAddress.setEnabled(false);
                } else {
                    srcAddress.setEnabled(true);
                    dstAddress.setEnabled(true);
                    tempChatAppLayer.SetEnetDstAddress(0);
                    tempChatAppLayer.SetEnetSrcAddress(0);
                    tempSocket.setServerPort(0);
                    tempSocket.setClientPort(0);
                    try {
                        tempSocket.revThread().stop();
                    } catch (IOException exce) {
                        System.out.println("error of IOException");
                    }
                    srcAddress.selectAll();
                    dstAddress.selectAll();
                    srcAddress.replaceSelection("");
                    dstAddress.replaceSelection("");
                    ((JButton) e.getSource()).setText("Setting");
                }
            } else if (e.getSource() == Chat_send_Button) {
                String sendMessage = ChattingWrite.getText();
                byte[] array_of_byte = sendMessage.getBytes();
                if (tempChatAppLayer.Send(array_of_byte, array_of_byte.length)) {
                    ChattingArea.append("[SEND] : " + sendMessage + "\n");
                } else {
                    ChattingArea.append("[Error] : send reject\n");
                }
                ChattingArea.selectAll();
                ChattingArea.setCaretPosition(ChattingArea.getDocument().getLength());
                ChattingWrite.selectAll();
                ChattingWrite.replaceSelection("");
            }
        }
    }

    public boolean Receive(byte[] input) {
        /*
         * 	과제 채팅 화면에 채팅 보여주기
         */
        String outputStr = new String(input);
        ChattingArea.append("[RECV] : " + outputStr + "\n");
        ChattingArea.selectAll();
        ChattingArea.setCaretPosition(ChattingArea.getDocument().getLength());

        return true;
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
        this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);//top에 넣는다
        // nUpperLayerCount++;
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
    public void SetUpperUnderLayer(BaseLayer pUULayer) {
        this.SetUpperLayer(pUULayer);
        pUULayer.SetUnderLayer(this);
    }
}