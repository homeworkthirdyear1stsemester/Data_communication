package ipc;

import org.jnetpcap.PcapIf;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class StopWaitDlg extends JFrame implements BaseLayer {
    public int nUpperLayerCount = 0;
    public String pLayerName = null;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();
    BaseLayer UnderLayer;

    private static LayerManager m_LayerMgr = new LayerManager();

    private JTextField ChattingWrite;

    private ArrayList<MacAndName> storageOfMacList = new ArrayList<>();

    Container contentPane;//프레임에 연결된 컨텐트팬을 알아냄

    JTextArea ChattingArea;//화면 보여주는 위치
    JTextArea srcAddress;
    JTextArea dstAddress;

    JLabel lblsrc;//label 설정 -> 제목 같은거 설정
    JLabel lbldst;
    JLabel lblNICLabel;


    JButton Setting_Button;//port 번호를 입력 받은 후 입력 완료 버튼 설정
    JButton Chat_send_Button;//체팅 완료 후 입력 된 data를 완료 됬다고 확인 하는 버튼

    JComboBox<String> NICComboBox;

    int adapterNumber = 0;

    String Text;

    public static void main(String[] args) {
        m_LayerMgr.AddLayer(new NILayer("NI"));
        m_LayerMgr.AddLayer(new EthernetLayer("Ethernet"));
        m_LayerMgr.AddLayer(new ChatAppLayer("ChatAppLayer"));
        m_LayerMgr.AddLayer(new StopWaitDlg("GUI"));
        m_LayerMgr.ConnectLayers(" NI ( *Ethernet ( *ChatAppLayer ( *GUI ) ) )");
    }

    public StopWaitDlg(String pName) {
        pLayerName = pName;

        setTitle("Stop & Wait Protocol");
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
        sourceAddressPanel.setBounds(10, 140, 170, 20);
        settingPanel.add(sourceAddressPanel);
        sourceAddressPanel.setLayout(null);

        lblsrc = new JLabel("Source Mac Address");
        lblsrc.setBounds(10, 115, 170, 20);//위치와 높이지정
        settingPanel.add(lblsrc);//panel 추가

        srcAddress = new JTextArea();
        srcAddress.setBounds(2, 2, 170, 20);
        sourceAddressPanel.add(srcAddress);// src address

        JPanel destinationAddressPanel = new JPanel();//입력 받는 위치의 GUI 생성
        destinationAddressPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        destinationAddressPanel.setBounds(10, 212, 170, 20);
        settingPanel.add(destinationAddressPanel);
        destinationAddressPanel.setLayout(null);


        NILayer tempNI = (NILayer) m_LayerMgr.GetLayer("NI");
        if (tempNI != null) {
            for (int indexOfPcapList = 0; indexOfPcapList < tempNI.m_pAdapterList.size(); indexOfPcapList += 1) {
                final PcapIf inputPcapIf = tempNI.m_pAdapterList.get(indexOfPcapList);//NILayer의 List를 가져옴
                byte[] macAdress = null;//객체 지정
                try {
                    macAdress = inputPcapIf.getHardwareAddress();
                } catch (IOException e) {
                    System.out.println("Address error is happen");
                }//에러 표출
                if (macAdress == null) {
                    continue;
                }
                this.storageOfMacList.add(new MacAndName(macAdress, inputPcapIf.getDescription(), this.macByteToString(macAdress), indexOfPcapList));
            }//해당 ArrayList에 Mac주소 포트번호 이름, byte배열, Mac주소 String으로 변환한 값, NILayer의 adapterNumber를 저장해 준다.
        }

        String[] nameOfConnection = new String[this.storageOfMacList.size()];
        for (int index = 0; index < this.storageOfMacList.size(); index++) {
            nameOfConnection[index] = this.storageOfMacList.get(index).macName;
        }

        this.NICComboBox = new JComboBox(nameOfConnection);
        this.NICComboBox.setBounds(10, 65, 170, 20);
        settingPanel.add(this.NICComboBox);
        this.NICComboBox.addActionListener(new setAddressListener());

        lblNICLabel = new JLabel("NIC 선택");
        lblNICLabel.setBounds(10, 35, 170, 20);
        settingPanel.add(lblNICLabel);

        lbldst = new JLabel("Destination Mac Address");
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

    private String macByteToString(byte[] mac) {
        final StringBuilder sb = new StringBuilder();

        for (byte nowByte : mac) {
            if (sb.length() != 0) {
                sb.append("-");
            }
            if (0 <= nowByte && nowByte < 16) {
                sb.append("0");
            }
            sb.append(Integer.toHexString((nowByte < 0) ? nowByte + 256 : nowByte).toUpperCase());
        }
        return sb.toString();
    }

    private byte[] strToByte(String macAddress) {
        byte[] hexTobyteArrayMacAdress = new byte[6];
        String changeMacAddress = macAddress.replaceAll("-", "");
        for (int index = 0; index < 12; index += 2) {
            hexTobyteArrayMacAdress[index / 2] = (byte) ((Character.digit(changeMacAddress.charAt(index), 16) << 4)
                    + Character.digit(changeMacAddress.charAt(index + 1), 16));
        }
        return hexTobyteArrayMacAdress;
    }

    class setAddressListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ChatAppLayer tempChatAppLayer = (ChatAppLayer) GetUnderLayer();
            EthernetLayer tempEthernetLayer = (EthernetLayer) tempChatAppLayer.GetUnderLayer();
            NILayer tempNILayer = (NILayer) tempEthernetLayer.GetUnderLayer();
            if (e.getSource() == Setting_Button) {
                if (e.getActionCommand().equals("Setting")) {
                    String srcMacNumber = srcAddress.getText();
                    for (int index = 0; index < storageOfMacList.size(); index++) {
                        MacAndName tempMacObj = storageOfMacList.get(index);
                        if (srcMacNumber.equals(tempMacObj.macAddressStr)) {
                            tempEthernetLayer.setSrcNumber(tempMacObj.macAddress);
                            adapterNumber = tempMacObj.portNumber;
                            System.out.print("내가 지정한 srcMac 주소 : ");
                            for (byte nowData : tempMacObj.macAddress) {
                                System.out.print(nowData + ", ");
                            }
                            System.out.println();
                            System.out.println("내가 지정한 port 번호 : " + adapterNumber);
                            break;
                        }
                    }

                    byte[] dstMacAddress = strToByte(dstAddress.getText());//입력된 상대 mac주소 byte 배열로 만들기

                    System.out.print("내가 지정한 dstMac 주소 : ");
                    for (int index = 0; index < dstMacAddress.length; index++) {
                        System.out.print(dstMacAddress[index] + ", ");
                    }
                    System.out.println();

                    tempEthernetLayer.setDestNumber(dstMacAddress);//입력
                    tempNILayer.setAdapterNumber(adapterNumber);//Pcap 객체 생성 및 모든 data를 받을 준비르 하는 메소드 -> receive가 내부에 포함됨
                    ((JButton) e.getSource()).setText("Reset");
                    srcAddress.setEnabled(false);
                    dstAddress.setEnabled(false);
                    NICComboBox.setEnabled(false);

                    System.out.println("etherNet에서의 data보기");
                    System.out.print("내가 지정한 dst번호 ethernet : ");
                    byte[] tempEhternetOfByte = tempEthernetLayer.etherNetDst();
                    for (int index = 0; index < tempEhternetOfByte.length; index++) {
                        System.out.print(tempEhternetOfByte[index] + ", ");
                    }
                    System.out.println();

                    System.out.print("내가 지정한 src번호 ethernet : ");
                    tempEhternetOfByte = tempEthernetLayer.etherNetSrc();
                    for (int index = 0; index < tempEhternetOfByte.length; index++) {
                        System.out.print(tempEhternetOfByte[index] + ", ");
                    }
                    System.out.println();
                } else {
                    tempNILayer.setThreadIsRun(false);
                    byte[] resetByteArray = new byte[6];
                    tempEthernetLayer.setDestNumber(resetByteArray);
                    tempEthernetLayer.setSrcNumber(resetByteArray);
                    srcAddress.setEnabled(true);
                    dstAddress.setEnabled(true);
                    NICComboBox.setEnabled(true);
                    srcAddress.selectAll();
                    dstAddress.selectAll();
                    srcAddress.replaceSelection("");
                    dstAddress.replaceSelection("");
                    ((JButton) e.getSource()).setText("Setting");
                }
            } else if (e.getSource() == Chat_send_Button) {
                String sendMessage = ChattingWrite.getText();
                if (sendMessage.equals("")) {
                    return;
                }
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
            } else if (e.getSource() == NICComboBox) {
                int index = NICComboBox.getSelectedIndex();
                srcAddress.setText(storageOfMacList.get(index).macAddressStr);
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

    private class MacAndName {
        public byte[] macAddress;
        public String macName;
        public String macAddressStr;
        public int portNumber;

        public MacAndName(byte[] macAddress, String macName, String macAddressStr, int portNumberOfMac) {
            this.macAddress = macAddress;
            this.macName = macName;
            this.macAddressStr = macAddressStr;
            this.portNumber = portNumberOfMac;
        }
    }
}