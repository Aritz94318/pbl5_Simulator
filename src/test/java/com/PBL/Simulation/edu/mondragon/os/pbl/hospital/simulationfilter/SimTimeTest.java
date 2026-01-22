package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.simulationfilter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.simulationfilter.SimTime;

public class SimTimeTest {

    @Test
    void testZeroTime() {
        SimTime simTime = new SimTime(0L);

        assertEquals(0L, simTime.getTime());
        assertEquals(0, simTime.getHours());
        assertEquals(0, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testOneSimulatedMinute() {
        long oneSecondInNanos = 1_000_000_000L;

        SimTime simTime = new SimTime(oneSecondInNanos);

        assertEquals(0, simTime.getHours());
        assertEquals(1, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testOneSimulatedHour() {
        long sixtySecondsInNanos = 60L * 1_000_000_000L;

        SimTime simTime = new SimTime(sixtySecondsInNanos);

        assertEquals(1, simTime.getHours());
        assertEquals(0, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testHourAndMinutes() {
        long seventyFiveSecondsInNanos = 75L * 1_000_000_000L;

        SimTime simTime = new SimTime(seventyFiveSecondsInNanos);

        assertEquals(1, simTime.getHours());
        assertEquals(15, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testSimMethodsConsistency() {
        long time = 130L * 1_000_000_000L; // 130 min simulados

        SimTime simTime = new SimTime(time);

        assertEquals(simTime.getSimHours(), simTime.getHours());
        assertEquals(simTime.getSimMinutes(), simTime.getMinutes());
        assertEquals(simTime.getSimSeconds(), simTime.getSeconds());
    }
}
