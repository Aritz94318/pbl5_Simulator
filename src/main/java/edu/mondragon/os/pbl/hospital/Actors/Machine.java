package edu.mondragon.os.pbl.hospital.Actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.SimulationFilter.SimulationService;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Machine extends Thread {

    private int id;
    private Message reply;
    private BlockingQueue<HospitalMessage> hospital;
    private final BlockingQueue<Message> myMailbox;
    private long t0;

    public Machine(int id, BlockingQueue<HospitalMessage> hospital) {
        super("Machine " + id);
        this.hospital = hospital;
        this.myMailbox = new LinkedBlockingQueue<>();
        this.id = id;
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
                log("😴", "REST", "Esperando siguiente paciente...");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    public void beMachine(int machineId) throws InterruptedException {

        // 1) Se anuncia como libre
        log("🟢", "AVAILABLE", "Máquina " + machineId + " libre");
        hospital.put(new HospitalMessage("FREE_MACHINE", "" + id, myMailbox));

        // 2) Espera asignación (según tu protocolo garantizado)
        reply = myMailbox.take(); // (por ejemplo: ack / señal interna)
        log("📩", "ASSIGN", "Señal recibida (asignación en proceso)");

        reply = myMailbox.take(); // aquí viene el paciente (reply.content)
        String patientId = reply.content;

        // 3) Mamografía
        log("🎛️", "MAMMOGRAPHY", "Inicia mamografía al paciente " + patientId);
        log("⏳", "MAMMOGRAPHY", "Realizando mamografía...");
        Thread.sleep(150);

        // 4) Finaliza y notifica
        log("✅", "DONE", "Mamografía completada para paciente " + patientId);
        hospital.put(new HospitalMessage("COMPLETED_PATIENT", "" + id, myMailbox));
        // reply = myMailbox.take();

    }
}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
