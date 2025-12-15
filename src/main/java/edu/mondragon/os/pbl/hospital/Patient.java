package edu.mondragon.os.pbl.hospital;

public class Patient extends Thread {

    private int arrivalTime = 0;
    private Hospital hospital;
    private Appoiment appoiment;
    private int appoiment_id;
    private int id;

    public Patient(int id, Hospital hospital,Appoiment appoiment) {
        super("Patient" + id);
        this.id=id;
        this.hospital = hospital;
        this.appoiment=appoiment;
        arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            appoiment_id=appoiment.getAppoiment(id);
            Thread.sleep(arrivalTime);
            hospital.enterHospital(getName());
        } catch (InterruptedException e) {
        }
    }
}
