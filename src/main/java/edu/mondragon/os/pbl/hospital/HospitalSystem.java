package edu.mondragon.os.pbl.hospital;

public class HospitalSystem {
    private Hospital hospital;
    private Diagnostic diagnostic;
    private Appointment appointment;
    
    public HospitalSystem(Appointment appointment, Hospital hospital, Diagnostic diagnostic) {
        
        this.hospital = hospital;
        this.diagnostic=diagnostic;
        this.appointment=appointment;
    }


}
//algo para que lleve los diagnosticos de la clase
// -->Hospital--->Diagnosis