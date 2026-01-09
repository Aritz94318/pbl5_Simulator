package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

/**
 * Hospital
 *
 */
public class App {
    final static int N = 5;
    final static int NUMPATIENTS = 6;
    final static int NUMDOCTORS = 5;
    final static int NUMMACHINES = 3;

    private Thread hospital;
    private Thread waitingRoom;
    private Doctor doctors[];
    private Patient patients[];
    private Machine machines[];
    private Thread appoiment;

    public App() {
        BlockingQueue<AppointmentMessage> apServer = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hoServer = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waServer = new LinkedBlockingQueue<>();


        doctors = new Doctor[NUMDOCTORS];
        patients = new Patient[NUMPATIENTS];
        machines = new Machine[NUMMACHINES];

        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(i + 1, apServer,hoServer,waServer);
        }
        for (int i = 0; i < NUMDOCTORS; i++) {
            doctors[i] = new Doctor(i, hoServer);
        }
        for (int i = 0; i < NUMMACHINES; i++) {
            machines[i] = new Machine(i, hoServer,waServer);
        }
        appoiment = new Thread(new Appointment(apServer));
        waitingRoom = new Thread(new WaitingRoom(waServer));
        hospital = new Thread(new Hospital(hoServer,waServer));
    }

    public void startThreads() {

        for (Patient patient : patients) {
            patient.start();
        }
        /*
         * for (Doctor doctor : doctors) {
         * doctor.start();
         * }
         */
        for (Machine machine : machines) {
            machine.start();
        }
        appoiment.start();
        waitingRoom.start();
        hospital.start();

    }

    public void waitEndOfThreads() {
        try {
            for (int i = 0; i < NUMPATIENTS; i++) {
                patients[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        App app = new App();

        app.startThreads();
        app.waitEndOfThreads();
    }
}
