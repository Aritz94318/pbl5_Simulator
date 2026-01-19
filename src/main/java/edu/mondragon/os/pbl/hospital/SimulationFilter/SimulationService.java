package edu.mondragon.os.pbl.hospital.simulationfilter;

import java.util.concurrent.Semaphore;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SimulationService {
    Semaphore sem = new Semaphore(1, true);

    public static void postSimEvent(String type, int id, String action, long ts) {

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://node-red-591094411846.europe-west1.run.app/api/sim/events";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "API_KEY_DEL_ADMIN");

        SimEvent event = new SimEvent(
                type, // o DOCTOR / MACHINE
                id,
                action,
                ts);

        HttpEntity<SimEvent> request = new HttpEntity<>(event, headers);

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Void.class);
    }

    public static void sendFinalTime(long timeNanoSeconds) {
        try {

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://node-red-591094411846.europe-west1.run.app/api/sim/final";
            restTemplate.postForEntity(
                    url,
                    timeNanoSeconds,
                    Void.class);
        } catch (Exception e) {
            // Importante: no romper la simulación si el backend no está activo
            System.err.println("⚠️ No se pudo enviar el tiempo final: " + e.getMessage());
        }
    }

    public void postList(String type, int id, String action, long ts) throws InterruptedException {
        sem.acquire();
        postSimEvent(type,id,action,ts);
        Thread.sleep(100);
        sem.release();
    }

}
