package edu.mondragon.os.pbl.hospital.SimulationFilter;


public class SimEvent {
    //private String runId;
    private String actor;   // PATIENT / DOCTOR / MACHINE
    private int actorId;
    private String text;
    private long ts;

    public SimEvent() {}

    public SimEvent(/*String runId,*/ String actor, int actorId, String text, long ts) {
      //  this.runId = runId;
        this.actor = actor;
        this.actorId = actorId;
        this.text = text;
        this.ts = ts;
    }

   // public String getRunId() { return runId; }
    //public void setRunId(String runId) { this.runId = runId; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }
}
