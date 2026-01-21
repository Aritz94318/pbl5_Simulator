package com.PBL.Simulation.room;

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

    // -------------------------
    // 1) Asignación de máquina
    // -------------------------
    @Test
    @Timeout(1)
    void waitingPatient_isStoredInBacklog_andLaterReleased() throws Exception {
        startHospital(1);

        // Máquina libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // WAITING_PATIENT -> debe ir a backlog
        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("WAITING_PATIENT", "0", machineMailbox));

        Map<String, HospitalMessage> backlog = getBacklog();
        assertTrue(backlog.containsKey("WA0"), "El backlog debería contener WA0");

        // ANY_FREE_MACHINE libera WA0
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "7", patientMailbox));
        await(patientMailbox);

        // El backlog debería quedar vacío
        assertFalse(backlog.containsKey("WA0"), "El backlog debería haberse vaciado");
    }

    @Test
    @Timeout(1)
    void waitingPatient_afterAnyFreeMachine_doesNotUseBacklog_andRepliesImmediately() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // 1) Paciente 7 pide ANY_FREE_MACHINE -> asignado a 0
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "7", patientMailbox));

        Message patientResp = await(patientMailbox);
        assertEquals("PATIENT_ASSIGNED", patientResp.type);
        assertEquals("0", patientResp.content);

        // 2) Ahora la máquina pregunta WAITING_PATIENT y debe responder YA (no backlog)
        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("WAITING_PATIENT", "0", machineMailbox));

        Message machineResp = await(machineMailbox);
        assertEquals("PATIENT_ASSIGNED", machineResp.type);
        assertEquals("7", machineResp.content);

        // (Indirecto) si hubiese backlog raro, podría duplicar; comprobamos que no hay
        // extra
        assertNull(machineMailbox.poll(150, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void anyFreeMachine_whenNoFreeMachine_isQueuedUntilFreeMachineArrives() throws Exception {
        startHospital(1);

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        // 1) Paciente pide ANY_FREE_MACHINE cuando no hay ninguna máquina registrada
        // como libre
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "1", patientMailbox));

        // Debe NO responder (se mete en backlogByFifo)
        assertNull(patientMailbox.poll(200, TimeUnit.MILLISECONDS),
                "No debería responder si no hay máquinas libres (debe quedar en backlog FIFO)");

        // 2) Ahora aparece una máquina libre (0)
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        Message ack = await(admin);
        assertEquals("FREE_MACHINE_SAVED", ack.type);

        // 3) Ahora el paciente debe recibir asignación (cuando el hospital reinyecta el
        // backlog)
        Message resp = await(patientMailbox);
        assertEquals("PATIENT_ASSIGNED", resp.type);
        assertEquals("0", resp.content);
    }

    // -------------------------
    // 2) Backlog PIR (Patient ready?)
    // -------------------------

    @Test
    @Timeout(1)
    void patientIsReady_question_isBacklogged_untilPreparingForMamografy_setsChangingTrue() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 5 asignado a máquina 0 (ps.changing=false inicialmente)
        BlockingQueue<Message> p5 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "5", p5));
        Message assigned = await(p5);
        assertEquals("0", assigned.content);

        // Máquina pregunta si paciente está listo
        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_IS_READY?", "0", m0));

        // si !ps.changing => backlog => NO responde aún
        assertNull(m0.poll(200, TimeUnit.MILLISECONDS));

        // Paciente entra a PREPARING_FOR_MAMOGRAFY => ps.changing=true => libera
        // backlog PIR
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "5", new LinkedBlockingQueue<>()));

        Message ready = await(m0);
        assertEquals("PATIENT_IS_READY", ready.type);
        assertEquals("5", ready.content);
    }

    @Test
    @Timeout(1)
    void patientIsReady_direct_repliesImmediately_ifPatientAlreadyPrepared() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 5 asignado a máquina 0
        BlockingQueue<Message> p5 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "5", p5));
        Message assigned = await(p5);
        assertEquals("0", assigned.content);

        // Paciente se prepara ANTES de que la máquina pregunte
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "5", new LinkedBlockingQueue<>()));

        // Ahora la máquina pregunta si el paciente está listo -> respuesta inmediata
        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_IS_READY?", "0", m0));

        Message ready = await(m0);
        assertEquals("PATIENT_IS_READY", ready.type);
        assertEquals("5", ready.content);

        // Opcional: comprobar que no hay mensajes extra
        assertNull(m0.poll(150, TimeUnit.MILLISECONDS));
    }

    // -------------------------
    // 3) Backlog MHF (Mamography finish gating)
    // -------------------------

    @Test
    @Timeout(1)
    void hasFinishTheMamography_isBacklogged_untilMachineSendsMamographyHasFinish() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 9 asignado
        BlockingQueue<Message> p9 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "9", p9));
        Message assigned = await(p9);
        assertEquals("0", assigned.content);

        // Paciente pregunta si ya acabó la mamografía
        BlockingQueue<Message> p9Leave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "9", p9Leave));

        // Como la máquina no ha enviado finish => backlog => no responde
        assertNull(p9Leave.poll(200, TimeUnit.MILLISECONDS));

        // Máquina 0 marca finish
        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        // Ahora sí responde al paciente
        Message ok = await(p9Leave);
        assertEquals("YOU_CAN_LEAVE", ok.type);
        assertEquals("0", ok.content);
    }

    @Test
    @Timeout(1)
    void hasFinishTheMamography_direct_repliesImmediately_ifMachineAlreadyFinished() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 9 asignado a máquina 0
        BlockingQueue<Message> p9 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "9", p9));
        Message assigned = await(p9);
        assertEquals("0", assigned.content);

        // Máquina 0 marca finish ANTES de que el paciente pregunte
        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        // Paciente pregunta si ya acabó la mamografía -> debe responder inmediato (sin
        // backlog)
        BlockingQueue<Message> p9Leave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "9", p9Leave));

        Message ok = await(p9Leave);
        assertEquals("YOU_CAN_LEAVE", ok.type);
        assertEquals("0", ok.content);

        // Opcional: asegurarnos de que no hay mensajes extra
        assertNull(p9Leave.poll(150, TimeUnit.MILLISECONDS));
    }

    // -------------------------
    // 4) Backlog PHG (Patient has gone?)
    // -------------------------

    @Test
    @Timeout(1)
    void patientHasGo_isBacklogged_untilPatientPreparingForLeaving_setsGoneTrue() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 4 asignado a máquina 0
        BlockingQueue<Message> p4 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "4", p4));
        Message assigned = await(p4);
        assertEquals("0", assigned.content);

        // Máquina pregunta si el paciente se ha ido
        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));

        // Como ps.gone=false => backlog => no responde
        assertNull(m0.poll(200, TimeUnit.MILLISECONDS));

        // Paciente se va
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "4", new LinkedBlockingQueue<>()));

        // Ahora debería responder PROCESS_FINISHED y resetear máquina
        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("4", finished.content);
    }

    @Test
    @Timeout(1)
    void patientHasGo_direct_returnsProcessFinished_ifPatientAlreadyGone() throws Exception {
        startHospital(1);

        // Máquina 0 libre
        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // Paciente 4 asignado a máquina 0
        BlockingQueue<Message> p4 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "4", p4));
        Message assigned = await(p4);
        assertEquals("0", assigned.content);

        // Paciente se va ANTES de que la máquina pregunte
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "4", new LinkedBlockingQueue<>()));

        // Ahora la máquina pregunta si el paciente ya se ha ido -> debe responder
        // inmediato (sin backlog)
        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));

        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("4", finished.content);
    }

    // -------------------------
    // 5) Flujo completo "feliz"
    // -------------------------

    @Test
    @Timeout(1)
    void fullPatientFlow_andMachineFinalization() throws Exception {
        startHospital(1);

        BlockingQueue<Message> admin = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", admin));
        await(admin);

        // 1) Patient ANY_FREE_MACHINE
        BlockingQueue<Message> p = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "10", p));
        Message assigned = await(p);
        assertEquals("0", assigned.content);

        // 2) Patient PREPARING_FOR_MAMOGRAFY (desbloquea PIR si existiera)
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "10", new LinkedBlockingQueue<>()));

        // 3) Machine says mamography finished
        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", new LinkedBlockingQueue<>()));

        // 4) Patient asks HAS_FINISH... => debe responder inmediato (ya está done)
        BlockingQueue<Message> pLeave = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("HAS_FINISH_THE_MAMOGRAPHY", "10", pLeave));
        Message canLeave = await(pLeave);
        assertEquals("YOU_CAN_LEAVE", canLeave.type);
        assertEquals("0", canLeave.content);

        // 5) Patient PREPARING_FOR_LEAVING
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "10", new LinkedBlockingQueue<>()));

        // 6) Machine asks PATIENT_HAS_GO? => responde PROCESS_FINISHED
        BlockingQueue<Message> m0 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", m0));
        Message finished = await(m0);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("10", finished.content);
    }
}
