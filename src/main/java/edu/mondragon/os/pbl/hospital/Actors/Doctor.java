package edu.mondragon.os.pbl.hospital.actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

public class Doctor extends Thread {

    private BlockingQueue<DiagnosticUnitMessage> diagnosticUnit;
    private final BlockingQueue<Message> myMailbox;
    private int id;
    private long t0;
    private SimulationService service;

    public Doctor(int id, BlockingQueue<DiagnosticUnitMessage> diagnosticUnit, SimulationService service) {
        super("Doctor " + id);
        this.id = id;
        this.diagnosticUnit = diagnosticUnit;
        this.myMailbox = new LinkedBlockingQueue<>();
        this.service = service;

    }

    private void log(String emoji, String phase, String msg) throws InterruptedException {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        service.postList("DOCTOR", id, text, ms);
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, getName(), phase, msg);
    }

    @Override
    public void run() {
        t0 = System.currentTimeMillis();
        try {
            log("🩺", "START", "Ready for work");

            while (!Thread.interrupted()) {

                // Simulates time until it “enters its turn”
                // 💤 Doctor rest / idle time before requesting work
                log("😴", "REST", "Resting...");
                Thread.sleep((long) (Math.random() * 7000));
                // 0.8 – 1.5 s → natural time between tasks

                // 1️⃣ Requests a case/diagnosis to review
                log("📥", "REQUEST", "Requests a case to review");
                // ⏳ Administrative wait / case assignment
                diagnosticUnit.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "" + id, myMailbox));

                // 0.3 – 0.7 s → queue / internal assignment

                // 2️⃣ Waits for the case to be assigned
                Message m1 = myMailbox.take();
                log("🔔", "ASSIGNED", "Case received: " + (m1.content != null ? m1.content : "(no details)"));

                // 🧠 Actual medical review
                Thread.sleep(1200 + (long) (Math.random() * 10000));
                // 1.2 – 2.2 s → diagnosis analysis

                // 3️⃣ Launches the final phase
                log("👨‍⚕️", "REVIEW", "Sending final diagnosis");
                Thread.sleep((long) (Math.random() * 4000));

                diagnosticUnit.put(new DiagnosticUnitMessage("FINAL_DIAGNOSIS", "" + id, myMailbox));

            }
        } catch (InterruptedException e) {
        }
    }
}
// Duerme
// Diagnosis en espera(x)--> Doctor despierta
// Escoge diagnosis por prioridad->Se comunica a paciente [estado de mamografía:
// siendo analizado]
// Analiza la mamografia
// Paciente recive aviso estado de mamografía: analizado