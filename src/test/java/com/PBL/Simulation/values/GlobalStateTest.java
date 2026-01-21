package com.PBL.Simulation.values;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.values.GlobalState;

public class GlobalStateTest {

    @Test
    void initialState_isZero() {
        GlobalState state = new GlobalState();

        assertEquals(0, state.getNumPatients());
        assertEquals(0, state.getNumDoctors());
        assertEquals(0, state.getNumMachines());
    }

    @Test
    void update_setsAllValuesCorrectly() {
        GlobalState state = new GlobalState();

        state.update(10, 3, 5);

        assertEquals(10, state.getNumPatients());
        assertEquals(3, state.getNumDoctors());
        assertEquals(5, state.getNumMachines());
    }

    @Test
    void concurrentUpdates_doNotCorruptState() throws Exception {
        GlobalState state = new GlobalState();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            awaitLatch(latch);
            state.update(5, 1, 2);
        });

        executor.submit(() -> {
            awaitLatch(latch);
            state.update(20, 4, 6);
        });

        executor.submit(() -> {
            awaitLatch(latch);
            state.update(15, 2, 3);
        });

        // Arrancan todos a la vez
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);

        // El estado final debe ser uno de los updates completos (no mezclado)
        boolean valid =
                (state.getNumPatients() == 5  && state.getNumDoctors() == 1 && state.getNumMachines() == 2) ||
                (state.getNumPatients() == 20 && state.getNumDoctors() == 4 && state.getNumMachines() == 6) ||
                (state.getNumPatients() == 15 && state.getNumDoctors() == 2 && state.getNumMachines() == 3);

        assertTrue(valid, "GlobalState ended in an inconsistent state");
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
