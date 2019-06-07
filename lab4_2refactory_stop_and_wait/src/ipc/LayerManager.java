package ipc;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class LayerManager {//Layer를 모두 관리 해줌

    private class _NODE {//String으로 data 저장
        private String token;
        private _NODE next;

        public _NODE(String input) {
            this.token = input;
            this.next = null;
        }
    }

    _NODE mp_sListHead;//head 지정
    _NODE mp_sListTail;//tail 지정

    private int m_nTop;
    private int m_nLayerCount;

    private ArrayList<BaseLayer> mp_Stack = new ArrayList<>();//thread를 담는 stack
    private ArrayList<BaseLayer> mp_aLayers = new ArrayList<>();


    public LayerManager() {//default 생성자
        m_nLayerCount = 0;//layer가 0개
        mp_sListHead = null;//head가 없으므로
        mp_sListTail = null;//tail도 없으므로
        m_nTop = -1;//top은 없음 -> 0번 째 index 가 존재 할 수 있으므로
    }

    public void AddLayer(BaseLayer pLayer) {//Layer 추가
        mp_aLayers.add(m_nLayerCount++, pLayer);//ArrayList에 추가
        //m_nLayerCount++;
    }


    public BaseLayer GetLayer(int nindex) {
        return mp_aLayers.get(nindex);
    }//해당 위치의 layer를 return 받는다

    public BaseLayer GetLayer(String pName) {
        for (int i = 0; i < m_nLayerCount; i++) {
            if (pName.compareTo(mp_aLayers.get(i).GetLayerName()) == 0)
                return mp_aLayers.get(i);
        }
        return null;
    }//해당 이름을 가지는 Layer를 넣는다

    public void ConnectLayers(String pcList) {
        MakeList(pcList);//string으로 Node list형성
        LinkLayer(mp_sListHead);//만든 list의 head를 넣기
    }

    private void MakeList(String pcList) {
        StringTokenizer tokens = new StringTokenizer(pcList, " ");

        for (; tokens.hasMoreElements(); ) {
            _NODE pNode = AllocNode(tokens.nextToken());
            AddNode(pNode);
        }
    }//pcList의 str를 모두 node로 만들고 LinkedList에 추가 해 준다.

    private _NODE AllocNode(String pcName) {
        _NODE node = new _NODE(pcName);
        return node;
    }//노드 생성 메소드

    private void AddNode(_NODE pNode) {//Node추가 -> head와 tail이 지정됨
        if (mp_sListHead == null) {
            mp_sListHead = mp_sListTail = pNode;
        } else {
            mp_sListTail.next = pNode;
            mp_sListTail = pNode;
        }
    }

    private void Push(BaseLayer pLayer) {//stack에 추가
        mp_Stack.add(++m_nTop, pLayer);
        //mp_Stack.add(pLayer);
        //m_nTop++;
    }

    private BaseLayer Pop() {//stack에서 제거
        BaseLayer pLayer = mp_Stack.get(m_nTop);
        mp_Stack.remove(m_nTop);
        m_nTop--;//해당 top에 위치한 것을 제거 하기위해 top은 index를 가짐

        return pLayer;
    }

    private BaseLayer Top() {//layer의 제일 위에 위치를 return
        return mp_Stack.get(m_nTop);
    }

    private void LinkLayer(_NODE pNode) {//head로 시작
        BaseLayer pLayer = null;

        while (pNode != null) {//tail까지 가도록 반복
            if (pLayer == null)
                pLayer = GetLayer(pNode.token);//pLayer에 pNode의 string값과 동일한 값의 layer를 return 해줌
            else {
                if (pNode.token.equals("("))
                    Push(pLayer);//layer 추가
                else if (pNode.token.equals(")"))
                    Pop();//layer 제거
                else {
                    char cMode = pNode.token.charAt(0);//0번 쨰 char
                    String pcName = pNode.token.substring(1, pNode.token.length());//0번째를 재외한 모든 String

                    pLayer = GetLayer(pcName);

                    switch (cMode) {
                        case '*':
                            Top().SetUpperUnderLayer(pLayer);//layer를 top에 넣기, 현재 class layer를 under layer에 넣기
                            break;
                        case '+':
                            Top().SetUpperLayer(pLayer);//upper layer에 입력
                            break;
                        case '-':
                            Top().SetUnderLayer(pLayer);//under layer로 지정
                            break;
                    }
                }
            }
            pNode = pNode.next;//next로 이동
        }
    }
}
