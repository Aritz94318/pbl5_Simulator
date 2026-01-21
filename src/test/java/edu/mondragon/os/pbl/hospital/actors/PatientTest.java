/* package edu.mondragon.os.pbl.hospital.actors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;
import org.junit.jupiter.api.Test;
public class PatientTest {
    @Test
void patientRequestsAppointment() throws Exception {
    BlockingQueue<AppointmentMessage> app = new LinkedBlockingQueue<>();
    BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
    BlockingQueue<WaitingRoomMessage> waiting = new LinkedBlockingQueue<>();
    BlockingQueue<DiagnosticUnitMessage> du = new LinkedBlockingQueue<>();

    SimulationService service = mock(SimulationService.class);

    Patient p = new Patient(1, app, hospital, waiting, du, service);
    p.start();

    AppointmentMessage m =
        app.poll(3, TimeUnit.SECONDS);

    assertNotNull(m);
    assertEquals("REQUEST_APPOINTMENT", m.type);

    p.interrupt();
}

}
 */