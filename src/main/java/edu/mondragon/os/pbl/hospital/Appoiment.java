package edu.mondragon.os.pbl.hospital;

public class Appoiment {
    int appoiment_id;
    public Appoiment(){

    }
    public int getAppoiment(int id)
    {
        System.err.println("Patient "+id+" Take appoiment nº="+appoiment_id);
        appoiment_id++;
        return appoiment_id;
    }
}
