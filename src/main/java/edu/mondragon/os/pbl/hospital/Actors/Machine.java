package edu.mondragon.os.pbl.hospital.actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

public class Machine extends Thread {

    private int id;
    private Message reply;
    private BlockingQueue<HospitalMessage> hospital;
    private final BlockingQueue<Message> myMailbox;
    private long t0;
    private final BlockingQueue<WaitingRoomMessage> waitingmailbox;
    SimulationService service;

    public Machine(int id, BlockingQueue<HospitalMessage> hospital, BlockingQueue<WaitingRoomMessage> waitingmailbox,
            SimulationService service) {
        super("Machine " + id);
        this.hospital = hospital;
        this.myMailbox = new LinkedBlockingQueue<>();
        this.id = id;
        this.waitingmailbox = waitingmailbox;
        this.reply = new Message("", "", null);
        this.service = service;
    }

    private void log(String emoji, String phase, String msg) throws InterruptedException {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        service.postList("MACHINE", id, text, ms);
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, getName(), phase, msg);
    }

    @Override
    public void run() {

        t0 = System.currentTimeMillis();

        while (!Thread.interrupted()) {
            try {
                beMachine(id);

                // Pequeña pausa antes de volver a ofrecerse como libre

            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    public void beMachine(int machineId) throws InterruptedException {
        log("🟢", "MACHINE", "Machine is free");
        hospital.put(new HospitalMessage("FREE_MACHINE", "" + id, myMailbox));
        waitingmailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "Need patient", myMailbox));

        hospital.put(new HospitalMessage("WAITING_PATIENT", "" + id, myMailbox));
        reply = myMailbox.take();
        log("", "AVAILABLE", "Machine " + machineId + " Waiting for the patient to get ready: " + reply.content);
        hospital.put(new HospitalMessage("PATIENT_IS_READY?", "" + id, myMailbox));
        reply = myMailbox.take();
        log("🎛️", "MAMMOGRAPHY", "Starting mammography on patient " + reply.content);
        Thread.sleep(150);
        log("⏳", "MAMMOGRAPHY", "Finis mammography...");

        Thread.sleep(100); // time it takes the patient to undergo the mammography
        hospital.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "" + id, myMailbox));
        log("⏳", "MAMMOGRAPHY", "Waiting fot the patient to leave");

        hospital.put(new HospitalMessage("PATIENT_HAS_GO?", "" + id, myMailbox)); // waits until the patient leaves
        reply = myMailbox.take();

    }
}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
