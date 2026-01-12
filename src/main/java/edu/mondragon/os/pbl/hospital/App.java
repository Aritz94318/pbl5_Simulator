package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

/**
 * Hospital
 *
 */
public class App {
    final static int N = 5;

    private Thread hospital;
    private Thread waitingRoom;
    private Thread diagnosticUnit;

    private Doctor doctors[];
    private Patient patients[];
    private Machine machines[];
    private Thread appoiment;

    public App(int NUMPATIENTS, int NUMDOCTORS, int NUMMACHINES) {
        BlockingQueue<AppointmentMessage> apServer = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hoServer = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waServer = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> duServer = new LinkedBlockingQueue<>();

        doctors = new Doctor[NUMDOCTORS];
        patients = new Patient[NUMPATIENTS];
        machines = new Machine[NUMMACHINES];

        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(i + 1, apServer, hoServer, waServer,duServer);
        }
        for (int i = 0; i < NUMDOCTORS; i++) {
            doctors[i] = new Doctor(i, duServer);
        }
        for (int i = 0; i < NUMMACHINES; i++) {
            machines[i] = new Machine(i, hoServer);
        }
        appoiment = new Thread(new Appointment(apServer));
        waitingRoom = new Thread(new WaitingRoom(waServer));
        hospital = new Thread(new Hospital(hoServer, waServer, NUMMACHINES));
        diagnosticUnit = new Thread(new DiagnosticUnit(duServer));

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
        diagnosticUnit.start();
    }

    public void waitEndOfThreads(int NUMPATIENTS,int NUMDOCTORS,int NUMMACHINES ) {
        try {
            for (int i = 0; i < NUMPATIENTS; i++) {
                patients[i].join();
            }
             for (int i = 0; i < NUMMACHINES; i++) {
                machines[i].interrupt();
            }
            for (int i = 0; i < NUMDOCTORS; i++) {
                doctors[i].interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        appoiment.interrupt();
        waitingRoom.interrupt();
        hospital.interrupt();
        diagnosticUnit.interrupt();
    }
      
    }

    /*
     * public static void main(String[] args) {
     * 
     * App app = new App();
     * 
     * app.startThreads();
     * app.waitEndOfThreads();
     * }
     */

