package edu.mondragon.os.pbl.hospital;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Hospital implements Runnable {

    private final Map<Integer, BlockingQueue<Message>> machineToPatient = new HashMap<>();
    private final Map<Integer, BlockingQueue<Message>> freeMachine = new HashMap<>();
    private final Map<Integer, BlockingQueue<Message>> patientToMachine = new HashMap<>();
    private final Map<Integer, Integer> machine = new HashMap<>();

    private int numMachines;

    private final BlockingQueue<HospitalMessage> mailbox;
    private final BlockingQueue<WaitingRoomMessage> waitingmailbox;
    private BlockingQueue<Message> mb;
    // private Diagnostic diagnotic;

    public Hospital(BlockingQueue<HospitalMessage> mailbox, BlockingQueue<WaitingRoomMessage> waitingmailbox,
            int numMachines) {
        this.mailbox = mailbox;
        this.waitingmailbox = waitingmailbox;
        this.numMachines = numMachines;
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
                        mb = freeMachine.remove(where);

                        if ((mb == null) && (where == -1)) {
                            mailbox.put(msg);
                            Thread.sleep(1);
                            break;
                        }
                        machine.put(data, where);
                        machineToPatient.put(where, msg.replyTo);

                        patientToMachine.put(data, mb);
                        mb.put(new Message("null", null, null));
                        msg.replyTo.put(new Message("Where you go", "" + where, null));
                        break;
                    case "IS_READY":
                        mb = patientToMachine.get(data);
                        if (mb == null) {
                            mailbox.put(msg);
                            Thread.sleep(1);
                            break;
                        }
                        mb.put(new Message(null, "" + data, null));
                        break;
                    case "COMPLETED_PATIENT":
                        mb = machineToPatient.get(Integer.parseInt(msg.content));
                        if (mb == null) {
                            mailbox.put(msg);
                            Thread.sleep(1);
                            break;
                        }
                        mb.put(new Message(null, null, null));
                        break;
                    case "PATIENT_GONE":
                        mb = patientToMachine.get(data);
                        if (mb == null) {
                            mailbox.put(msg);
                            Thread.sleep(1);
                            break;
                        }

                        mb.put(new Message(null, null, null));
                        patientToMachine.remove(Integer.parseInt(msg.content));
                        Integer machineId = machine.remove(data);
                        if (machineId != null) {
                            machineToPatient.remove(machineId);
                        }

                        break;
                    case "FREE_MACHINE":
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
        for (int id = 0; id < numMachines; id++) {
            if (freeMachine.containsKey(id)) {
                return id; // primera máquina libre
            }
        }
        return -1; // ninguna libre
    }

}
