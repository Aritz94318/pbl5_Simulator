package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.print.DocFlavor.READER;

import edu.mondragon.os.pbl.hospital.mailbox.AppointmentMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.mailbox.WaitingRoomMessage;

public class Machine extends Thread {

    private int arrivalTime = 0;
    private int id;
    private Message reply;

    private BlockingQueue<HospitalMessage> hospital;
    private BlockingQueue<WaitingRoomMessage> waitinroom;
    private final BlockingQueue<Message> myMailbox;

    public Machine(int id, BlockingQueue<HospitalMessage> hospital, BlockingQueue<WaitingRoomMessage> waitinroom) {
        super("Machine " + id);
        this.hospital = hospital;
        this.waitinroom = waitinroom;
        this.myMailbox = new LinkedBlockingQueue<>();
        arrivalTime += 500 * id;
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


        
        System.out.println("hola");
        waitinroom.put(new WaitingRoomMessage("NEXT_PATIENT", "" + id, myMailbox));
        reply = myMailbox.take();
        Thread.sleep(200);

        /*
        hospital.put(new HospitalMessage("WAITING_PATIENT", "" + id, myMailbox));
        reply = myMailbox.take();

        System.out.println("🎛️  Machine:" + machineId + " Start making mamograph of patient:" + reply.content);

        hospital.put(new HospitalMessage("COMPLETED_PATIENT", "" + id, myMailbox));

        reply = myMailbox.take();*/

    }
}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
