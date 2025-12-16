package edu.mondragon.os.pbl.hospital;

public class Doctor extends Thread {

    private int arrivalTime = 0;
    private Hospital hospital;

    public Doctor(int id, Hospital hospital) {
        super("Doctor " + id);
        this.hospital = hospital;
        arrivalTime += 500 * id;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(arrivalTime);
            hospital.enterHospital(getName());
        } catch (InterruptedException e) {
        }
    }
}
//Duerme
//Diagnosis en espera(x)--> Doctor despierta
//Escoge diagnosis por prioridad->Se comunica a paciente [estado de mamografía: siendo analizado]
//Analiza la mamografia
//Paciente recive aviso estado de mamografía: analizado