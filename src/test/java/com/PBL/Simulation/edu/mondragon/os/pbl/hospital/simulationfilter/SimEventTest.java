package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.simulationfilter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.simulationfilter.SimEvent;

public class SimEventTest {

    @Test
    void testEmptyConstructor() {
        SimEvent event = new SimEvent();

        assertNull(event.getActor());
        assertEquals(0, event.getActorId());
        assertNull(event.getText());
        assertEquals(0L, event.getTs());
    }

    @Test
    void testConstructorWithParameters() {
        long timestamp = System.currentTimeMillis();

        SimEvent event = new SimEvent(
                "PATIENT",
                5,
                "Arrives at hospital",
                timestamp
        );

        assertEquals("PATIENT", event.getActor());
        assertEquals(5, event.getActorId());
        assertEquals("Arrives at hospital", event.getText());
        assertEquals(timestamp, event.getTs());
    }

    @Test
    void testSettersAndGetters() {
        SimEvent event = new SimEvent();

        event.setActor("DOCTOR");
        event.setActorId(2);
        event.setText("Diagnosis completed");
        event.setTs(12345L);

        assertEquals("DOCTOR", event.getActor());
        assertEquals(2, event.getActorId());
        assertEquals("Diagnosis completed", event.getText());
        assertEquals(12345L, event.getTs());
    }
}
