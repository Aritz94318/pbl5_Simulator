package com.PBL.Simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import edu.mondragon.os.pbl.hospital.App;

class AppTest {

    // Ajusta si tu simulación tarda más por sleeps internos
    @Test
    @Timeout(15) // segundos
    void simulation_should_finish_and_print_elapsed_seconds() throws Exception {
        // Capturar System.out para comprobar que imprime el tiempo
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            int NUMPATIENTS = 3;
            int NUMDOCTORS = 1;
            int NUMMACHINES = 1;

            App app = new App(NUMPATIENTS, NUMDOCTORS, NUMMACHINES);

            // Arranca la simulación
            app.startThreads();

            // Espera a que acabe (esto internamente hace join() a pacientes y luego interrumpe el resto)
            app.waitEndOfThreads(NUMPATIENTS, NUMDOCTORS, NUMMACHINES);

            // Verificación 1: todos los pacientes han terminado
            Thread[] patients = (Thread[]) getPrivateField(app, "patients");
            for (Thread p : patients) {
                assertNotNull(p);
                assertFalse(p.isAlive(), "Un paciente sigue vivo: " + p.getName());
            }

            // Verificación 2: hilos "de servicio" están interrumpidos (o al menos ya no vivos)
            Thread[] machines = (Thread[]) getPrivateField(app, "machines");
            for (Thread m : machines) {
                assertNotNull(m);
                // Puede que siga vivo si su bucle ignora el interrupted flag,
                // pero al menos debería estar marcado como interrupted tras tu código.
                assertTrue(m.isInterrupted() || !m.isAlive(),
                        "Machine no quedó interrumpida ni terminó: " + m.getName());
            }

            Thread[] doctors = (Thread[]) getPrivateField(app, "doctors");
            for (Thread d : doctors) {
                assertNotNull(d);
                assertTrue(d.isInterrupted() || !d.isAlive(),
                        "Doctor no quedó interrumpido ni terminó: " + d.getName());
            }

            Thread appoiment = (Thread) getPrivateField(app, "appoiment");
            assertNotNull(appoiment);
            assertTrue(appoiment.isInterrupted() || !appoiment.isAlive(),
                    "Appointment thread no quedó interrumpido ni terminó");

            Thread waitingRoom = (Thread) getPrivateField(app, "waitingRoom");
            assertNotNull(waitingRoom);
            assertTrue(waitingRoom.isInterrupted() || !waitingRoom.isAlive(),
                    "WaitingRoom thread no quedó interrumpido ni terminó");

            Thread hospital = (Thread) getPrivateField(app, "hospital");
            assertNotNull(hospital);
            assertTrue(hospital.isInterrupted() || !hospital.isAlive(),
                    "Hospital thread no quedó interrumpido ni terminó");

            Thread diagnosticUnit = (Thread) getPrivateField(app, "diagnosticUnit");
            assertNotNull(diagnosticUnit);
            assertTrue(diagnosticUnit.isInterrupted() || !diagnosticUnit.isAlive(),
                    "DiagnosticUnit thread no quedó interrumpido ni terminó");

            // Verificación 3: imprime un número de segundos al final
            String output = outContent.toString().trim();
            assertFalse(output.isEmpty(), "No se imprimió nada por consola");

            // Tu método imprime "seconds" con System.out.println(seconds);
            // Así que buscamos la última línea y comprobamos que sea parseable como double.
            String[] lines = output.split("\\R");
            String lastLine = lines[lines.length - 1].trim();
            double seconds = Double.parseDouble(lastLine);
            assertTrue(seconds >= 0.0, "Tiempo negativo (?) -> " + seconds);

        } finally {
            System.setOut(originalOut);
        }
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }
}
