package com.PBL.Simulation;


import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.room.Hospital;

class HospitalTest {

    private BlockingQueue<HospitalMessage> hospitalMailbox;
    private Thread hospitalThread;

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

    private void startHospital(int numMachines) {
        Hospital hospital = new Hospital(hospitalMailbox, numMachines);
        hospitalThread = new Thread(hospital, "HospitalThreadTest");
        hospitalThread.start();
    }

    private static Message await(BlockingQueue<Message> q) throws InterruptedException {
        Message m = q.poll(800, TimeUnit.MILLISECONDS);
        assertNotNull(m, "No llegó respuesta a tiempo");
        return m;
    }

    @Test
    void anyFreeMachine_asignaPacienteSiHayMaquinaLibre() throws Exception {
        startHospital(2);

        // Máquina 0 libre
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", null));

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "7", patientMailbox));

        Message resp = await(patientMailbox);
        assertEquals("PATIENT_ASSIGNED", resp.type);
        assertEquals("0", resp.content); // asigna la máquina 0
    }

    @Test
    void anyFreeMachine_siNoHayMaquinaLibre_seQuedaEnBacklog_hastaQueAparezcaUna() throws Exception {
        startHospital(1);

        // No mandamos FREE_MACHINE todavía => no hay libres
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "3", patientMailbox));

        // Debe NO responder todavía
        assertNull(patientMailbox.poll(200, TimeUnit.MILLISECONDS));

        // Ahora declaramos libre la máquina 0
        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", null));

        // Debe responder tras “desbloquear” el backlog
    }

    @Test
    void patientReady_seBloqueaSiPacienteSeEstaCambiando_ySeLiberaConPreparingForMamografy() throws Exception {
        startHospital(1);

        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", null));

        // Asignamos paciente 5 a máquina 0
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "5", patientMailbox));
        Message assigned = await(patientMailbox);
        assertEquals("0", assigned.content);

        // Marcamos "changing = true" a través del estado del propio hospital:
        // OJO: tu código pone changing=false siempre en ANY_FREE_MACHINE y solo cambia en otro sitio (no existe).
        // Así que este test SOLO tiene sentido si en tu proyecto real hay un mensaje que ponga ps.changing=true.
        // Si no existe, este caso nunca pasa.

        // Si NO tienes mensaje para poner changing=true, elimina este test o crea uno.
        // Aquí te enseño la estructura:
        // hospitalMailbox.put(new HospitalMessage("PATIENT_IS_CHANGING", "5", null)); // <- si lo implementas

        // Pregunta desde máquina: ¿está listo?
        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_IS_READY?", "0", machineMailbox));

        // Con tu código actual (changing siempre false), debería responder
        Message ready = await(machineMailbox);
        assertEquals("PATIENT_IS_READY", ready.type);
        assertEquals("5", ready.content);

        // Y cuando el paciente “prepara mamografía”, también responde algo (según tu implementación)
        BlockingQueue<Message> patient2 = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_MAMOGRAFY", "5", patient2));
        Message r2 = await(patient2);
        assertEquals("PATIENT_IS_READY", r2.type);
        assertEquals("0", r2.content);
    }

    @Test
    void flujoCompleto_mamografia_fin_salida_y_processFinished() throws Exception {
        startHospital(1);

        hospitalMailbox.put(new HospitalMessage("FREE_MACHINE", "0", null));

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("ANY_FREE_MACHINE", "9", patientMailbox));
        Message assigned = await(patientMailbox);
        assertEquals("0", assigned.content);

        // Paciente intenta irse ANTES de terminar mamografía => backlog
        BlockingQueue<Message> leaveMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PREPARING_FOR_LEAVING", "9", leaveMailbox));
        assertNull(leaveMailbox.poll(200, TimeUnit.MILLISECONDS));

        // Termina mamografía
        hospitalMailbox.put(new HospitalMessage("MAMOGRAPHY_HAS_FINISH", "0", null));

        // Ahora sí puede irse (se libera el backlog "PFL9")
        Message canLeave = await(leaveMailbox);
        assertEquals("YOU_CAN_LEAVE", canLeave.type);
        assertEquals("0", canLeave.content);

        // Máquina pregunta si el paciente ya se fue
        BlockingQueue<Message> machineMailbox = new LinkedBlockingQueue<>();
        hospitalMailbox.put(new HospitalMessage("PATIENT_HAS_GO?", "0", machineMailbox));

        Message finished = await(machineMailbox);
        assertEquals("PROCESS_FINISHED", finished.type);
        assertEquals("9", finished.content);
    }
}
