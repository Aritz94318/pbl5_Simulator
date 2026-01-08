package edu.mondragon.os.pbl.hospital;


import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Hospital {

    private Lock mutex;
    private Condition machineRest;
    private Condition machineWaitPatient;
    private Condition patientWaiting;

    private boolean rest;
    private int[] machinePatient;
    private Random rand;
    private WaitingRoom waitingRoom;
    private int waiting;

    // private Diagnostic diagnotic;

    public Hospital(WaitingRoom waitingRoom) {
        mutex = new ReentrantLock();
        machineRest = mutex.newCondition();
        machineWaitPatient = mutex.newCondition();
        patientWaiting=mutex.newCondition();
        this.waitingRoom = waitingRoom;
        rest = true;
        rand = new Random();
        machinePatient = new int[3];
        waiting = 0;
    }


    public void attendPatient(int patientId) throws InterruptedException // Este es el que esta unido al paciente
    {
        int id;
        mutex.lock();
        try {
            waiting++;
            if (waiting > 0) {
                rest = false;
                machineRest.signalAll();
            }
            id = setPatient(patientId);
            System.out.println("Patient: " + patientId + "  Go to MACHINE=" + id);
            machineWaitPatient.signalAll();
            while (machinePatient[id] == patientId) {
                patientWaiting.await();
            }
            waiting--;
            if(waiting==0)
            {
                rest=true;
            }
            System.out.println("Patient:" + patientId + "Is leaving Hospital");
        } finally {
            mutex.unlock();
        }
    }

   


    public int setPatient(int patient) {
        int id;
        if (machinePatient[0] == 0) {
            id = 0;
        } else if (machinePatient[1] == 0) {
            id = 1;
        } else {
            id = 2;
        }
        machinePatient[id] = patient;
        return id;
    }

    public void deletePatient(int machineId) {
        machinePatient[machineId] = 0;
    }

}
