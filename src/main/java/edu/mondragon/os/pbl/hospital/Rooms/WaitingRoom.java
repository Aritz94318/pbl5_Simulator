package edu.mondragon.os.pbl.hospital.Rooms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class WaitingRoom implements Runnable {
    private int currentTurn;
    private int freeMachines;

    private final Map<Integer, BlockingQueue<Message>> waiting = new HashMap<>();
    private final BlockingQueue<WaitingRoomMessage> mailbox;
    private long t0;

    public WaitingRoom(BlockingQueue<WaitingRoomMessage> mailbox) {

        this.mailbox = mailbox;
        currentTurn = 0;
        freeMachines = 0;

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
        
            while (true) {
                WaitingRoomMessage msg = mailbox.take(); // espera solicitudes

                if (msg.type.equals("STOP")) {
                    break;
                }

                if (msg.type.equals("WAIT")) {
                    waiting.put(Integer.parseInt(msg.content), msg.replyTo);
                    releaseIfPossible();
                }
                if (msg.type.equals("NEXT_PATIENT")) {
                    currentTurn++;
                    freeMachines++;

                    log("🔔", "DISPLAY", "🔊 TURNO #" + currentTurn + " → pase por favor");
                }
                if (freeMachines > 0) {
                    releaseIfPossible();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseIfPossible() throws InterruptedException {
        BlockingQueue<Message> mb = waiting.remove(currentTurn - freeMachines + 1);
        if (mb != null) {
            mb.put(new Message("YOUR_TURN", "" + currentTurn, null));
            freeMachines--;
        }
    }
}