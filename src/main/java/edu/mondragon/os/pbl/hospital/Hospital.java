package edu.mondragon.os.pbl.hospital;

import java.security.PublicKey;
import java.util.concurrent.Semaphore;

public class Hospital {

    private int capacity;
    private int numCustomers;
    // TODO: Check these semaphores.
    private Semaphore mutex;
    private Semaphore customer;
    private Semaphore barber;
    private Semaphore customerGone;
    private Semaphore haircutDone;

    public Hospital(int capacity) {
    }

    public void enterHospital(String name) throws InterruptedException {

    }

    public void attendPatient() throws InterruptedException {

    }
    

}
