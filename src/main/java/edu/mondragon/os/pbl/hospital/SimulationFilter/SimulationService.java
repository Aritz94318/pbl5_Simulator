package edu.mondragon.os.pbl.hospital.SimulationFilter;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SimulationService {

    public static void postSimEvent(String type, int id, String action, long ts) {

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/sim/events";

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

}
