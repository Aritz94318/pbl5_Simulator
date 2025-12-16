package edu.mondragon.os.pbl.hospital;

public class Machine extends Thread {

    private int arrivalTime = 0;
    private Hospital hospital;
    private int id;

    public Machine(int id, Hospital hospital) {
        super("Machine " + id);
        this.hospital = hospital;
        arrivalTime += 500 * id;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
        }
        while (!Thread.interrupted()) {
            try {
                hospital.beMachine(id);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

}

// Machine durmiendo
// Cuando hay paciente esperando -> machine despierta
// Paciente pasa->Machine haciendo mamographia
// Acaba Mamographia
// Esperoa a siguiente paciente
// Si no viene se duerme
