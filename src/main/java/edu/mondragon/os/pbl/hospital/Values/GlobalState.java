package edu.mondragon.os.pbl.hospital.values;

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

    public int getNumPatients() { return numPatients; }
    public int getNumDoctors() { return numDoctors; }
    public int getNumMachines() { return numMachines; }
}
