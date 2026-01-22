package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.room;

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
           
            duMailbox.put(new DiagnosticUnitMessage("STOP", "", null));
            duThread.join(500);

           
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

        assertNull(doctorMailbox.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(1)
    void getDiagnosis_inBacklog_isReleased_afterPassMammograph() throws Exception {
        startDiagnosticUnit();

        BlockingQueue<Message> doctorMailbox = new LinkedBlockingQueue<>();
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();

        duMailbox.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "7", doctorMailbox));
        assertNull(doctorMailbox.poll(200, TimeUnit.MILLISECONDS));

        duMailbox.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "patientX", patientMailbox));

        Message ai = await(patientMailbox);
        assertEquals("AI_RESULT", ai.type);

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

        
        duMailbox.put(new DiagnosticUnitMessage("PASS MAMOGRAPH IN AI", "patientX", patientMailbox));
        Message ai = await(patientMailbox);
        assertEquals("AI_RESULT", ai.type);

        
        duMailbox.put(new DiagnosticUnitMessage("GET_DIAGNOSIS", "99", doctorMailbox));
        Message assigned = await(doctorMailbox);
        assertEquals("CASE_ASSIGNED", assigned.type);

        duMailbox.put(new DiagnosticUnitMessage("FINAL_DIAGNOSIS", "99", new LinkedBlockingQueue<>()));

        Message finalMsg = await(patientMailbox);
        assertEquals("FINAL_DIAGNOSIS", finalMsg.type);

        assertTrue(
                "MALIGNANT".equals(finalMsg.content) ||
                        "BENIGN".equals(finalMsg.content) ||
                        "INCONCLUSIVE".equals(finalMsg.content),
                "FINAL_DIAGNOSIS content should be MALIGNANT, BENIGN or INCONCLUSIVE, got=" + finalMsg.content);
    }
}
