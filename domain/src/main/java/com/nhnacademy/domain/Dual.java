package com.nhnacademy.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dual {
    HashMap<String,Integer> infoList = new HashMap<>(); // 용사와 몬스터의 HP를 확인하기 위한 맵.
    List<String> savedMessageList = new ArrayList<>(); // 혹시 메세지 보낼일..
    public static void main(String[] args) throws InterruptedException {
        Dual dual = new Dual();
        dual.fightUserVsMonster();
    }
    Creatures warrior;
    Creatures monster;
    @SuppressWarnings("squid:1104")
    public String winner;


    public Dual() {
    }
    public Dual(Creatures warrior, Creatures monster) {
        this.warrior = warrior;
        this.monster = monster;
        this.warrior.setDual(this);
        this.monster.setDual(this);
    }

    //쓰레드에서 나 말고 다른 상대 이름을 찾기 위한 메서드 ( 어차피 2명이니 나 아니면 정답. )
    String findOtherName(String name){
        for (Map.Entry<String, Integer> entry : infoList.entrySet()) {
            String key   = entry.getKey();
            if (!name.equals(key)) return key;
        }
        return null;
    }

    //공격 메서드 두 개의 쓰레드의 무한반복을 동시에 멈추기 위해 boolean타입으로 반환.
    boolean attack(int dmg , Creatures obj) {
        for (Integer value : infoList.values()) {
            if (value<=0){
                return true;
            }
        }
        String enemyName = findOtherName(obj.getName());
        infoList.put(enemyName,infoList.get(enemyName)-dmg);
        savedMessageList.add(obj.getName()+"님이 "+enemyName+"에게 "+dmg+"만큼의 피해를 주었습니다. "+enemyName+"의 현재 체력 : " + infoList.get(enemyName));
        return false;
    }


    // 누가 이겼는지 판단하는 메서드.
    @SuppressWarnings("squid:108")
    public String whoIsWin(){
        for (Map.Entry<String, Integer> entry : infoList.entrySet()) {
            String key   = entry.getKey();
            int value =  entry.getValue();
            if(value>0){
                warrior.setHealthPoint(value);
                return key;
            }
        }
        return null;
    }

    // 진짜 싸우는 메서드.
    @SuppressWarnings("all")
    public List fightUserVsMonster() throws InterruptedException {
        List list;
        Thread tWarrior = new Thread(this.warrior);
        Thread tMonster = new Thread(this.monster);
        infoList.put(this.warrior.getName(), this.warrior.getHealthPoint());
        infoList.put(this.monster.getName(), this.monster.getHealthPoint());
        tWarrior.start();
        tMonster.start();
        tWarrior.join();
        tMonster.join();
        winner = whoIsWin();
        savedMessageList.add(winner+"이(가) 승리하였습니다!");
        list = savedMessageList;
        return list;
    }


}
