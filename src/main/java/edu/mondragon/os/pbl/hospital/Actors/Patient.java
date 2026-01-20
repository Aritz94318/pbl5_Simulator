package edu.mondragon.os.pbl.hospital.actors;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

public class Patient extends Thread {

    private BlockingQueue<AppointmentMessage> appoiment;
    private BlockingQueue<HospitalMessage> hospital;
    private BlockingQueue<WaitingRoomMessage> waitingroom;
    private BlockingQueue<DiagnosticUnitMessage> diagnosticUnit;

    private final BlockingQueue<Message> myMailbox;

    private Random rand;
    private int id;
    private String appoimentId;
    private Message reply;
    private long t0;
    private SimulationService service;

    public Patient(int id, BlockingQueue<AppointmentMessage> appoiment, BlockingQueue<HospitalMessage> hospital,
            BlockingQueue<WaitingRoomMessage> waitingroom, BlockingQueue<DiagnosticUnitMessage> diagnosticUnit,
            SimulationService service) {
        super("Patient" + id);
        this.id = id;
        this.appoiment = appoiment;
        this.hospital = hospital;
        this.waitingroom = waitingroom;
        this.diagnosticUnit = diagnosticUnit;
        this.myMailbox = new LinkedBlockingQueue<>();
        appoimentId = "";
        this.service = service;
        rand = new Random();
    }

    @Override
    public void run() {
        try {
            // arrives to the hospital
            t0 = System.currentTimeMillis();

            log("🚶‍♂️", "ARRIVAL", "Arrives at the hospital");
            Thread.sleep(rand.nextInt(800)); // Arrival time and requesting an appointment

            log("📅", "APPOINTMENT", "Requests an appointment");
            appoiment.put(new AppointmentMessage("REQUEST_APPOINTMENT", "" + id, myMailbox));

            reply = myMailbox.take();
            appoimentId = reply.content;
            log("✅", "APPOINTMENT", "Receives appointment #" + appoimentId);

            log("🧍", "WAIT", "Patient arrives with ticket #" + appoimentId);
            waitingroom.put(new WaitingRoomMessage("WAIT", appoimentId, myMailbox));

            reply = myMailbox.take();
            log("🔔", "WAITING_ROOM", "It's their turn, they go to mammography");

            attendPatient();
            doctorsDiagnostics(id);

        } catch (InterruptedException e) {
        }
    }

    public void attendPatient() throws InterruptedException // Este es el que esta unido al paciente
    {
        int assignedMachine = -1;

        log("🏥", "HOSPITAL", "Requests a mammography machine");
        hospital.put(new HospitalMessage("ANY_FREE_MACHINE", "" + id, myMailbox));// teoricamente solo llega asta aqui

        reply = myMailbox.take(); // si una maquina esta libre
        assignedMachine = Integer.parseInt(reply.content);

        log("✅", "HOSPITAL", "Machine assigned: #" + assignedMachine + " (msg=" + reply.type + ")");

        log("🧍‍♂️", "MAMMO_PREP", "Getting ready for mammography in machine #" + assignedMachine);
        Thread.sleep(100); // 100 ms
        hospital.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "" + id, myMailbox));// Cuando envia esto a
                                                                                         // hospitalentra en un sleep
                                                                                         // que simula el tiempo

        log("🩻", "MAMMO", "Mammography in progress on machine #" + assignedMachine + "...");
        hospital.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "" + id, myMailbox));// Cuando envia esto a
        reply = myMailbox.take();
        log("🧍‍♂️", "MAMMO", "Mammography has finis preparing to leave");

        hospital.put(new HospitalMessage("PREPARING_FOR_LEAVING", "" + id, myMailbox));// Cuando envia esto a
        log("🏁", "LEAVING",
                "Permission granted to leave hospital (msg=" + reply.type + ", machine=" + reply.content + ")");

    }

    public void doctorsDiagnostics(int id) throws InterruptedException {

        log("🤖", "DIAG_AI", "Mammography sent to AI");
        diagnosticUnit.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "" + id, myMailbox));

        reply = myMailbox.take();
        log("📩", "DIAG_AI", "AI has received the mammography. Diagnosis: " + reply.content);

        reply = myMailbox.take();
        log("👨‍⚕️", "DIAG_FINAL", "FINAL diagnosis: " + reply.content + " (schedule appointment)");

    }

    private void log(String emoji, String phase, String msg) throws InterruptedException {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        service.postList("PATIENT", id, text, ms);
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, getName(), phase, msg);
    }

}
// Paciente pide cita
// Paciente espera a que llegue su turno
// Paciente se esta realizando una mamografia
// Paciente Acaba Mamografia
// Paciente se va del hospital
// Paciente espera al resultado
// Paciente acaba
