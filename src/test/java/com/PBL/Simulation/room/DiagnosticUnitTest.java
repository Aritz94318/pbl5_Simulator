package com.PBL.Simulation.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.room.DiagnosticUnit;

class DiagnosticUnitTest {

    private BlockingQueue<DiagnosticUnitMessage> duMailbox;
    private Thread duThread;

    @BeforeEach
    void setup() {
        duMailbox = new LinkedBlockingQueue<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (duThread != null && duThread.isAlive()) {
            // STOP limpio
            duMailbox.put(new DiagnosticUnitMessage("STOP", "", null));
            duThread.join(500);

            // Por si acaso
            if (duThread.isAlive()) {
                duThread.interrupt();
                duThread.join(500);
            }
        }
    }

    private void startDiagnosticUnit() {
        DiagnosticUnit du = new DiagnosticUnit(duMailbox);
        duThread = new Thread(du, "DiagnosticUnitThreadTest");
        duThread.start();
    }

    private static Message await(BlockingQueue<Message> q) throws InterruptedException {
        Message m = q.poll(800, TimeUnit.MILLISECONDS);
        assertNotNull(m, "No llegó respuesta a tiempo");
        return m;
    }

    @Test
    @Timeout(1)
    void passMammograph_returnsAiResult_withExpectedValues() throws Exception {
        startDiagnosticUnit();

        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        duMailbox.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "patientId=1", patientMailbox));

        Message ai = await(patientMailbox);
        assertEquals("AI_RESULT", ai.type);

        // No dependemos de random: solo comprobamos que es uno de los valores válidos
        assertTrue(
                "MALIGNANT".equals(ai.content) || "BENIGN".equals(ai.content),
                "AI_RESULT content should be MALIGNANT or BENIGN, got=" + ai.content);
    }

    @Test
    @Timeout(1)
    void getDiagnosis_withoutAnyDiagnostics_isBacklogged_andDoesNotReply() throws Exception {
        startDiagnosticUnit();

        BlockingQueue<Message> doctorMailbox = new LinkedBlockingQueue<>();

        duMailbox.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "42", doctorMailbox));

        // Como no hay diagnósticos aún, debe quedarse en backlog => no responde
        assertNull(doctorMailbox.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void getDiagnosis_inBacklog_isReleased_afterPassMammograph() throws Exception {
        startDiagnosticUnit();

        BlockingQueue<Message> doctorMailbox = new LinkedBlockingQueue<>();
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        // 1) Doctor pide diagnóstico cuando no hay => backlog
        duMailbox.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "7", doctorMailbox));
        assertNull(doctorMailbox.poll(200, TimeUnit.MILLISECONDS));

        // 2) Llega una mamografía => se crea un Diagnostic y además se libera backlog
        duMailbox.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "patientX", patientMailbox));

        // El paciente recibe AI_RESULT seguro
        Message ai = await(patientMailbox);
        assertEquals("AI_RESULT", ai.type);

        // Y el doctor ahora debería recibir CASE_ASSIGNED (porque se reinyectó el
        // backlog)
        Message assigned = await(doctorMailbox);
        assertEquals("CASE_ASSIGNED", assigned.type);
        assertEquals("OK", assigned.content);
    }

    @Test
    @Timeout(1)
    void finalDiagnosis_sendsResult_toOriginalPatientReplyTo_afterCaseAssigned() throws Exception {
        startDiagnosticUnit();

        BlockingQueue<Message> doctorMailbox = new LinkedBlockingQueue<>();
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        // Aseguramos que hay al menos un diagnostic:
        duMailbox.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "patientX", patientMailbox));
        Message ai = await(patientMailbox);
        assertEquals("AI_RESULT", ai.type);

        // Doctor pide caso y debe recibir CASE_ASSIGNED (como hay 1 diagnostic, debería
        // asignar)
        duMailbox.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "99", doctorMailbox));
        Message assigned = await(doctorMailbox);
        assertEquals("CASE_ASSIGNED", assigned.type);

        // Doctor pide FINAL_DIAGNOSIS para su id=99
        duMailbox.put(new DiagnosticUnitMessage("FINAL_DIAGNOSIS", "99", new LinkedBlockingQueue<>()));

        // El resultado final se manda al replyTo del Diagnostic (patientMailbox)
        Message finalMsg = await(patientMailbox);
        assertEquals("FINAL_DIAGNOSIS", finalMsg.type);

        // Puede ser MALIGNANT, BENIGN o INCONCLUSIVE (depende de random)
        assertTrue(
                "MALIGNANT".equals(finalMsg.content) ||
                        "BENIGN".equals(finalMsg.content) ||
                        "INCONCLUSIVE".equals(finalMsg.content),
                "FINAL_DIAGNOSIS content should be MALIGNANT, BENIGN or INCONCLUSIVE, got=" + finalMsg.content);
    }
}
