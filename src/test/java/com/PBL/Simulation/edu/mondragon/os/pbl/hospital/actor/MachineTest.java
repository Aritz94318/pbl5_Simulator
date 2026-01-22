package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.actor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.actors.Machine;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

class MachineTest {

    private static <T> T takeOrFail(BlockingQueue<T> q, long timeoutMs, String err) throws InterruptedException {
        T v = q.poll(timeoutMs, TimeUnit.MILLISECONDS);
        assertNotNull(v, err);
        return v;
    }


    private static void reply(HospitalMessage msg, String type, String content) throws InterruptedException {
        msg.replyTo.put(new Message(type, content, null));
    }


    @Test
    void machinePerformsFullCycleWithProperMessageSequence() throws Exception {
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waitingRoom = new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Machine machine = new Machine(7, hospital, waitingRoom, service);

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                machine.beMachine(7);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HospitalMessage m1 = takeOrFail(hospital, 3000, "Expected FREE_MACHINE to be sent");
        assertEquals("FREE_MACHINE", m1.type);
        assertEquals("7", m1.content);
        reply(m1, "ACK", "OK");

        WaitingRoomMessage w1 = takeOrFail(waitingRoom, 3000, "Expected NEXT_PATIENT to be sent");
        assertEquals("NEXT_PATIENT", w1.type);
        assertNotNull(w1.replyTo, "NEXT_PATIENT should include a replyTo mailbox");

        HospitalMessage m2 = takeOrFail(hospital, 3000, "Expected WAITING_PATIENT to be sent");
        assertEquals("WAITING_PATIENT", m2.type);
        reply(m2, "PATIENT_STATUS", "GETTING_READY");

        HospitalMessage m3 = takeOrFail(hospital, 3000, "Expected PATIENT_IS_READY? to be sent");
        assertEquals("PATIENT_IS_READY?", m3.type);
        reply(m3, "READY", "42");

        HospitalMessage m4 = takeOrFail(hospital, 5000, "Expected MAMOGRAPHY_HAS_FINISH to be sent");
        assertEquals("MAMOGRAPHY_HAS_FINISH", m4.type);

        HospitalMessage m5 = takeOrFail(hospital, 3000, "Expected PATIENT_HAS_GO? to be sent");
        assertEquals("PATIENT_HAS_GO?", m5.type);
        reply(m5, "GONE", "OK");

        f.get(3, TimeUnit.SECONDS);

        ex.shutdownNow();
    }


    @Test
    void machineLogsEvents() throws Exception {
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waitingRoom = new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Machine machine = new Machine(3, hospital, waitingRoom, service);

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                machine.beMachine(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HospitalMessage m1 = takeOrFail(hospital, 3000, "Expected FREE_MACHINE");
        reply(m1, "ACK", "OK");

        takeOrFail(waitingRoom, 3000, "Expected NEXT_PATIENT");

        HospitalMessage m2 = takeOrFail(hospital, 3000, "Expected WAITING_PATIENT");
        reply(m2, "STATUS", "READYING");

        HospitalMessage m3 = takeOrFail(hospital, 3000, "Expected PATIENT_IS_READY?");
        reply(m3, "READY", "99");

        HospitalMessage m4 = takeOrFail(hospital, 5000, "Expected MAMOGRAPHY_HAS_FINISH");

        HospitalMessage m5 = takeOrFail(hospital, 3000, "Expected PATIENT_HAS_GO?");
        reply(m5, "GONE", "OK");

        f.get(3, TimeUnit.SECONDS);

        verify(service, atLeastOnce()).postList(eq("MACHINE"), eq(3), anyString(), anyLong());

        ex.shutdownNow();
    }


    @Test
    void machineBlocksWithoutReplies() throws Exception {
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waitingRoom = new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Machine machine = new Machine(1, hospital, waitingRoom, service);

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                machine.beMachine(1);
            } catch (InterruptedException e) {
               
            }
        });

        HospitalMessage m1 = takeOrFail(hospital, 3000, "Expected FREE_MACHINE");
        assertEquals("FREE_MACHINE", m1.type);

        assertThrows(TimeoutException.class, () -> f.get(400, TimeUnit.MILLISECONDS),
                "Machine should block if it does not receive a reply");

        ex.shutdownNow();
    }


    @Test
    void multipleMachinesCompleteOneCycleEach() throws Exception {
        int n = 5;
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waitingRoom = new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        List<Machine> machines = new ArrayList<>();
        ExecutorService ex = Executors.newFixedThreadPool(n);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int id = i + 1;
            Machine m = new Machine(id, hospital, waitingRoom, service);
            machines.add(m);
            futures.add(ex.submit(() -> {
                try {
                    m.beMachine(id);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        long deadline = System.currentTimeMillis() + 10000;
        int done = 0;

        while (System.currentTimeMillis() < deadline && done < n) {
            HospitalMessage msg = hospital.poll(200, TimeUnit.MILLISECONDS);
            if (msg == null) continue;

            switch (msg.type) {
                case "FREE_MACHINE" -> reply(msg, "ACK", "OK");
                case "WAITING_PATIENT" -> reply(msg, "STATUS", "READYING");
                case "PATIENT_IS_READY?" -> reply(msg, "READY", "10");
                case "PATIENT_HAS_GO?" -> { reply(msg, "GONE", "OK"); done++; }
                case "MAMOGRAPHY_HAS_FINISH" -> { }
                default -> { }
            }
        }

        assertEquals(n, done, "All machines should reach PATIENT_HAS_GO? and complete");

        for (Future<?> f : futures) {
            f.get(3, TimeUnit.SECONDS);
        }

        ex.shutdownNow();
    }
}
