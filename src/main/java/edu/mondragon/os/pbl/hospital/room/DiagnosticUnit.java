package edu.mondragon.os.pbl.hospital.room;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.HospitalMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.values.Diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DiagnosticUnit implements Runnable {

    private final BlockingQueue<DiagnosticUnitMessage> mailbox;
    private static final double POSITIVE_PROBABILITY = 0.3;
    private static final double CHANGE_PROBABILITY = 0.34;
    private static final double INCONCLUSIVE_PROBABILITY = 0.4;

    private static final String MALIGNO = "MALIGNO";
    private static final String VENIGNO = "VENIGNO";

    private final ArrayList<Diagnostic> positiveDiagnostics = new ArrayList<>();
    private final ArrayList<Diagnostic> negativeDiagnostics = new ArrayList<>();
    private final Map<Integer, Diagnostic> doctorsDiagnostics = new HashMap<>();
    private final ArrayList<DiagnosticUnitMessage> backlog = new ArrayList<>();

    public DiagnosticUnit(BlockingQueue<DiagnosticUnitMessage> mailbox) {
        this.mailbox = mailbox;
    }

    @Override
    public void run() {
        try {
            while (true) {
                DiagnosticUnitMessage msg = mailbox.take(); // espera solicitudes

                if (msg.type.equals("STOP")) {
                    break;
                }

                if (msg.type.equals("PASS MAMOGRAPH IN AI")) {//

                    double randomValue = Math.random();

                    boolean isPositive = randomValue < POSITIVE_PROBABILITY;

                    String result = isPositive ? MALIGNO : VENIGNO;
                    Diagnostic diagnostic = new Diagnostic(msg.content, result, msg.replyTo);

                    if (isPositive) {
                        positiveDiagnostics.add(diagnostic);
                    } else {
                        negativeDiagnostics.add(diagnostic);
                    }
                    msg.replyTo.put(new Message("AI_RESULT", result, null));
                    mailbox.put(backlog.remove(0));

                }

                if (msg.type.equals("Get Diagnosis")) {
                    boolean wantPositive = Math.random() < POSITIVE_PROBABILITY;

                    ArrayList<Diagnostic> list = wantPositive ? positiveDiagnostics : negativeDiagnostics;

                    if (!list.isEmpty()) {
                        Diagnostic take = list.remove(0); // SOLO UNA VEZ
                        doctorsDiagnostics.put(Integer.parseInt(msg.content), take);
                        msg.replyTo.put(new Message("CASE_ASSIGNED", "OK", null));
                    } else {
                        backlog.add(msg);
                    }
                }

                if (msg.type.equals("FINAL DIAGNOSIS")) {
                    Diagnostic diagnosis = doctorsDiagnostics.remove(Integer.parseInt(msg.content));
                    if (diagnosis != null) {
                        double randomValue = Math.random();
                        double inconclusiveValue = Math.random();

                        boolean shouldChange = randomValue < CHANGE_PROBABILITY;
                        boolean isInconclusive = inconclusiveValue < INCONCLUSIVE_PROBABILITY;

                        if (isInconclusive) {
                            diagnosis.setPositive("INCONCLUSIVE");
                        } else if (shouldChange) {
                            if (diagnosis.getDiagnosis().equals(MALIGNO)) {
                                diagnosis.setPositive(VENIGNO);
                            } else {
                                diagnosis.setPositive(MALIGNO);
                            }
                        }
                        // else: no cambia, se queda como estaba

                        BlockingQueue<Message> mb = diagnosis.getReplyTo();
                        mb.put(new Message("FINAL_DIAGNOSIS", "" + diagnosis.getDiagnosis(), null));
                    }

                }

            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
