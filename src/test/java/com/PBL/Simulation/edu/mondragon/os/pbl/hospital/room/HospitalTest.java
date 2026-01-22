package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.room.Hospital;

class HospitalTest_RoomOnly {

    private BlockingQueue<HospitalMessage> hospitalMailbox;
    private Thread hospitalThread;
    private Hospital hospital;

    @BeforeEach
    void setup() {
        hospitalMailbox = new LinkedBlockingQueue<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (hospitalThread != null) {
            hospitalThread.interrupt();
            hospitalThread.join(500);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, HospitalMessage> getBacklog() throws Exception {
        Field backlogField = Hospital.class.getDeclaredField("backlogByPhase");
        backlogField.setAccessible(true);
        return (Map<String, HospitalMessage>) backlogField.get(hospital);
    }

    private void startHospital(int numMachines) {
        hospital = new Hospital(hospitalMailbox, numMachines);
        hospitalThread = new Thread(hospital, "HospitalThreadTest");
        hospitalThread.start();
    }

    private static Message await(BlockingQueue<Message> q) throws InterruptedException {
        Message m = q.take();
        assertNotNull(m, "No llegó respuesta a tiempo");
        return m;
    }

    @Test
    @Timeout(1)
    void waitingPatient_isStoredInBacklog_andLaterReleased() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("WAITING_PATIENT", "0", machineMailbox));

        Map<String, HospitalMessage> backlog = getBacklog();
        assertTrue(backlog.containsKey("WA0"), "El backlog debería contener WA0");

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "7", patientMailbox));
        await(patientMailbox);

        assertFalse(backlog.containsKey("WA0"), "El backlog debería haberse vaciado");
    }

    @Test
    @Timeout(1)
    void waitingPatient_afterAnyFreeMachine_doesNotUseBacklog_andRepliesImmediately() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "7", patientMailbox));

        Message patientResp = await(patientMailbox);
        assertEquals("PATIENT_ASSIGNED", patientResp.type);
        assertEquals("0", patientResp.content);

        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("WAITING_PATIENT", "0", machineMailbox));

        Message machineResp = await(machineMailbox);
        assertEquals("PATIENT_ASSIGNED", machineResp.type);
        assertEquals("7", machineResp.content);

        assertNull(machineMailbox.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void anyFreeMachine_whenNoFreeMachine_isQueuedUntilFreeMachineArrives() throws Exception {
        startHospital(1);

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "1", patientMailbox));

        assertNull(patientMailbox.poll(200, TimeUnit.MILLISECONDS),
                "No debería responder si no hay máquinas libres (debe quedar en backlog FIFO)");

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        Message ack = await(admin);
        assertEquals("FREE_MACHINE_SAVED", ack.type);


        Message resp = await(patientMailbox);
        assertEquals("PATIENT_ASSIGNED", resp.type);
        assertEquals("0", resp.content);
    }


    @Test
    @Timeout(1)
    void patientIsReady_question_isBacklogged_untilPreparingForMamografy_setsChangingTrue() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p5 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "5", p5));
        Message assigned = await(p5);
        assertEquals("0", assigned.content);

        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_IS_READY?", "0", m0));

        assertNull(m0.poll(200, TimeUnit.MILLISECONDS));

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "5", new LinkedBlockingQueue<>()));

        Message ready = await(m0);
        assertEquals("PATIENT_IS_READY", ready.type);
        assertEquals("5", ready.content);
    }

    @Test
    @Timeout(1)
    void patientIsReady_direct_repliesImmediately_ifPatientAlreadyPrepared() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p5 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "5", p5));
        Message assigned = await(p5);
        assertEquals("0", assigned.content);

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "5", new LinkedBlockingQueue<>()));

        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_IS_READY?", "0", m0));

        Message ready = await(m0);
        assertEquals("PATIENT_IS_READY", ready.type);
        assertEquals("5", ready.content);

        assertNull(m0.poll(150, TimeUnit.MILLISECONDS));
    }



    @Test
    @Timeout(1)
    void hasFinishTheMamography_isBacklogged_untilMachineSendsMamographyHasFinish() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p9 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "9", p9));
        Message assigned = await(p9);
        assertEquals("0", assigned.content);

        BlockingQueue<Message> p9Leave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "9", p9Leave));

        assertNull(p9Leave.poll(200, TimeUnit.MILLISECONDS));

        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        Message ok = await(p9Leave);
        assertEquals("YOU_CAN_LEAVE", ok.type);
        assertEquals("0", ok.content);
    }

    @Test
    @Timeout(1)
    void hasFinishTheMamography_direct_repliesImmediately_ifMachineAlreadyFinished() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p9 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "9", p9));
        Message assigned = await(p9);
        assertEquals("0", assigned.content);

        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        BlockingQueue<Message> p9Leave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "9", p9Leave));

        Message ok = await(p9Leave);
        assertEquals("YOU_CAN_LEAVE", ok.type);
        assertEquals("0", ok.content);

        assertNull(p9Leave.poll(150, TimeUnit.MILLISECONDS));
    }



    @Test
    @Timeout(1)
    void patientHasGo_isBacklogged_untilPatientPreparingForLeaving_setsGoneTrue() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p4 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "4", p4));
        Message assigned = await(p4);
        assertEquals("0", assigned.content);

        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));

        assertNull(m0.poll(200, TimeUnit.MILLISECONDS));

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "4", new LinkedBlockingQueue<>()));

        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("4", finished.content);
    }

    @Test
    @Timeout(1)
    void patientHasGo_direct_returnsProcessFinished_ifPatientAlreadyGone() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p4 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "4", p4));
        Message assigned = await(p4);
        assertEquals("0", assigned.content);

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "4", new LinkedBlockingQueue<>()));

        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));

        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("4", finished.content);
    }

    @Test
    @Timeout(1)
    void fullPatientFlow_andMachineFinalization() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        BlockingQueue<Message> p = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "10", p));
        Message assigned = await(p);
        assertEquals("0", assigned.content);

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "10", new LinkedBlockingQueue<>()));

        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        BlockingQueue<Message> pLeave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "10", pLeave));
        Message canLeave = await(pLeave);
        assertEquals("YOU_CAN_LEAVE", canLeave.type);
        assertEquals("0", canLeave.content);

        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "10", new LinkedBlockingQueue<>()));

        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));
        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("10", finished.content);
    }
}
