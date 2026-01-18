package edu.mondragon.os.pbl.hospital.Rooms;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Hospital implements Runnable {

    private int numMachines;

    static class PatientState {
        Integer machineId;
        boolean changing;
        boolean gone; // null si no tiene
    }

    static class MachineState {
        Integer patientId; // null si no tiene
        boolean mamographiDone;
        boolean isFree;
    }

    private final Map<Integer, MachineState> machines = new HashMap<>();
    private final Map<Integer, PatientState> patiens = new HashMap<>();

    private final BlockingQueue<HospitalMessage> mailbox;
    private final Map<String, HospitalMessage> backlogByPhase = new HashMap<>();

    private BlockingQueue<Message> mb;
    private MachineState ms;
    private PatientState ps;
    private int machineId;
    private int patientId;

    // private Diagnostic diagnotic;

    public Hospital(BlockingQueue<HospitalMessage> mailbox, int numMachines) {
        this.mailbox = mailbox;
        this.numMachines = numMachines;
    }

    @Override
    public void run() {
        try {
            while (true) {
                HospitalMessage msg = mailbox.take(); // espera solicitudes
                switch (msg.type) {
                    case "FREE_MACHINE":
                        System.out.println("Machine:"+msg.content+"Crea un guardado AAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        ms = machines.computeIfAbsent(Integer.parseInt(msg.content),
                                id -> new MachineState());
                        ms.mamographiDone = false;
                        ms.patientId = -1;
                        ms.isFree = true;
                        break;
                    case "WAITING_PATIENT":
                        ms = machines.get(Integer.parseInt(msg.content));

                        if (ms == null || ms.patientId == -1) {
                            backlogByPhase.put("WA" + msg.content, msg);
                            break; // primera máquina libre
                        }
                        msg.replyTo.put(new Message("PATIENT_ASSIGNED", "" + ms.patientId, null));
                        break;
                    case "ANY_FREE_MACHINE":
                        int where = setPatient();
                        if (where == -1) {
                            backlogByPhase.put("AFM" + msg.content, msg);
                            break;
                        }
                        ps = patiens.computeIfAbsent(Integer.parseInt(msg.content),
                                id -> new PatientState());
                        ps.machineId = where;
                        ps.changing = false;
                        ps.gone = false;
                        ms = machines.get(ps.machineId);
                        ms.patientId = Integer.parseInt(msg.content);
                        ms.isFree = false;
                        pulledOfTheBacklog("WA" + ps.machineId);

                        msg.replyTo.put(new Message("PATIENT_ASSIGNED", "" + ps.machineId, null));
                        break;
                    case "PATIENT_IS_READY?":
                        machineId = Integer.parseInt(msg.content);
                        ms = machines.get(machineId);
                        ps = patiens.get(ms.patientId);
                        if (ps.changing) { // si está cambiándose, NO está listo
                            backlogByPhase.put("PIR" + machineId, msg);
                            break;
                        }

                        msg.replyTo.put(new Message("PATIENT_IS_READY", "" + ms.patientId, null));
                        break;

                    case "PREPARING_FOR_MAMOGRAFY":
                        patientId = Integer.parseInt(msg.content);
                        ps = patiens.get(patientId);
                        pulledOfTheBacklog("PIR" + ps.machineId);

                        msg.replyTo.put(new Message("PATIENT_IS_READY", "" + ps.machineId, null));
                        break;
                    case "MAMOGRAPHY_HAS_FINISH":
                        machineId = Integer.parseInt(msg.content);
                        ms = machines.get(machineId);
                        ms.mamographiDone = true;
                        pulledOfTheBacklog("PFL" + ms.patientId);
                        break;

                    case "PREPARING_FOR_LEAVING":
                        patientId = Integer.parseInt(msg.content);
                        ps = patiens.get(patientId);
                        ms = machines.get(ps.machineId);
                        if (!ms.mamographiDone) {
                            backlogByPhase.put("PFL" + patientId, msg);
                            break;
                        }
                        ps.gone = true;
                        pulledOfTheBacklog("PHG" + ps.machineId);
                        msg.replyTo.put(new Message("YOU_CAN_LEAVE", "" + ps.machineId, null));
                        break;
                    case "PATIENT_HAS_GO?":
                        machineId = Integer.parseInt(msg.content);
                        ms = machines.get(machineId);
                        ps = patiens.get(ms.patientId);

                        if (!ps.gone) {
                            backlogByPhase.put("PHG" + machineId, msg);
                            break; // <-- CLAVE
                        }

                        int finishedPatientId = ms.patientId;

                        // limpiar estados
                        patiens.remove(finishedPatientId);
                        ms.patientId = -1;
                        ms.isFree = true;
                        ms.mamographiDone = false;

                        msg.replyTo.put(new Message("PROCESS_FINISHED", "" + finishedPatientId, null));
                        break;

                    default:
                        throw new IllegalStateException(
                                "Esperaba APPOINTMENT_GRANTED y llegó: " + msg.type);
                }

            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int setPatient() {
        for (int id = 0; id < numMachines; id++) {

            MachineState ms = machines.get(id);
            if (ms != null && ms.isFree) {
                return id; // primera máquina libre
            }
        }
       
        return -1; // ninguna libre
    }

    public void pulledOfTheBacklog(String id) throws InterruptedException {
        HospitalMessage m = backlogByPhase.remove(id);
        if (m != null) {
            mailbox.put(m);
        }
    }

}
