package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Doctor extends Thread {

    private int arrivalTime = 0;
    private BlockingQueue<DiagnosticUnitMessage> diagnosticUnit;
    private final BlockingQueue<Message> myMailbox;
    private int id;

    public Doctor(int id, BlockingQueue<DiagnosticUnitMessage> diagnosticUnit) {
        super("Doctor " + id);
        this.id = id;
        this.diagnosticUnit = diagnosticUnit;
        this.myMailbox = new LinkedBlockingQueue<>();
        arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {

                Thread.sleep(arrivalTime);
                diagnosticUnit.put(new DiagnosticUnitMessage("Get Diagnosis", "" + id, myMailbox));
                myMailbox.take();
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