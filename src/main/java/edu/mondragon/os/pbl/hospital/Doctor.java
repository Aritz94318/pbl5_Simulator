package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Doctor extends Thread {

    private int arrivalTime = 0;
    private BlockingQueue<HospitalMessage> hospital;
    private final BlockingQueue<Message> myMailbox;

    public Doctor(int id, BlockingQueue<HospitalMessage> hospital) {
        super("Doctor " + id);
        this.hospital = hospital;
        this.myMailbox = new LinkedBlockingQueue<>();
        arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(arrivalTime);

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