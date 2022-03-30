package com.nhnacademy.domain;

import static java.lang.Thread.sleep;

import java.util.Random;

public class Creatures implements Runnable{
    private final String name;
    private int healthPoint;
    private final int damagePoint;
    private final boolean specialCreature;
    private Dual dual;

    public void setDual(Dual dual) {
        this.dual = dual;
    }

    public String getName() {
        return name;
    }

    public int getHealthPoint() {
        return healthPoint;
    }


    public Creatures(String name, int healthPoint, int damagePoint, boolean specialCreature) {
        this.name = name;
        this.healthPoint = healthPoint;
        this.damagePoint = damagePoint;
        this.specialCreature = specialCreature;
    }

    public void setHealthPoint(int healthPoint) {
        this.healthPoint = healthPoint;
    }
    @SuppressWarnings("squid:2119")
    public int attack() {
        Random random = new Random();
        int luckyAttackNum = random.nextInt(10);
        if (specialCreature && luckyAttackNum == 7) {
            return 15;
        } else {
            return random.nextInt(this.damagePoint)+1;
        }
    }



    @Override
    @SuppressWarnings("squid:2142")
    public void run() {
        boolean flag=false;
        synchronized (this){
            while (!flag){
                flag = dual.attack(attack(),this);
                try {
                    sleep(500);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}





