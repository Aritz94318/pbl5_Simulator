package edu.mondragon.os.pbl.hospital;

import java.util.concurrent.BlockingQueue;

import edu.mondragon.os.pbl.hospital.mailbox.Message;

public class Diagnostic {

    private  String patientName;
    private  String positive;
    private  BlockingQueue<Message> replyTo;

    public Diagnostic(String patientName, String positive, BlockingQueue<Message> replyTo) {
        this.patientName = patientName;
        this.positive = positive;
        this.replyTo = replyTo;
    }
    public void setReplyTo(BlockingQueue<Message> replyTo) {
        this.replyTo = replyTo;
    }
    public void setPositive(String positive) {
        this.positive = positive;
    }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }
    
    public String getPatientName() {
        return patientName;
    }

    public String getDiagnosis() {
        return positive;
    }

    public BlockingQueue<Message> getReplyTo() {
        return replyTo;
    }
    
    @Override
    public String toString() {
        return "Diagnostic{" +
                "patientName='" + patientName + '\'' +
                ", positive=" + positive +
                '}';
    }
}
