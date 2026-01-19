package edu.mondragon.os.pbl.hospital.room;


import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Appointment implements Runnable {

    private final BlockingQueue<AppointmentMessage> mailbox;
    private int nextTicket = 1;

    public Appointment(BlockingQueue<AppointmentMessage> mailbox) {
        this.mailbox = mailbox;
    }

    @Override
    public void run() {
        try {
            while (true) {
                
                AppointmentMessage msg = mailbox.take(); // espera solicitudes
                if (msg.type.equals("STOP")) {
                    break;
                }

                if (msg.type.equals("REQUEST_APPOINTMENT")) {
                    int ticket = nextTicket++;

                    // respuesta al cliente
                    msg.replyTo.put(
                        new Message("APPOINTMENT_GRANTED",
                                    String.valueOf(ticket),
                                    null)
                    );
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

