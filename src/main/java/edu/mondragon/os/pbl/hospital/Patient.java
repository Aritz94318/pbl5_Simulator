package edu.mondragon.os.pbl.hospital;

import java.lang.reflect.Member;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Patient extends Thread {

    private BlockingQueue<AppointmentMessage> appoiment;
    private BlockingQueue<HospitalMessage> hospital;
    private BlockingQueue<WaitingRoomMessage> waitingroom;

    private final BlockingQueue<Message> myMailbox;

    private Random rand;
    private int id;
    private String appoiment_id;
    private Message reply;

    public Patient(int id, BlockingQueue<AppointmentMessage> appoiment, BlockingQueue<HospitalMessage> hospital,
            BlockingQueue<WaitingRoomMessage> waitingroom) {
        super("Patient" + id);
        this.id = id;
        this.appoiment = appoiment;
        this.hospital = hospital;
        this.waitingroom = waitingroom;
        this.myMailbox = new LinkedBlockingQueue<>();

        rand = new Random();
        // arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            // arrives to the hospital
            System.out.println(getName() + " arrives to the hospital.");
            Thread.sleep(rand.nextInt(2000));

            appoiment.put(new AppointmentMessage("REQUEST_APPOINTMENT", "" + id, myMailbox));

            // esperar respuesta
            reply = myMailbox.take();
            appoiment_id = reply.content;
            System.out.println(getName() + " has appointment #" + appoiment_id);

            // patient waits for her turn
            System.out.println(getName() + " waits in the waiting room.");
            waitingroom.put(new WaitingRoomMessage("WAIT", appoiment_id, myMailbox));
            reply = myMailbox.take();
            attendPatient(id);

        } catch (InterruptedException e) {
        }
    }

    public void attendPatient(int patientId) throws InterruptedException // Este es el que esta unido al paciente
    {
        hospital.put(new HospitalMessage("WAITING", "" + id, myMailbox));

        System.out.println("Patient: " + patientId + "  Go to MACHINE=" + reply.content);

        hospital.put(new HospitalMessage("IS_READY", "" + id, myMailbox));

        reply = myMailbox.take();

        System.out.println("Patient:" + patientId + "Is leaving Hospital");

        hospital.put(new HospitalMessage("PATIENT_GONE", "" + id, myMailbox));

    }
}
// Paciente pide cita
// Paciente espera a que llegue su turno
// Paciente se esta realizando una mamografia
// Paciente Acaba Mamografia
// Paciente se va del hospital
// Paciente espera al resultado
// Paciente acaba
