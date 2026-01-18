package edu.mondragon.os.pbl.hospital.Actors;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.SimulationFilter.SimulationService;
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

            log("🚶‍♂️", "ARRIVAL", "Llega al hospital");
            Thread.sleep(rand.nextInt(800));// Tiempo de llegada y solicitar cita

            log("📅", "APPOINTMENT", "Solicita cita");
            appoiment.put(new AppointmentMessage("REQUEST_APPOINTMENT", "" + id, myMailbox));

            reply = myMailbox.take();
            appoiment_id = reply.content;
            log("✅", "APPOINTMENT", "Recibe cita #" + appoiment_id);

            log("🪑", "WAITING_ROOM", "Entra en sala de espera (turno " + appoiment_id + ")");
            waitingroom.put(new WaitingRoomMessage("WAIT", appoiment_id, myMailbox));

            reply = myMailbox.take();
            log("🔔", "WAITING_ROOM", "Es su turno, pasa a mamografía");

            attendPatient(id);
            doctorsDiagnostics(id);

        } catch (InterruptedException e) {
        }
    }

    public void attendPatient(int patientId) throws InterruptedException // Este es el que esta unido al paciente
    {
        log("🏥", "HOSPITAL", "Solicita máquina de mamografía");
        hospital.put(new HospitalMessage("ANY_FREE_MACHINE", "" + id, myMailbox));// teoricamente solo llega asta aqui
        reply = myMailbox.take(); // si una maquina esta libre
        hospital.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "" + id, myMailbox));// Cuando envia esto a
                                                                                         // hospitalentra en un sleep
                                                                                         // que simula el tiempo

        reply = myMailbox.take();
        hospital.put(new HospitalMessage("PREPARING_FOR_LEAVING", "" + id, myMailbox));// Cuando envia esto a
        reply = myMailbox.take();

        // el tiempo
        /*
         * hospital.put(new HospitalMessage("WAITING", "" + id, myMailbox));
         * reply = myMailbox.take();
         * log("🩻", "MAMMOGRAPHY", "Asignada MÁQUINA " + reply.content);
         * 
         * hospital.put(new HospitalMessage("IS_READY", "" + id, myMailbox));
         * log("⏳", "MAMMOGRAPHY", "Realizando mamografía");
         * reply = myMailbox.take();
         * log("✅", "MAMMOGRAPHY", "Mamografía finalizada");
         * hospital.put(new HospitalMessage("PATIENT_GONE", "" + id, myMailbox));
         * reply=myMailbox.take();
         * log("🚪", "EXIT", "Sale del hospital");
         */

    }

    public void doctorsDiagnostics(int id) throws InterruptedException {

        log("🤖", "DIAG_AI", "Mamografía enviada a la IA");
        diagnosticUnit.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "" + id, myMailbox));

        // 1) ACK / “recibido” (si lo mandas)
        reply = myMailbox.take();
        log("📩", "DIAG_AI", "La IA ha recibido la mamografía Diagnostico:" + reply.content);

        // 2) Resultado IA
        reply = myMailbox.take();
        log("🧠", "DIAG_AI", "Resultado IA: " + reply.content + " → pasa a expertos");

        // 3) Diagnóstico final
        reply = myMailbox.take();
        log("👨‍⚕️", "DIAG_FINAL", "Diagnóstico FINAL: " + reply.content + " (pedir cita)");
    }

    private void log(String emoji, String phase, String msg) {
        long ms = System.currentTimeMillis() - t0;
        String text = emoji + " [" + phase + "]" + msg;
        SimulationService.postSimEvent("PATIENT", id, text, ms);
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
