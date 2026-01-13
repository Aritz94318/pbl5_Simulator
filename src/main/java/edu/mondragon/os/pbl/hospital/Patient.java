package edu.mondragon.os.pbl.hospital;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Patient extends Thread {

    private BlockingQueue<AppointmentMessage> appoiment;
    private BlockingQueue<HospitalMessage> hospital;
    private BlockingQueue<WaitingRoomMessage> waitingroom;
    private BlockingQueue<DiagnosticUnitMessage> diagnosticUnit;

    private final BlockingQueue<Message> myMailbox;

    private Random rand;
    private int id;
    private String appoiment_id;
    private Message reply;
    private long t0;

    public Patient(int id, BlockingQueue<AppointmentMessage> appoiment, BlockingQueue<HospitalMessage> hospital,
            BlockingQueue<WaitingRoomMessage> waitingroom, BlockingQueue<DiagnosticUnitMessage> diagnosticUnit) {
        super("Patient" + id);
        this.id = id;
        this.appoiment = appoiment;
        this.hospital = hospital;
        this.waitingroom = waitingroom;
        this.diagnosticUnit = diagnosticUnit;
        this.myMailbox = new LinkedBlockingQueue<>();

        rand = new Random();
        // arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            // arrives to the hospital
            t0 = System.currentTimeMillis();

            log("ARRIVAL", "Llega al hospital");
            Thread.sleep(rand.nextInt(2000));

            log("APPOINTMENT", "Solicita cita");

            appoiment.put(new AppointmentMessage("REQUEST_APPOINTMENT", "" + id, myMailbox));

            // esperar respuesta
            reply = myMailbox.take();
            appoiment_id = reply.content;
            log("APPOINTMENT", "Recibe cita #" + appoiment_id);

            log("WAITING_ROOM", "Entra a sala de espera (turno " + appoiment_id + ")");
            waitingroom.put(new WaitingRoomMessage("WAIT", appoiment_id, myMailbox));
            reply = myMailbox.take();
            log("WAITING_ROOM", "Es su turno. Sale hacia triaje/mamografía");

            attendPatient(id);
            doctorsDiagnostics(id);

        } catch (InterruptedException e) {
        }
    }

    public void attendPatient(int patientId) throws InterruptedException // Este es el que esta unido al paciente
    {
        hospital.put(new HospitalMessage("WAITING", "" + id, myMailbox));

        reply = myMailbox.take();

        System.out.println("Patient: " + patientId + "  Go to MACHINE=" + reply.content);

        hospital.put(new HospitalMessage("IS_READY", "" + id, myMailbox));

        reply = myMailbox.take();

        System.out.println("Patient:" + patientId + "Is leaving Hospital");

        hospital.put(new HospitalMessage("PATIENT_GONE", "" + id, myMailbox));

    }

    public void doctorsDiagnostics(int id) throws InterruptedException {
        diagnosticUnit.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "" + id, myMailbox));
        boolean iaReceived = false;

        while (true) {
            Message m = myMailbox.take(); // espera el siguiente mensaje que llegue

            // 1) Resultado de IA (en tu código viene con type = "" y content =
            // "MALIGNO/VENIGNO")
            if (!iaReceived) {
                iaReceived = true;
                System.out.println("Ha sido analizado por la IA y ha pasado a manos de expertos. Paciente: " + id
                        + " | IA: " + m.content);
                continue;
            }

            // 2) Mensaje final (en tu código lo envías con type = "End")
            if ("End".equals(m.type)) {
                System.out.println("Diagnóstico FINAL listo. Coja cita con su doctor. Paciente: " + id
                        + " | Resultado: " + m.content);
                break;
            }

            // 3) Cualquier otro mensaje intermedio (por si en el futuro añades más)
            System.out.println("Actualización (" + m.type + "): " + m.content + " | Paciente: " + id);
        }
    }

    private void log(String phase, String msg) {
        long ms = (System.currentTimeMillis() - t0);
        System.out.printf("[%6dms] [%s] %-14s %s%n", ms, getName(), phase, msg);
    }

}
// Paciente pide cita
// Paciente espera a que llegue su turno
// Paciente se esta realizando una mamografia
// Paciente Acaba Mamografia
// Paciente se va del hospital
// Paciente espera al resultado
// Paciente acaba
