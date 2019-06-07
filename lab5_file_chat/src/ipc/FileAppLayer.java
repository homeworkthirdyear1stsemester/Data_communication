package ipc;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileAppLayer implements BaseLayer {
    private _FAPP_HEADER _fileHeaderForSend;
    private _FAPP_HEADER _fileHeaderForReceive;

    public int nUpperLayerCount = 0;
    public String pLayerName;
    public BaseLayer p_UnderLayer = null;
    public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<>();

    private File sendFile;
    private File storeFile;

    private List<FileData> fileData;
    private List<byte[]> fileDataOfArray;

    public class _FAPP_HEADER {
        byte[] fapp_totlen;
        byte[] fapp_type;
        byte fapp_msg_type;
        byte ed;
        byte[] fapp_seq_num;
        byte[] fapp_data;

        private _FAPP_HEADER() {
            this.fapp_totlen = new byte[4];
            this.fapp_type = new byte[2];//0x00 : file명, 정보, 0x01 중간 data, 0x02 : 마지막 data
            this.fapp_msg_type = 0x00;//0x00 : sendFile 명, 0x01 : data
            this.ed = 0x00;
            this.fapp_seq_num = new byte[4];
            this.fapp_data = null;
        }
    }

    public void setTheHeaderOfFile() {//sendFile Layer에 header 초기화
        this._fileHeaderForSend.fapp_totlen = this.totalLengh(this.sendFile.length());
        this._fileHeaderForSend.fapp_type[0] = (byte) 0x00;
        this._fileHeaderForSend.fapp_type[1] = (byte) 0x00;
        this._fileHeaderForSend.fapp_msg_type = (byte) 0x00;
    }

    public void setSendFile(File newFile) {
        this.sendFile = newFile;
    }

    public FileAppLayer(String pName) {
        // super(pName);
        // TODO Auto-generated constructor stub
        pLayerName = pName;
        this.resetHeaderForSend();
        this.resetHeaderForReceive();
    }

    private void resetHeaderForSend() {
        this._fileHeaderForSend = new _FAPP_HEADER();
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

    public boolean sendData() {
        FileSimplestDlg upperLayer = (FileSimplestDlg) this.GetUpperLayer(0);
        try (FileInputStream fileInputStream = new FileInputStream(this.sendFile)) {
            BufferedInputStream fileReader = new BufferedInputStream(fileInputStream);
            int maxIndex = this.frameSize(this.sendFile.length());//파일의 최대 크기
            byte[] inputData = new byte[1448];
            if (!this.sendFirstFrame()) {
                return false;
            }
            int index = 1;
            while (fileReader.read(inputData) != -1 && index != maxIndex + 1) {
                if (!this.sendFrames(inputData, index, maxIndex)) {
                    return false;
                }
                upperLayer.progressBar.setValue((int) ((index / (double) maxIndex) * 100));
                index++;
                this.waitThread();
            }
            fileInputStream.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean sendFrames(byte[] inputData, int index, int maxIndex) {
        if (index == maxIndex) {
            System.out.println("last Frame");
            this._fileHeaderForSend.fapp_type[1] = 0x02;
        }
        this._fileHeaderForSend.fapp_seq_num = this.getSequceNumberToByteArray(index);//배열
        byte[] sendData = this.objectToByte(inputData);
        this.waitForOtherFrameSendAndSetEhterNetHeaderType();//file명을 보내는 경우를 넣어 줘야한다.
        System.out.println(index);
        return this.Send(sendData, sendData.length);
    }

    private void waitThread() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] getSequceNumberToByteArray(int number) {
        int getNumberToByteArray = number;
        byte[] sequceNumber = new byte[4];
        for (int index = 0; index < 4; index++) {
            sequceNumber[index] = (byte) (getNumberToByteArray & 0xFF);
            getNumberToByteArray = getNumberToByteArray >> 8;
        }
        return sequceNumber;
    }

    private int getByteArrayToSeqenceNumber(byte[] inputData) {
        int getByteToNumber = 0;
        for (int index = 11; index > 7; index--) {
            getByteToNumber = getByteToNumber << 8;
            getByteToNumber += (((int) inputData[index]) & 0xFF);
        }
        return getByteToNumber;
    }

    private boolean sendFirstFrame() {
        this.waitForOtherFrameSendAndSetEhterNetHeaderType();//file명을 보내는 경우를 넣어 줘야한다.
        String fileName = this.sendFile.getName();
        byte[] fileNameToByteArray = this.objectToByte(fileName.getBytes());
        this._fileHeaderForSend.fapp_type[1] = 0x01;
        this._fileHeaderForSend.fapp_msg_type = (byte) 0x01;
        return this.Send(fileNameToByteArray, fileNameToByteArray.length);
    }

    @Override
    public boolean Send(byte[] fileData, int length) {
        return this.GetUnderLayer().Send(fileData, length);
    }

    private int frameSize(long fileSize) {
        int maxIndex = (int) (fileSize / 1448);
        maxIndex = fileSize % 1448 > 0 ? maxIndex + 1 : maxIndex;
        return maxIndex;
    }

    private byte[] objectToByte(byte[] inputData) {
        byte[] sendData = new byte[1460];
        for (int index = 0; index < 4; index++) {
            sendData[index] = this._fileHeaderForSend.fapp_totlen[index];
            sendData[index + 8] = this._fileHeaderForSend.fapp_seq_num[index];
        }
        sendData[4] = this._fileHeaderForSend.fapp_type[0];
        sendData[5] = this._fileHeaderForSend.fapp_type[1];
        sendData[6] = this._fileHeaderForSend.fapp_msg_type;
        sendData[7] = this._fileHeaderForSend.ed;

        System.arraycopy(inputData, 0, sendData, 12, inputData.length);

        return sendData;
    }

    private void waitForOtherFrameSendAndSetEhterNetHeaderType() {//ehternet에서 초기화 해 줄 경우 쓰레드 상에서 오류 header type문제 해결
        EthernetLayer underLayer = (EthernetLayer) this.GetUnderLayer();
        while (!((underLayer).ethernetHeaderGetType(0) == 0x00
                && (underLayer).ethernetHeaderGetType(1) == 0x00)) {
            this.waitThread();
        }
        byte[] newTypeOfEthernet = new byte[2];
        newTypeOfEthernet[0] = (byte) 0x20;
        newTypeOfEthernet[1] = (byte) 0x90;
        underLayer.setEthernetHeaderType(newTypeOfEthernet);
    }

    private byte[] totalLengh(long fileLength) {//byte 배열 형성
        long lengthOfFileForDivide = fileLength;
        byte[] totalLengthOfFile = new byte[4];
        for (int index = 0; index < 4; index++) {
            totalLengthOfFile[index] = ((byte) (lengthOfFileForDivide & 0xFF));
            lengthOfFileForDivide = lengthOfFileForDivide >> 8;
        }
        return totalLengthOfFile;
    }//최대 길이

    private long changeByteToIntTotalLength() {
        byte[] lengthOfByte = this._fileHeaderForReceive.fapp_totlen;
        long lengthOfLong = 0;
        for (int index = 3; index >= 0; index--) {
            byte data = lengthOfByte[index];
            lengthOfLong = lengthOfLong << 8;
            lengthOfLong += ((int) data & 0xFF);
        }
        return lengthOfLong;
    }

    @Override
    public synchronized boolean Receive(byte[] inputData) {
        long totalRealLength = this.changeByteToIntTotalLength();
        int totalLength = this.frameSize(totalRealLength);
        FileSimplestDlg upperLayer = ((FileSimplestDlg) this.GetUpperLayer(0));

        if (inputData[6] == (byte) 0x00 && inputData[5] == (byte) 0x00 && this._fileHeaderForReceive.fapp_msg_type == 0x00) {
            System.out.println("accept 1st frame");
            this.gotTheFirstFrame(inputData);
            return false;
        } else if (inputData[6] == (byte) 0x01 && inputData[5] == (byte) 0x01 && this.fileDataOfArray != null) {
            this.fileDataOfArray.add(inputData);
            int sequnceNumber = this.getByteArrayToSeqenceNumber(inputData);
            upperLayer.progressBar.setValue((int) (((sequnceNumber) / (double) totalLength) * 50));
            return false;
        } else if (inputData[6] == (byte) 0x01 && inputData[5] == (byte) 0x02 && this.fileDataOfArray != null) {
            System.out.println("accept last frame");
            this.fileDataOfArray.add(inputData);

            this.accepData();
            Collections.sort(this.fileData);

            System.out.println("fileData.size : " + this.fileData.size());
            System.out.println("totoalLength : " + totalLength);
            this.resetHeaderForReceive();
            System.out.println("file Receive End");
            upperLayer.progressBar.setValue(50);
            return this.fileWrite(this.fileData.size(), totalRealLength);
        }

        return false;
    }

    private void accepData() {
        this.fileData = new ArrayList<>();
        for (int index = 0; index < this.fileDataOfArray.size(); index++) {
            this.fileData.add(new FileData(this.getByteArrayToSeqenceNumber(this.fileDataOfArray.get(index)), this.fileDataOfArray.get(index)));
        }
    }

    public byte[] RemoveCappHeader(byte[] inputData, int length) {
        byte[] finalByteArray = new byte[length - 12];
        System.arraycopy(inputData, 12, finalByteArray, 0, length - 12);
        return finalByteArray;// 변경하세요 필요하시면
    }

    public void resetHeaderForReceive() {
        this._fileHeaderForReceive = new _FAPP_HEADER();
    }

    private void gotTheFirstFrame(byte[] inputData) {//파일 생성을 해주고 bar의 퍼센테이지를 올려주고, transper을 enabled를 false해준다
        //header 초기화 해 주기
        for (int index = 0; index < 4; index++) {
            this._fileHeaderForReceive.fapp_totlen[index] = inputData[index];
        }//전체 길이 저장
        this._fileHeaderForReceive.fapp_type[0] = inputData[4];
        this._fileHeaderForReceive.fapp_type[1] = inputData[5];
        this._fileHeaderForReceive.fapp_msg_type = (byte) 0x01;

        this.fileDataOfArray = new ArrayList<>();
        byte[] fileNameByteData = this.RemoveCappHeader(inputData, 1460);
        String fileName = (new String(fileNameByteData)).trim();
        this.storeFile = new File("./" + fileName);//파일 생성
    }

    private boolean fileWrite(int maxSeqenceNumber, long totalRealLength) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(this.storeFile, false)) {
            for (int indexOfSequceNumber = 0; indexOfSequceNumber < maxSeqenceNumber - 1; indexOfSequceNumber++) {
                int percent = (int) (((indexOfSequceNumber + 1) / (double) maxSeqenceNumber) * 49);
                int result = this.write(indexOfSequceNumber, fileOutputStream);
                if (result == 0) {
                    long number = totalRealLength / 1448;
                    number = totalRealLength % 1448 > 0 ? number + 1 : number;
                    System.out.println("fail to store data accept frame numbers : " + maxSeqenceNumber + " real all frame size : " + number);
                    return false;
                } else if (result == -1) {
                    System.out.println("duplication is happen");
                    indexOfSequceNumber--;
                }
                ((FileSimplestDlg) this.GetUpperLayer(0)).progressBar.setValue(50 + percent);
            }
            if (!this.writeForLast(maxSeqenceNumber - 1, fileOutputStream, totalRealLength)) {
                return false;
            }
            ((FileSimplestDlg) this.GetUpperLayer(0)).progressBar.setValue(100);
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("It finished store all data");
        return true;
    }

    private int write(int index, FileOutputStream fileOutputStream) throws IOException {
        FileData dataFileObj = this.fileData.get(index);
        if (index + 1 < dataFileObj.getSequnceNumber()) {
            System.out.println("fail : " + index);
            return 0;
        }
        System.out.println(dataFileObj.getSequnceNumber());
        byte[] data = this.RemoveCappHeader(dataFileObj.getData(), 1460);
        fileOutputStream.write(data);
        return 1;
    }

    private boolean writeForLast(int lastIndex, FileOutputStream fileOutputStream, long totalLength) throws IOException {
        FileData dataFileObj = this.fileData.get(lastIndex);
        if (lastIndex + 1 != dataFileObj.getSequnceNumber()) {
            return false;
        }
        byte[] data = this.RemoveCappHeader(dataFileObj.getData(), 1460);
        int lastFrameIndex = (int) (totalLength % 1448);
        if (lastFrameIndex == 0) {
            lastFrameIndex = 1448;
        }
        byte[] realEndSizeByte = new byte[lastFrameIndex];
        for (int index = 0; index < realEndSizeByte.length; index++) {
            realEndSizeByte[index] = data[index];
        }
        fileOutputStream.write(realEndSizeByte);
        return true;
    }
}

class FileData implements Comparable<FileData> {
    private int sequnceNumber;
    private byte[] data;

    public byte[] getData() {
        return this.data;
    }

    public FileData(int sequnceNumber, byte[] data) {
        this.sequnceNumber = sequnceNumber;
        this.data = data;
    }

    public int getSequnceNumber() {
        return this.sequnceNumber;
    }

    @Override
    public int compareTo(FileData o) {
        if (this.sequnceNumber < o.sequnceNumber) {
            return -1;
        } else if (this.sequnceNumber == o.sequnceNumber) {
            return 0;
        }
        return 1;
    }
}