package com.PBL.Simulation.simulationfilter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MediaType;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import edu.mondragon.os.pbl.hospital.simulationfilter.SimEvent;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimTime;
import edu.mondragon.os.pbl.hospital.simulationfilter.SimulationService;

class SimulationServiceTest {

    private RestTemplate restTemplate;
    private ExecutorService executor;
    private SimulationService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        executor = Executors.newSingleThreadExecutor();
        service = new SimulationService(restTemplate, executor);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    void postSimEvent_sendsHttpPost_withHeadersAndBody() throws Exception {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.postSimEvent("PATIENT", 7, "ARRIVAL", 123L);

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<SimEvent>> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq("https://node-red-591094411846.europe-west1.run.app/api/sim/events"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(Void.class)
        );

        HttpEntity<SimEvent> sent = captor.getValue();
        assert sent != null;

        // headers
        HttpHeaders h = sent.getHeaders();
        Assertions.assertEquals(MediaType.APPLICATION_JSON, h.getContentType());
        Assertions.assertEquals("API_KEY_DEL_ADMIN", h.getFirst("X-API-KEY"));

        // body
        SimEvent body = sent.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("PATIENT", body.getActor());
        Assertions.assertEquals(7, body.getActorId());
        Assertions.assertEquals("ARRIVAL", body.getText());
        Assertions.assertEquals(123L, body.getTs());
    }

    @Test
    void sendFinalTime_postsToFinalUrl() throws Exception {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.sendFinalTime(75L * 1_000_000_000L); // 75 min sim

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<SimTime>> captor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq("https://node-red-591094411846.europe-west1.run.app/api/sim/final"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(Void.class)
        );

        SimTime body = captor.getValue().getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(1, body.getHours());
        Assertions.assertEquals(15, body.getMinutes());
        Assertions.assertEquals(0, body.getSeconds());
    }
}
