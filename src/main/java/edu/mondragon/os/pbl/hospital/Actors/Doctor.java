package edu.mondragon.os.pbl.hospital.Actors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.SimulationFilter.SimulationService;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Doctor extends Thread {

    private int arrivalTime = 0;
    private BlockingQueue<DiagnosticUnitMessage> diagnosticUnit;
    private final BlockingQueue<Message> myMailbox;
    private int id;
    private long t0;

    public Doctor(int id, BlockingQueue<DiagnosticUnitMessage> diagnosticUnit) {
        super("Doctor " + id);
        this.id = id;
        this.diagnosticUnit = diagnosticUnit;
        this.myMailbox = new LinkedBlockingQueue<>();
        arrivalTime += 500 * id;
    }

    private void log(String emoji, String phase, String msg) {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        SimulationService.postSimEvent("DOCTOR", id, text, ms);
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, getName(), phase, msg);
    }

    @Override
    public void run() {
        t0 = System.currentTimeMillis();
        try {
            log("🩺", "START", "Listo para trabajar");

            while (!Thread.interrupted()) {

                // Simula tiempo hasta que “entra en turno”
                // 💤 Descanso / tiempo muerto del doctor antes de pedir trabajo
                log("😴", "REST", "Descansando...");
                Thread.sleep((long) (Math.random() * 700));
                // 0.8 – 1.5 s → tiempo natural entre tareas

                // 1️⃣ Pide un caso/diagnóstico para revisar
                log("📥", "REQUEST", "Pide un caso para revisar");
                diagnosticUnit.put(new DiagnosticUnitMessage("Get Diagnosis", "" + id, myMailbox));

                // ⏳ Espera administrativa / asignación de caso
                Thread.sleep((long) (Math.random() * 400));
                // 0.3 – 0.7 s → cola / asignación interna

                // 2️⃣ Espera a que le asignen el caso
                Message m1 = myMailbox.take();
                log("🔔", "ASSIGNED", "Caso recibido: " + (m1.content != null ? m1.content : "(sin detalle)"));

                // 🧠 Revisión médica real
                Thread.sleep(1200 + (long) (Math.random() * 1000));
                // 1.2 – 2.2 s → análisis del diagnóstico

                // 3️⃣ Lanza la fase final
                log("👨‍⚕️", "REVIEW", "Enviando diagnóstico final");
                diagnosticUnit.put(new DiagnosticUnitMessage("FINAL DIAGNOSIS", "" + id, myMailbox));

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