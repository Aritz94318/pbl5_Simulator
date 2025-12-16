package edu.mondragon.os.pbl.hospital;

/**
 * Hospital
 *
 */
public class App {
    final static int N = 5;
    final static int NUMPATIENTS = 6;
    final static int NUMDOCTORS = 5;
    final static int NUMMACHINES = 3;

    private Hospital hospital;
    private Appoiment appoiment;
    private WaitingRoom waitingRoom;
    private Doctor doctors[];
    private Patient patients[];
    private Machine machines[];

    public App() {
        appoiment = new Appoiment();
        waitingRoom = new WaitingRoom(hospital);
        hospital = new Hospital(waitingRoom);

        doctors = new Doctor[NUMDOCTORS];
        patients = new Patient[NUMPATIENTS];
        machines = new Machine[NUMMACHINES];

        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(i+1, hospital, appoiment,waitingRoom);
        }
        for (int i = 0; i < NUMDOCTORS; i++) {
            doctors[i] = new Doctor(i, hospital);
        }
        for (int i = 0; i < NUMMACHINES; i++) {
            machines[i] = new Machine(i, hospital);
        }
    }

    public void startThreads() {

        for (Patient patient : patients) {
            patient.start();
        }
        /* 
         for (Doctor doctor : doctors) {
         doctor.start();
         }*/
          for (Machine machine : machines) {
          machine.start();
          }
         
    }

    public void waitEndOfThreads() {
        try {
            for (int i = 0; i < NUMPATIENTS; i++) {
                patients[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        App app = new App();

        app.startThreads();
        app.waitEndOfThreads();
    }
}
