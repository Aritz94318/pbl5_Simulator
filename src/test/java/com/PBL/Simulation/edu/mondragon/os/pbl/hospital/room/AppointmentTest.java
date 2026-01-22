package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.room.Appointment;

class AppointmentTest {

    private BlockingQueue<AppointmentMessage> appointmentMailbox;
    private Thread appointmentThread;

    @BeforeEach
    void setup() {
        appointmentMailbox = new LinkedBlockingQueue<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (appointmentThread != null && appointmentThread.isAlive()) {
            
            appointmentMailbox.put(new AppointmentMessage("STOP", "", new LinkedBlockingQueue<>()));
            appointmentThread.join(500);

            if (appointmentThread.isAlive()) {
                appointmentThread.interrupt();
                appointmentThread.join(500);
            }
        }
    }

    private void startAppointment() {
        Appointment appointment = new Appointment(appointmentMailbox);
        appointmentThread = new Thread(appointment, "AppointmentThreadTest");
        appointmentThread.start();
    }

    private static Message await(BlockingQueue<Message> q) throws InterruptedException {
        Message m = q.poll(800, TimeUnit.MILLISECONDS);
        assertNotNull(m, "No llegó respuesta a tiempo");
        return m;
    }

    @Test
    void requestAppointment_returnsIncrementingTickets() throws Exception {
        startAppointment();

        BlockingQueue<Message> clientMailbox = new LinkedBlockingQueue<>();

        appointmentMailbox.put(new AppointmentMessage("REQUEST_APPOINTMENT", "client1", clientMailbox));
        Message r1 = await(clientMailbox);
        assertEquals("APPOINTMENT_GRANTED", r1.type);
        assertEquals("1", r1.content);

        appointmentMailbox.put(new AppointmentMessage("REQUEST_APPOINTMENT", "client2", clientMailbox));
        Message r2 = await(clientMailbox);
        assertEquals("APPOINTMENT_GRANTED", r2.type);
        assertEquals("2", r2.content);

        appointmentMailbox.put(new AppointmentMessage("REQUEST_APPOINTMENT", "client3", clientMailbox));
        Message r3 = await(clientMailbox);
        assertEquals("APPOINTMENT_GRANTED", r3.type);
        assertEquals("3", r3.content);
    }

    @Test
    void stop_message_finishes_thread() throws Exception {
        startAppointment();

        appointmentMailbox.put(new AppointmentMessage("STOP", "", new LinkedBlockingQueue<>()));

        appointmentThread.join(500);
        assertFalse(appointmentThread.isAlive(), "El hilo de Appointment no terminó tras STOP");
    }

    @Test
    void defaultMessage_doesNotCrashOrBlockRoom() throws Exception {
        startAppointment();

        BlockingQueue<Message> clientMailbox = new LinkedBlockingQueue<>();

        appointmentMailbox.put(
                new AppointmentMessage("UNKNOWN_TYPE", "whatever", clientMailbox));

        Message reply = clientMailbox.poll(300, TimeUnit.MILLISECONDS);
        assertNull(reply, "No debería haber respuesta para un mensaje desconocido");

        appointmentMailbox.put(
                new AppointmentMessage("REQUEST_APPOINTMENT", "client1", clientMailbox));

        Message validReply = await(clientMailbox);
        assertEquals("APPOINTMENT_GRANTED", validReply.type);
        assertEquals("1", validReply.content);

    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)

    void interruptWhileWaiting_stopsThread() throws Exception {
        startAppointment();


        Thread.sleep(50);

        appointmentThread.interrupt();
        appointmentThread.join(500);

        assertFalse(appointmentThread.isAlive(), "El hilo debería terminar tras InterruptedException en take()");
        assertTrue(appointmentThread.isInterrupted() || !appointmentThread.isAlive());
    }

}
