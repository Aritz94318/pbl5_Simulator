package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.actor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.actors.Doctor;
import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

class DoctorTest {


    @Test
    void doctorSendsGetDiagnosisMessage() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> diagnosticUnit =
                new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Doctor doctor = new Doctor(1, diagnosticUnit, service);
        doctor.start();

        DiagnosticUnitMessage msg =
                diagnosticUnit.poll(10, TimeUnit.SECONDS);

        assertNotNull(msg, "Doctor should send a message to DiagnosticUnit");
        assertEquals("GET_DIAGNOSIS", msg.type);
        assertEquals("1", msg.content);

        doctor.interrupt();
        doctor.join(1000);
    }

 
    @Test
    void doctorProcessesAssignedCase() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> diagnosticUnit =
                new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Doctor doctor = new Doctor(2, diagnosticUnit, service);
        doctor.start();

        DiagnosticUnitMessage request =
                diagnosticUnit.poll(10, TimeUnit.SECONDS);

        assertNotNull(request);
        assertEquals("GET_DIAGNOSIS", request.type);

        request.replyTo.put(new Message(
                "CASE_ASSIGNED", "DX-123", null
        ));

        DiagnosticUnitMessage finalMsg =
                diagnosticUnit.poll(20, TimeUnit.SECONDS);

        assertNotNull(finalMsg, "Doctor should send FINAL_DIAGNOSIS");
        assertEquals("FINAL_DIAGNOSIS", finalMsg.type);
        assertEquals("2", finalMsg.content);

        doctor.interrupt();
        doctor.join(1000);
    }

   
    @Test
    void doctorLogsEvents() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> diagnosticUnit =
                new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Doctor doctor = new Doctor(3, diagnosticUnit, service);
        doctor.start();

        DiagnosticUnitMessage request =
                diagnosticUnit.poll(20, TimeUnit.SECONDS);

        assertNotNull(request);

        request.replyTo.put(new Message(
                "CASE_ASSIGNED", "DX", null
        ));

        Thread.sleep(500);

        verify(service, atLeastOnce())
                .postList(eq("DOCTOR"), eq(3), anyString(), anyLong());

        doctor.interrupt();
        doctor.join(1000);
    }


    @Test
    void doctorStopsWhenInterrupted() throws Exception {
        BlockingQueue<DiagnosticUnitMessage> diagnosticUnit =
                new LinkedBlockingQueue<>();
        SimulationService service = mock(SimulationService.class);

        Doctor doctor = new Doctor(4, diagnosticUnit, service);
        doctor.start();

        Thread.sleep(300);
        doctor.interrupt();

        doctor.join(1000);

        assertFalse(doctor.isAlive(),
                "Doctor thread should stop when interrupted");
    }
}
