package edu.mondragon.os.pbl.hospital;

public class Appoiment{
    private int nextAppointmentNumber;

    public Appoiment() {
        nextAppointmentNumber = 1;
    }

    public synchronized int getAppoiment(int patientId) {
        int givenNumber = nextAppointmentNumber;

        nextAppointmentNumber++;
        System.out.println("🧑 " + patientId + " has taken ticket #" + givenNumber);
        return givenNumber;
    }
}
