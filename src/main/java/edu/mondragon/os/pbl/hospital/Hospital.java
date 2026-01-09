package edu.mondragon.os.pbl.hospital;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Hospital implements Runnable {

    private final Map<Integer, BlockingQueue<Message>> patient = new HashMap<>();
    private final Map<Integer, BlockingQueue<Message>> freeMachine = new HashMap<>();
    private final Map<Integer, BlockingQueue<Message>> ocupiedMachine = new HashMap<>();
    private final BlockingQueue<HospitalMessage> mailbox;
    private final BlockingQueue<WaitingRoomMessage> waitingmailbox;
    private BlockingQueue<Message> mb;
    // private Diagnostic diagnotic;

    public Hospital(BlockingQueue<HospitalMessage> mailbox, BlockingQueue<WaitingRoomMessage> waitingmailbox) {
        this.mailbox = mailbox;
        this.waitingmailbox = waitingmailbox;
    }

    @Override
    public void run() {
        try {
            while (true) {
                HospitalMessage msg = mailbox.take(); // espera solicitudes
                int data = Integer.parseInt(msg.content);
                if (msg.type.equals("STOP")) {
                    break;
                }
                switch (msg.type) {
                    case "WAITING":
                        int where = setPatient(data);
                        patient.put(where, msg.replyTo);
                        mb = freeMachine.remove(where);
                        if (mb == null) {
                            break;
                        }
                        ocupiedMachine.put(data, mb);
                        mb.put(new Message("null", null, null));

                        break;
                    case "IS_READY":
                        mb = ocupiedMachine.get(data);
                        mb.put(new Message(null, "" + data, null));
                        break;
                    case "COMPLETED_PATIENT":
                        mb = patient.get(Integer.parseInt(msg.content));
                        mb.put(new Message(null, null, null));
                        break;
                    case "PATIENT_GONE":
                        mb = ocupiedMachine.get(data);
                        mb.put(new Message(null, null, null));
                        patient.remove(Integer.parseInt(msg.content));

                        break;
                    case "FREE_MACHINE":
                        ocupiedMachine.remove(Integer.parseInt(msg.content));
                        freeMachine.put(Integer.parseInt(msg.content), msg.replyTo);
                        waitingmailbox.put(new WaitingRoomMessage("NEXT_PATIENT", "", null));
                        break;
                }

            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int setPatient(int patientId) {
        int id = -1;

        // 1) Elegir la primera máquina libre en orden 0,1,2
        if (freeMachine.containsKey(0)) {
            id = 0;
        } else if (freeMachine.containsKey(1)) {
            id = 1;
        } else if (freeMachine.containsKey(2)) {
            id = 2;
        }
        return id; // o lanza excepción si prefieres
    }

}
