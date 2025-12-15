package edu.mondragon.os.pbl.hospital;

public class Machine extends Thread {

    private int arrivalTime = 0;
    private Hospital hospital;

    public Machine(int id, Hospital hospital) {
        super("Machine " + id);
        this.hospital = hospital;
        arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(arrivalTime);
            hospital.enterHospital(getName());
        } catch (InterruptedException e) {
        }
    }
}
