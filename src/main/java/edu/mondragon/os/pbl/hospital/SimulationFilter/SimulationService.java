package edu.mondragon.os.pbl.hospital.simulationfilter;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean backendAvailable = true;

 private static final String API_URL = "https://node-red-591094411846.europe-west1.run.app/api/sim/events";
    private static final String FINAL_URL = "https://node-red-591094411846.europe-west1.run.app/api/sim/final";

    private static final String API_KEY = "API_KEY_DEL_ADMIN";

    private final RestTemplate restTemplate;
    Semaphore sem = new Semaphore(1, true);

    public SimulationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);

        this.restTemplate = new RestTemplate(factory);
    }


    public void postSimEvent(String type, int id, String action, long timestamp) {
        if (!backendAvailable) {
            return;
        }

        executor.submit(() -> {
            try {
                HttpHeaders headers = createHeaders();
                SimEvent event = new SimEvent(type, id, action, timestamp);
                HttpEntity<SimEvent> request = new HttpEntity<>(event, headers);

                restTemplate.exchange(API_URL, HttpMethod.POST, request, Void.class);

                backendAvailable = true;

            } catch (ResourceAccessException e) {
                backendAvailable = false;
                log.warn("⚠️ Node-RED not responding (timeout/connection): {}", e.getMessage());
            } catch (Exception e) {
                log.warn("⚠️ couldn´t send event to Node-RED: {}", e.getMessage());
            }
        });
    }

    public void sendFinalTime(long timeNanoSeconds) {
        executor.submit(() -> {
            try {
                HttpHeaders headers = createHeaders();

                SimTime simTime = new SimTime(timeNanoSeconds);
                HttpEntity<SimTime> request = new HttpEntity<>(simTime, headers);

                restTemplate.exchange(FINAL_URL, HttpMethod.POST, request, Void.class);

            } catch (Exception e) {
                log.warn("⚠️ couldn´t send final time: {}", e.getMessage());
            }
        });
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }

    public void postList(String type, int id, String action, long ts) throws InterruptedException {
        sem.acquire();
        postSimEvent(type, id, action, ts);
        Thread.sleep(100);
        sem.release();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
