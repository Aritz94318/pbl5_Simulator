package edu.mondragon.os.pbl.hospital.Values;

import org.springframework.stereotype.Component;

@Component
public class GlobalState {

    private int numPatients;
    private int numDoctors;
    private int numMachines;

    public synchronized void update(int numPatients, int numDoctors, int numMachines) {
        this.numPatients = numPatients;
        this.numDoctors = numDoctors;
        this.numMachines = numMachines;
    }

    public int getValue1() { return numPatients; }
    public int getValue2() { return numDoctors; }
    public int getValue3() { return numMachines; }
}
