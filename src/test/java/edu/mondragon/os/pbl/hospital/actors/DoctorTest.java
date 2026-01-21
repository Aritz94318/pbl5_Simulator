package edu.mondragon.os.pbl.hospital.actors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;


import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

class DoctorTest {
    @Test
    void doctorRequestsAndReceivesDiagnosis() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> duMailbox = new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Doctor doctor = new Doctor(1, duMailbox, service);
        doctor.start();

        DiagnosticUnitMessage msg = duMailbox.poll(3, TimeUnit.SECONDS);

        assertNotNull(msg);
        assertEquals("GET_DIAGNOSIS", msg.type);

        // Simulamos que el DiagnosticUnit responde
        msg.replyTo.put(new Message("CASE_ASSIGNED", "OK", null));

        Thread.sleep(200);
        doctor.interrupt();
    }

}
