package edu.mondragon.os.pbl.hospital.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.mondragon.os.pbl.hospital.App;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;
import edu.mondragon.os.pbl.hospital.values.GlobalState;
import edu.mondragon.os.pbl.hospital.values.GlobalUpdateRequest;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/Simulation")
public class HospitalController {

    private final GlobalState globalState;
    private final SimulationService simulationService;

    public HospitalController(GlobalState globalState,
                              SimulationService simulationService) {
        this.globalState = globalState;
        this.simulationService = simulationService;
        globalState.update(1, 1, 1);
    }

    @GetMapping(value = "/ping", produces = "application/json")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Server OK");
    }

    @PutMapping(value = "/modify", consumes = { "application/json", "application/xml" }, produces = {
            "application/json", "application/xml" })
    public ResponseEntity<GlobalState> modifyGlobals(@RequestBody GlobalUpdateRequest body) {

        if ((body.getNumPatients() == 0) && (body.getNumDoctors() == 0) && (body.getNumMachines() == 0)) {
            return ResponseEntity.badRequest().build();
        }
        System.out.println("✅ Entered /Simulation/modify");

        System.out
                .println("Body: " + body.getNumPatients() + ", " + body.getNumDoctors() + ", " + body.getNumMachines());

        globalState.update(body.getNumPatients(), body.getNumDoctors(), body.getNumMachines());

        return ResponseEntity.ok(globalState);
    }

    @PostMapping(value = "/start", produces = { "application/json", "application/xml" })
    public ResponseEntity<GlobalState> startSimulation() {

        System.out.println("✅ Entered /Simulation/start");

        new Thread(() -> {
            try {
                long startNs = System.nanoTime();
                App app = new App(
                        globalState.getNumPatients(),
                        globalState.getNumDoctors(),
                        globalState.getNumMachines());
                app.startThreads();
                app.waitEndOfThreads(
                        globalState.getNumPatients(),
                        globalState.getNumDoctors(),
                        globalState.getNumMachines());
                System.out.println("🏁 Simulation finished");
                long endNs = System.nanoTime();
                long elapsedNs = endNs - startNs;
                simulationService.sendFinalTime(elapsedNs);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "simulation-runner").start();


        // 202 Accepted: “he arrancado el proceso”
        return ResponseEntity.accepted().body(globalState);
    }

}
