package edu.mondragon.os.pbl.hospital;

public class HospitalSystem {
    private Hospital hospital;
    private Diagnostic diagnostic;
    private Appoiment appoiment;
    
    public HospitalSystem(Appoiment appoiment, Hospital hospital, Diagnostic diagnostic) {
        
        this.hospital = hospital;
        this.diagnostic=diagnostic;
        this.appoiment=appoiment;
    }


}
//algo para que lleve los diagnosticos de la clase
// -->Hospital--->Diagnosis