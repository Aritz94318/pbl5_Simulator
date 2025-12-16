package edu.mondragon.os.pbl.hospital;

/**
 * Hospital
 *
 */
public class App {

    final static int NUMPATIENTS = 50;
    final static int NUMDOCTORS = 5;
    final static int NUMMACHINES = 3;
    final static int MAXPDAYTPATIENTS = 20;

    private Hospital hospital;
    private Appoiment appoiment;
    private Doctor doctors[];
    private Patient patients[];
    private Machine machines[];

    public App() {
        hospital = new Hospital(MAXPDAYTPATIENTS);
        appoiment=new Appoiment();
        doctors = new Doctor[NUMDOCTORS];
        patients = new Patient[NUMPATIENTS];
        machines = new Machine[NUMMACHINES];

        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(i, hospital, appoiment);
        }
        for (int i = 0; i < NUMDOCTORS; i++) {
            doctors[i] = new Doctor(i, hospital);
        }
        for (int i = 0; i < NUMMACHINES; i++) {
            machines[i] = new Machine(i, hospital);
        }
    }

    public void startThreads() {

    }

    public void waitEndOfThreads() {
    }

    public static void main(String[] args) {

        App app = new App();

        app.startThreads();
        app.waitEndOfThreads();
    }
}
