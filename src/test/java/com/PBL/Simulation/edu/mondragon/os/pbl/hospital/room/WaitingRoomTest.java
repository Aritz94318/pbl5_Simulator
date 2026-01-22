package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.room.WaitingRoom;

class WaitingRoomTest {

    private BlockingQueue<WaitingRoomMessage> wrMailbox;
    private Thread wrThread;

    @BeforeEach
    void setup() {
        wrMailbox = new LinkedBlockingQueue<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (wrThread != null && wrThread.isAlive()) {
          
            wrMailbox.put(new WaitingRoomMessage("STOP", "", null));
            wrThread.join(500);

           
            if (wrThread.isAlive()) {
                wrThread.interrupt();
                wrThread.join(500);
            }
        }
    }

    private void startWaitingRoom() {
        WaitingRoom wr = new WaitingRoom(wrMailbox);
        wrThread = new Thread(wr, "WaitingRoomThreadTest");
        wrThread.start();
    }

    private static Message await(BlockingQueue<Message> q) throws InterruptedException {
        Message m = q.poll(800, TimeUnit.MILLISECONDS);
        assertNotNull(m, "No llegó respuesta a tiempo");
        return m;
    }

    @Test
    @Timeout(1)
    void doesNotRelease_ifCurrentTurnTicketHasNotArrived() throws Exception {
        startWaitingRoom();

        BlockingQueue<Message> p2 = new LinkedBlockingQueue<>();

        wrMailbox.put(new WaitingRoomMessage("WAIT", "2", p2));

        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));

        assertNull(p2.poll(200, TimeUnit.MILLISECONDS),
                "No debería notificar al ticket 2 si el turno actual (1) no ha llegado");
    }

    @Test
    @Timeout(1)
    void releasesPatientsInOrder_whenMachinesBecomeFree() throws Exception {
        startWaitingRoom();

        BlockingQueue<Message> p1 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> p2 = new LinkedBlockingQueue<>();

        wrMailbox.put(new WaitingRoomMessage("WAIT", "1", p1));
        wrMailbox.put(new WaitingRoomMessage("WAIT", "2", p2));

        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));
        Message m1 = await(p1);
        assertEquals("YOUR_TURN", m1.type);
        assertEquals("1", m1.content);

        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));
        Message m2 = await(p2);
        assertEquals("YOUR_TURN", m2.type);
        assertEquals("2", m2.content);

        assertNull(p1.poll(150, TimeUnit.MILLISECONDS));
        assertNull(p2.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void releasesPatientsInOrder_whenMachinesAreAlreadyBecomeFree() throws Exception {
        startWaitingRoom();

        BlockingQueue<Message> p1 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> p2 = new LinkedBlockingQueue<>();

        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));
        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));

        assertNull(p1.poll(150, TimeUnit.MILLISECONDS));
        assertNull(p2.poll(150, TimeUnit.MILLISECONDS));

        wrMailbox.put(new WaitingRoomMessage("WAIT", "1", p1));
        wrMailbox.put(new WaitingRoomMessage("WAIT", "2", p2));

        Message m1 = await(p1);
        assertEquals("YOUR_TURN", m1.type);
        assertEquals("1", m1.content);

        Message m2 = await(p2);
        assertEquals("YOUR_TURN", m2.type);
        assertEquals("2", m2.content);

        assertNull(p1.poll(150, TimeUnit.MILLISECONDS));
        assertNull(p2.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void withOneFreeMachine_onlyCurrentTurnIsNotified() throws Exception {
        startWaitingRoom();

        BlockingQueue<Message> p1 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> p2 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> p3 = new LinkedBlockingQueue<>();

        wrMailbox.put(new WaitingRoomMessage("WAIT", "1", p1));
        wrMailbox.put(new WaitingRoomMessage("WAIT", "2", p2));
        wrMailbox.put(new WaitingRoomMessage("WAIT", "3", p3));

        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));

        Message m1 = await(p1);
        assertEquals("YOUR_TURN", m1.type);
        assertEquals("1", m1.content);

        assertNull(p2.poll(200, TimeUnit.MILLISECONDS), "No debería notificar al ticket 2 todavía");
        assertNull(p3.poll(200, TimeUnit.MILLISECONDS), "No debería notificar al ticket 3 todavía");
    }

    @Test
    @Timeout(1)
    void unknownMessage_doesNotCrash_andRoomStillWorks() throws Exception {
        startWaitingRoom();

        BlockingQueue<Message> p1 = new LinkedBlockingQueue<>();

        wrMailbox.put(new WaitingRoomMessage("WHAT_IS_THIS", "x", p1));
        assertNull(p1.poll(200, TimeUnit.MILLISECONDS),
                "No debería responder a mensajes desconocidos");

        wrMailbox.put(new WaitingRoomMessage("WAIT", "1", p1));
        wrMailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));

        Message m1 = await(p1);
        assertEquals("YOUR_TURN", m1.type);
        assertEquals("1", m1.content);
    }

    void interruptWhileWaiting_stopsThread() throws Exception {
        startWaitingRoom();


        Thread.sleep(50);

        wrThread.interrupt();
        wrThread.join(500);

        assertFalse(wrThread.isAlive(), "El hilo debería terminar tras InterruptedException en take()");
        assertTrue(wrThread.isInterrupted() || !wrThread.isAlive());
    }
}
