
package com.nhnacademy.domain;
import java.io.Serializable;
import java.util.List;
public class ServerData implements Serializable{
    //리스트를 객체로 포장해 보내기 위한 클래스 직렬화.
    @SuppressWarnings("all")
    List list;
    @SuppressWarnings("all")
    public List getList() {
        return list;
    }
    @SuppressWarnings("all")
    public void setList(List list) {
        this.list = list;
    }

}