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

                switch (msg.type) {

                    case "STOP":
                        // Finaliza el hilo de la sala de citas
                        return; // o break + flag, según tu run()

                    case "REQUEST_APPOINTMENT":
                        int ticket = nextTicket++;

                        // Respuesta al cliente
                        msg.replyTo.put(
                                new Message(
                                        "APPOINTMENT_GRANTED",
                                        String.valueOf(ticket),
                                        null));
                        break;

                    default:
                        // Mensaje no reconocido (opcional, pero recomendable)
                        System.out.println("⚠️ Unknown message type: " + msg.type);
                        break;
                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}
