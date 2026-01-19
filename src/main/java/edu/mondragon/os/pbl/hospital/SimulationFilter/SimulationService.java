package edu.mondragon.os.pbl.hospital.SimulationFilter;

import java.util.concurrent.Semaphore;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class SimulationService {
    
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    private static volatile boolean backendAvailable = true;
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://node-red-591094411846.europe-west1.run.app/api/sim/events";
    private static final String FINAL_URL = "https://node-red-591094411846.europe-west1.run.app/api/sim/final";

    Semaphore sem = new Semaphore(1, true);

    public static void postSimEvent(String type, int id, String action, long ts) {
        if (!backendAvailable) {
            return; // Silently skip if backend is down
        }
        
        asyncExecutor.submit(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-KEY", "API_KEY_DEL_ADMIN");

                SimEvent event = new SimEvent(type, id, action, ts);
                HttpEntity<SimEvent> request = new HttpEntity<>(event, headers);

                restTemplate.exchange(API_URL, HttpMethod.POST, request, Void.class);
                
            } catch (ResourceAccessException e) {
                System.err.println("⚠️ Node-RED no responde (posible timeout): " + e.getMessage());
                backendAvailable = false;
            } catch (Exception e) {
                System.err.println("⚠️ Error enviando evento a Node-RED: " + e.getMessage());
                // No imprimir stack trace completo para no saturar la consola
            }
        });
    }

    public static void sendFinalTime(long timeNanoSeconds) {
        asyncExecutor.submit(() -> {
            try {
                restTemplate.postForEntity(FINAL_URL, timeNanoSeconds, Void.class);
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo enviar el tiempo final: " + e.getMessage());
            }
        });
    }
    
    public static void shutdown() {
        asyncExecutor.shutdown();
    }

    public void postList(String type, int id, String action, long ts) throws InterruptedException {
        sem.acquire();
        postSimEvent(type,id,action,ts);
        Thread.sleep(100);
        sem.release();
    }

}
