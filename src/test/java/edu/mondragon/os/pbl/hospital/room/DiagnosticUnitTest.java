/* package edu.mondragon.os.pbl.hospital.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

class DiagnosticUnitTest {

    @Test
    void diagnosticUnitTest() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> duMailbox = new LinkedBlockingQueue<>();
        BlockingQueue<Message> patientMailbox = new LinkedBlockingQueue<>();
        BlockingQueue<Message> doctorMailbox = new LinkedBlockingQueue<>();

        DiagnosticUnit du = new DiagnosticUnit(duMailbox);
        Thread duThread = new Thread(du);
        duThread.start();

        // 1️⃣ Patient sends mammography to AI
        duMailbox.put(new DiagnosticUnitMessage(
                "PASS MAMOGRAPH IN AI",
                "1",
                patientMailbox));

        Message aiResult = patientMailbox.poll(1, TimeUnit.SECONDS);
        assertNotNull(aiResult);
        assertEquals("AI_RESULT", aiResult.type);

        // 2️⃣ Doctor asks for diagnosis
        duMailbox.put(new DiagnosticUnitMessage(
                "GET_DIAGNOSIS",
                "10",
                doctorMailbox));
        Message assigned = null;

        for (int i = 0; i < 5 && assigned == null; i++) {
            duMailbox.put(new DiagnosticUnitMessage(
                    "GET_DIAGNOSIS",
                    "10",
                    doctorMailbox));

            assigned = doctorMailbox.poll(1, TimeUnit.SECONDS);
        }

        assertNotNull(assigned);
        assertEquals("CASE_ASSIGNED", assigned.type);

        // 3️⃣ Doctor sends final diagnosis
        duMailbox.put(new DiagnosticUnitMessage(
                "FINAL_DIAGNOSIS",
                "10",
                doctorMailbox));

        Message finalDiagnosis = patientMailbox.poll(1, TimeUnit.SECONDS);
        assertNotNull(finalDiagnosis);
        assertEquals("FINAL_DIAGNOSIS", finalDiagnosis.type);

        // Stop
        duMailbox.put(new DiagnosticUnitMessage("STOP", "", null));
        duThread.join(1000);
    }
}
 */