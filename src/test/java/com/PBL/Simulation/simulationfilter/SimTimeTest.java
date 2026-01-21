package com.PBL.Simulation.simulationfilter;

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
        // 1 segundo real = 1 minuto simulado
        long oneSecondInNanos = 1_000_000_000L;

        SimTime simTime = new SimTime(oneSecondInNanos);

        assertEquals(0, simTime.getHours());
        assertEquals(1, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testOneSimulatedHour() {
        // 60 segundos reales = 60 minutos simulados = 1 hora
        long sixtySecondsInNanos = 60L * 1_000_000_000L;

        SimTime simTime = new SimTime(sixtySecondsInNanos);

        assertEquals(1, simTime.getHours());
        assertEquals(0, simTime.getMinutes());
        assertEquals(0, simTime.getSeconds());
    }

    @Test
    void testHourAndMinutes() {
        // 1h 15min simulados = 75 segundos reales
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
