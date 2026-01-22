package com.PBL.Simulation.edu.mondragon.os.pbl.hospital.values;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import edu.mondragon.os.pbl.hospital.values.GlobalUpdateRequest;

public class GlobalUpdateRequestTest {

    @Test
    void settersAndGetters_workCorrectly() {
        GlobalUpdateRequest req = new GlobalUpdateRequest();

        req.setNumPatients(12);
        req.setNumDoctors(3);
        req.setNumMachines(5);

        assertEquals(12, req.getNumPatients());
        assertEquals(3, req.getNumDoctors());
        assertEquals(5, req.getNumMachines());
    }

    @Test
    void defaultValues_areZero() {
        GlobalUpdateRequest req = new GlobalUpdateRequest();

        assertEquals(0, req.getNumPatients());
        assertEquals(0, req.getNumDoctors());
        assertEquals(0, req.getNumMachines());
    }
}
