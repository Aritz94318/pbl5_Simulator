package com.PBL.Simulation.values;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.values.Diagnostic;

public class DiagnosticTest {

    @Test
    void constructor_setsAllFieldsCorrectly() {
        BlockingQueue<Message> replyTo = new LinkedBlockingQueue<>();

        Diagnostic diagnostic = new Diagnostic(
                "patientA",
                "MALIGNANT",
                replyTo
        );

        assertEquals("patientA", diagnostic.getPatientName());
        assertEquals("MALIGNANT", diagnostic.getDiagnosis());
        assertSame(replyTo, diagnostic.getReplyTo());
    }

    @Test
    void setters_updateValuesCorrectly() {
        BlockingQueue<Message> initialQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Message> newQueue = new LinkedBlockingQueue<>();

        Diagnostic diagnostic = new Diagnostic(
                "patientA",
                "BENIGN",
                initialQueue
        );

        diagnostic.setPatientName("patientB");
        diagnostic.setPositive("INCONCLUSIVE");
        diagnostic.setReplyTo(newQueue);

        assertEquals("patientB", diagnostic.getPatientName());
        assertEquals("INCONCLUSIVE", diagnostic.getDiagnosis());
        assertSame(newQueue, diagnostic.getReplyTo());
    }

    @Test
    void toString_containsRelevantInformation() {
        BlockingQueue<Message> replyTo = new LinkedBlockingQueue<>();

        Diagnostic diagnostic = new Diagnostic(
                "patientX",
                "BENIGN",
                replyTo
        );

        String text = diagnostic.toString();

        assertTrue(text.contains("patientX"));
        assertTrue(text.contains("BENIGN"));
        assertTrue(text.startsWith("Diagnostic{"));
    }
}
