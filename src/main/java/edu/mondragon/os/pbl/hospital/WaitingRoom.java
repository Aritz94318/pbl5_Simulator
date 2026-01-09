package edu.mondragon.os.pbl.hospital;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class WaitingRoom implements Runnable {
    private int currentTurn;
    private int freeMachines;

    private final Map<Integer, BlockingQueue<Message>> waiting = new HashMap<>();
    private final BlockingQueue<WaitingRoomMessage> mailbox;

    public WaitingRoom(BlockingQueue<WaitingRoomMessage> mailbox) {
        this.mailbox = mailbox;
        currentTurn = 0;
        freeMachines = 0;

    }

    @Override
    public void run() {
        try {
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

                    System.out.println("📢 Display calling turn #" + currentTurn);
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