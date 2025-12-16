package edu.mondragon.os.pbl.hospital;

import java.util.Random;

public class Patient extends Thread {

    // private int arrivalTime;
    private Hospital hospital;
    private Appoiment appoiment;
    private WaitingRoom waitingRoom;

    private Random rand;
    private int id;
    private int appoiment_id;

    public Patient(int id, Hospital hospital, Appoiment appoiment, WaitingRoom waitingRoom) {
        super("Patient" + id);
        this.id = id;
        this.hospital = hospital;
        this.appoiment = appoiment;
        this.waitingRoom=waitingRoom;
        rand = new Random();
        // arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            // arrives to the hospital
            System.out.println(getName() + " arrives to the hospital.");
            Thread.sleep(rand.nextInt(2000));
            appoiment_id = appoiment.getAppoiment(id);
            
            System.out.println(getName() + " has appointment #" + appoiment_id);

            // patient waits for her turn
            System.out.println(getName() + " waits in the waiting room.");
            waitingRoom.waitForTurn(appoiment_id);
            hospital.attendPatient(appoiment_id);
            //System.out.println(getName() + "has finished the mamogram.");

            // Thread.sleep(arrivalTime);
            // hospital.enterHospital(getName());
        } catch (InterruptedException e) {
        }
    }
}
// Paciente pide cita
// Paciente espera a que llegue su turno
// Paciente se esta realizando una mamografia
// Paciente Acaba Mamografia
// Paciente se va del hospital
// Paciente espera al resultado
// Paciente acaba
