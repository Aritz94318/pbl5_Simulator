package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.actor;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.actors.Patient;
import edu.mondragon.os.pbl.hospital.mailbox.*;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

class PatientTest {


    private static <T> T takeOrFail(BlockingQueue<T> q, long timeoutMs, String error)
            throws InterruptedException {
        T v = q.poll(timeoutMs, TimeUnit.MILLISECONDS);
        assertNotNull(v, error);
        return v;
    }

    private static void reply(BlockingQueue<Message> mailbox, String type, String content)
            throws InterruptedException {
        mailbox.put(new Message(type, content, null));
    }



    @Test
    void attendPatientSendsCorrectHospitalFlow() throws Exception {

        BlockingQueue<AppointmentMessage> appointment = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waiting = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> diagnostic = new LinkedBlockingQueue<>();

        SimulationService service = mock(SimulationService.class);

        Patient patient = new Patient(
                5, appointment, hospital, waiting, diagnostic, service
        );

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                patient.attendPatient();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HospitalMessage m1 =
                takeOrFail(hospital, 3000, "Expected ANY_FREE_MACHINE");
        assertEquals("ANY_FREE_MACHINE", m1.type);
        reply(m1.replyTo, "MACHINE_ASSIGNED", "2");

        HospitalMessage m2 =
                takeOrFail(hospital, 4000, "Expected PREPARING_FOR_MAMOGRAFY");
        assertEquals("PREPARING_FOR_MAMOGRAFY", m2.type);

        HospitalMessage m3 =
                takeOrFail(hospital, 3000, "Expected HAS_FINISH_THE_MAMOGRAPHY");
        assertEquals("HAS_FINISH_THE_MAMOGRAPHY", m3.type);

        reply(m3.replyTo, "MAMMO_DONE", "2");

        HospitalMessage m4 =
                takeOrFail(hospital, 12000, "Expected PREPARING_FOR_LEAVING");
        assertEquals("PREPARING_FOR_LEAVING", m4.type);

        f.get(2, TimeUnit.SECONDS);
        ex.shutdownNow();
    }


    @Test
    void doctorsDiagnosticsFlowIsCorrect() throws Exception {

        BlockingQueue<AppointmentMessage> appointment = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waiting = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> diagnostic = new LinkedBlockingQueue<>();

        SimulationService service = mock(SimulationService.class);

        Patient patient = new Patient(
                9, appointment, hospital, waiting, diagnostic, service
        );

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                patient.doctorsDiagnostics(9);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        DiagnosticUnitMessage dm =
                takeOrFail(diagnostic, 3000, "Expected PASS MAMOGRAPH IN AI");
        assertEquals("PASS MAMOGRAPH IN AI", dm.type);
        assertEquals("9", dm.content);

        reply(dm.replyTo, "AI_RECEIVED", "BENIGN");
        reply(dm.replyTo, "FINAL_DIAGNOSIS", "BENIGN");

        f.get(2, TimeUnit.SECONDS);
        ex.shutdownNow();
    }



    @Test
    void patientLogsEvents() throws Exception {

        BlockingQueue<AppointmentMessage> appointment = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waiting = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> diagnostic = new LinkedBlockingQueue<>();

        SimulationService service = mock(SimulationService.class);

        Patient patient = new Patient(
                2, appointment, hospital, waiting, diagnostic, service
        );

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                patient.attendPatient();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HospitalMessage m1 =
                takeOrFail(hospital, 3000, "Expected ANY_FREE_MACHINE");
        reply(m1.replyTo, "MACHINE_ASSIGNED", "1");

        HospitalMessage m2 =
                takeOrFail(hospital, 4000, "Expected PREPARING_FOR_MAMOGRAFY");
        assertEquals("PREPARING_FOR_MAMOGRAFY", m2.type);

        HospitalMessage m3 =
                takeOrFail(hospital, 3000, "Expected HAS_FINISH_THE_MAMOGRAPHY");
        reply(m3.replyTo, "MAMMO_DONE", "1");

        takeOrFail(hospital, 12000, "Expected PREPARING_FOR_LEAVING");

        f.get(2, TimeUnit.SECONDS);

        verify(service, atLeastOnce())
                .postList(eq("PATIENT"), eq(2), anyString(), anyLong());

        ex.shutdownNow();
    }



    @Test
    void attendPatientBlocksWithoutMachineAssignment() throws Exception {

        BlockingQueue<AppointmentMessage> appointment = new LinkedBlockingQueue<>();
        BlockingQueue<HospitalMessage> hospital = new LinkedBlockingQueue<>();
        BlockingQueue<WaitingRoomMessage> waiting = new LinkedBlockingQueue<>();
        BlockingQueue<DiagnosticUnitMessage> diagnostic = new LinkedBlockingQueue<>();

        SimulationService service = mock(SimulationService.class);

        Patient patient = new Patient(
                11, appointment, hospital, waiting, diagnostic, service
        );

        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> {
            try {
                patient.attendPatient();
            } catch (InterruptedException e) {
            }
        });

        HospitalMessage m1 =
                takeOrFail(hospital, 3000, "Expected ANY_FREE_MACHINE");
        assertEquals("ANY_FREE_MACHINE", m1.type);

        assertThrows(TimeoutException.class,
                () -> f.get(600, TimeUnit.MILLISECONDS));

        ex.shutdownNow();
    }
}
