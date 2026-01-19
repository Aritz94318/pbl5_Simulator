package edu.mondragon.os.pbl.hospital.room;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class WaitingRoom implements Runnable {
    private int currentTurn;
    private int freeMachines;

    private final BlockingQueue<WaitingRoomMessage> mailbox;
    private long t0;

    // ticket -> WAIT msg (contiene replyTo del paciente)
    private final Map<Integer, WaitingRoomMessage> backlogByPatient = new HashMap<>();

    public WaitingRoom(BlockingQueue<WaitingRoomMessage> mailbox) {
        this.mailbox = mailbox;
        this.currentTurn = 1; // si tus tickets empiezan en 0, pon 0
        this.freeMachines = 0;
    }

    private void log(String emoji, String phase, String msg) {
        long ms = System.currentTimeMillis() - t0;
        System.out.printf("[%6dms] %s [%s] %-14s %s%n",
                ms, emoji, "Waiting Room", phase, msg);
    }

    @Override
    public void run() {
        try {
            t0 = System.currentTimeMillis();

            while (!Thread.currentThread().isInterrupted()) {
                WaitingRoomMessage msg = mailbox.take();

                if ("STOP".equals(msg.type))
                    break;

                switch (msg.type) {

                    case "WAIT": {
                        int ticket = Integer.parseInt(msg.content);
                        backlogByPatient.put(ticket, msg);
                        log("🧍", "WAIT", "Patient arrives with ticket #" + ticket);

                        releaseIfPossible();
                        break;

                    }

                    case "NEXT_PATIENT": {
                        freeMachines++;
                        log("🟢", "MACHINE", "Machine is free. freeMachines=" + freeMachines);

                        releaseIfPossible();
                        break;
                    }

                    default:
                        log("❓", "UNKNOWN", msg.type + " content=" + msg.content);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseIfPossible() throws InterruptedException {
        while (freeMachines > 0) {
            WaitingRoomMessage next = backlogByPatient.remove(currentTurn);
            if (next == null)
                return; // aún no llegó el paciente de este turno

            // Ahora SÍ es su turno
            next.replyTo.put(new Message("YOUR_TURN", "" + currentTurn, null));
            log("📢", "DISPLAY", "🔊 TURN #" + currentTurn + " → please proceed");

            currentTurn++;
            freeMachines--; // 👈 esto faltaba sí o sí
        }
    }
}
