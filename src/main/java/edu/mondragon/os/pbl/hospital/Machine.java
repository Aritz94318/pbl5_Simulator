package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Machine extends Thread {

    private int id;
    private Message reply;
    private BlockingQueue<HospitalMessage> hospital;
    private final BlockingQueue<Message> myMailbox;

    public Machine(int id, BlockingQueue<HospitalMessage> hospital) {
        super("Machine " + id);
        this.hospital = hospital;
        this.myMailbox = new LinkedBlockingQueue<>();
        this.id = id;
    }

    @Override
    public void run() {

        while (!Thread.interrupted()) {
            try {
                beMachine(id);
                Thread.sleep(200);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    public void beMachine(int machineId) throws InterruptedException {

        hospital.put(new HospitalMessage("FREE_MACHINE", "" + id, myMailbox));
        reply = myMailbox.take();

        reply = myMailbox.take();

        System.out.println("🎛️  Machine:" + machineId + " Start making mamograph of patient:" + reply.content);
        Thread.sleep(150);
        hospital.put(new HospitalMessage("COMPLETED_PATIENT", "" + id, myMailbox));
    
        //reply = myMailbox.take();
        
        
    }
}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
