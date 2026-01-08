package edu.mondragon.os.pbl.hospital;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class WaitingRoom implements Runnable {
    private int currentTurn;

    private final Map<Integer, BlockingQueue<Message>> waiting = new HashMap<>();
    private final BlockingQueue<WaitingRoomMessage> mailbox;
    private int waitingMachines;
    private boolean list;

    public WaitingRoom(BlockingQueue<WaitingRoomMessage> mailbox) {
        this.mailbox = mailbox;
        list = false;
        currentTurn = 0;
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
                        System.out.println("HOYE");
                        waitingMachines++;
                        System.out.println("📢 Display calling turn #" + currentTurn);
                }

                releaseIfPossible();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseIfPossible() throws InterruptedException {
        BlockingQueue<Message> mb = waiting.remove(currentTurn);
        if (mb != null) {
            mb.put(new Message("YOUR_TURN", "" + currentTurn, null));
            waitingMachines--;
   
        }
    }
}