package edu.mondragon.os.pbl.hospital.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.mondragon.os.pbl.hospital.App;
import edu.mondragon.os.pbl.hospital.Values.GlobalState;
import edu.mondragon.os.pbl.hospital.Values.GlobalUpdateRequest;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/Simulation")
public class HospitalController {

    private final GlobalState globalState;

    public HospitalController(GlobalState globalState) {
        this.globalState = globalState;
    }

    @GetMapping(value = "/ping", produces = "application/json")
public ResponseEntity<String> ping() {
    return ResponseEntity.ok("Servidor OK");
}


    @PutMapping(value = "/modify", consumes = { "application/json", "application/xml" }, produces = {
            "application/json", "application/xml" })
    public ResponseEntity<GlobalState> modifyGlobals(@RequestBody GlobalUpdateRequest body) {

        System.out.println("✅ Entró en /Simulation/modify");
        System.out
                .println("Body: " + body.getNumPatients() + ", " + body.getNumDoctors() + ", " + body.getNumMachines());

        globalState.update(body.getNumPatients(), body.getNumDoctors(), body.getNumMachines());

        try {
            System.out.println("✅ Antes de crear App");
            App app = new App(body.getNumPatients(), body.getNumDoctors(), body.getNumMachines());
            System.out.println("✅ App creada");
            System.out.println("🚀 startThreads()");
            app.startThreads();
            System.out.println("⏳ waitEndOfThreads()");
            app.waitEndOfThreads(body.getNumPatients(),body.getNumDoctors(),body.getNumMachines());
            System.out.println("🏁 Terminó waitEndOfThreads()");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(globalState);
    }

}
