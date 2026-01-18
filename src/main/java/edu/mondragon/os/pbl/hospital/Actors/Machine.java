package edu.mondragon.os.pbl.hospital.Actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.SimulationFilter.SimulationService;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Machine extends Thread {

    private int id;
    private Message reply;
    private BlockingQueue<HospitalMessage> hospital;
    private final BlockingQueue<Message> myMailbox;
    private long t0;
    private final BlockingQueue<WaitingRoomMessage> waitingmailbox;

    public Machine(int id, BlockingQueue<HospitalMessage> hospital,BlockingQueue<WaitingRoomMessage> waitingmailbox) {
        super("Machine " + id);
        this.hospital = hospital;
        this.myMailbox = new LinkedBlockingQueue<>();
        this.id = id;
        this.waitingmailbox=waitingmailbox;
    }

    private void log(String emoji, String phase, String msg) {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        SimulationService.postSimEvent("MACHINE", id, text, ms);
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, getName(), phase, msg);
    }

    @Override
    public void run() {

        t0 = System.currentTimeMillis();
        log("🛠️", "START", "Encendida y lista");

        while (!Thread.interrupted()) {
            try {
                beMachine(id);

                // Pequeña pausa antes de volver a ofrecerse como libre
                // log("😴", "REST", "Esperando siguiente paciente...");
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    public void beMachine(int machineId) throws InterruptedException {

        hospital.put(new HospitalMessage("FREE_MACHINE", "" + id, myMailbox));
        waitingmailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "Need patient", myMailbox));

        log("🟢", "AVAILABLE", "Máquina " + machineId + " libre");
        hospital.put(new HospitalMessage("WAITING_PATIENT", "" + id, myMailbox));
        System.out.println("Oye");
        reply = myMailbox.take();
        log("", "AVAILABLE", "Maquina" + machineId + " Esperando a que se prepare el paciente: " + reply.content);
        hospital.put(new HospitalMessage("PATIENT_IS_READY?", "" + id, myMailbox));
        reply = myMailbox.take();
        log("🎛️", "MAMMOGRAPHY", "Inicia mamografía al paciente " + reply.content);
        log("⏳", "MAMMOGRAPHY", "Realizando mamografía...");

        Thread.sleep(1300);// tiempo que tarda el paciente en hacer la mamografio
        hospital.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "" + id, myMailbox));
        hospital.put(new HospitalMessage("PATIENT_HAS_GO?", "" + id, myMailbox));// espera asta que paciente se
        reply = myMailbox.take();

        /*
         * // 1) Se anuncia como libre
         * log("🟢", "AVAILABLE", "Máquina " + machineId + " libre");
         * hospital.put(new HospitalMessage("FREE_MACHINE", "" + id, myMailbox));
         * reply = myMailbox.take(); // (por ejemplo: ack / señal interna)
         * log("📩", "ASSIGN", "Señal recibida (asignación en proceso)");
         * hospital.put(new HospitalMessage("WAITING_PATIENT", "" + id, myMailbox));
         * // 2) Espera asignación (según tu protocolo garantizado)
         * reply = myMailbox.take(); // aquí viene el paciente (reply.content)
         * String patientId = reply.content;
         * 
         * // 3) Mamografía
         * log("🎛️", "MAMMOGRAPHY", "Inicia mamografía al paciente " + patientId);
         * log("⏳", "MAMMOGRAPHY", "Realizando mamografía...");
         * Thread.sleep(1300);// tiempo que tarda el paciente en hacer la mamografio
         * 
         * // 4) Finaliza y notifica
         * log("✅", "DONE", "Mamografía completada para paciente " + patientId);
         * hospital.put(new HospitalMessage("COMPLETED_PATIENT", "" + id, myMailbox));
         * reply = myMailbox.take();
         */

    }
}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
