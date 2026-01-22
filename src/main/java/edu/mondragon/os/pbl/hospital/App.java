package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.actors.Doctor;
import edu.mondragon.os.pbl.hospital.actors.Machine;
import edu.mondragon.os.pbl.hospital.actors.Patient;
import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.room.Appointment;
import edu.mondragon.os.pbl.hospital.room.DiagnosticUnit;
import edu.mondragon.os.pbl.hospital.room.Hospital;
import edu.mondragon.os.pbl.hospital.room.WaitingRoom;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

/**
 * Hospital
 *
 */
public class App {
    final static int N = 5;
    private long start;

    private long end;
    private Thread hospital;
    private Thread waitingRoom;
    private Thread diagnosticUnit;
    private SimulationService service;
    private Doctor doctors[];
    private Patient patients[];
    private Machine machines[];
    private Thread appoiment;

    public App(int numPatients, int numDoctors, int numMachines) {
        BlockingQueue<AppointmentMessage> apServer = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hoServer = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waServer = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> duServer = new LinkedBlockingQueue<>();
        end=0;
        doctors = new Doctor[numDoctors];
        patients = new Patient[numPatients];
        machines = new Machine[numMachines];
        service=new SimulationService();

        for (int i = 0; i < numPatients; i++) {
            patients[i] = new Patient(i + 1, apServer, hoServer, waServer, duServer, service);
        }
        for (int i = 0; i < numDoctors; i++) {
            doctors[i] = new Doctor(i+1, duServer, service);
        }
        for (int i = 0; i < numMachines; i++) {
            machines[i] = new Machine(i+1, hoServer,waServer, service);
        }
        appoiment = new Thread(new Appointment(apServer));
        waitingRoom = new Thread(new WaitingRoom(waServer));
        hospital = new Thread(new Hospital(hoServer, numMachines));
        diagnosticUnit = new Thread(new DiagnosticUnit(duServer));

    }

    public void startThreads() {

        start = System.nanoTime();

        for (Patient patient : patients) {
            patient.start();
        }

        for (Doctor doctor : doctors) {
            doctor.start();
        }

        for (Machine machine : machines) {
            machine.start();
        }
        appoiment.start();
        waitingRoom.start();
        hospital.start();
        diagnosticUnit.start();
    }

    public void waitEndOfThreads(int numPatients, int numDoctors, int numMachines) {
        try {
            for (int i = 0; i < numPatients; i++) {
                patients[i].join();
            }
            for (int i = 0; i < numMachines; i++) {
                machines[i].interrupt();
            }
            for (int i = 0; i < numDoctors; i++) {
                doctors[i].interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        appoiment.interrupt();
        waitingRoom.interrupt();
        hospital.interrupt();
        diagnosticUnit.interrupt();
        end = System.nanoTime();
        double seconds = (end - start) / 1_000_000_000.0;
        System.out.println(seconds);
    }

}

