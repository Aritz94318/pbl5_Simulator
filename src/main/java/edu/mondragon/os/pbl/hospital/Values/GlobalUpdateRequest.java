package edu.mondragon.os.pbl.hospital.Values;


public class GlobalUpdateRequest {
    private int numPatients;
    private int numDoctors;
    private int numMachines;

    public int getNumPatients() {
        return numPatients;
    }

    public void setNumPatients(int numPatients) {
        this.numPatients = numPatients;
    }

    public int getNumDoctors() {
        return numDoctors;
    }

    public void setNumDoctors(int numDoctors) {
        this.numDoctors = numDoctors;
    }

    public int getNumMachines() {
        return numMachines;
    }

    public void setNumMachines(int numMachines) {
        this.numMachines = numMachines;
    }
}
