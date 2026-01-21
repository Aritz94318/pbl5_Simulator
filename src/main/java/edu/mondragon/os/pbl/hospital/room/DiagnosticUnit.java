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

    private static final String MALIGNO = "MALIGNANT";
    private static final String BENIGNO = "BENIGN";

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

                switch (msg.type) {

                    case "STOP":
                        return; // termina el hilo limpiamente

                    case "PASS MAMOGRAPH IN AI": {
                        double randomValue = Math.random();
                        boolean isPositive = randomValue < POSITIVE_PROBABILITY;

                        String result = isPositive ? MALIGNO : BENIGNO;
                        Diagnostic diagnostic = new Diagnostic(msg.content, result, msg.replyTo);

                        if (isPositive) {
                            positiveDiagnostics.add(diagnostic);
                        } else {
                            negativeDiagnostics.add(diagnostic);
                        }

                        // Respuesta inmediata al actor que envió la mamografía
                        msg.replyTo.put(new Message("AI_RESULT", result, null));

                        // Liberar backlog si existe
                        if (!backlog.isEmpty()) {
                            DiagnosticUnitMessage md = backlog.remove(0);
                            mailbox.put(md);
                        }
                        break;
                    }

                    case "GET_DIAGNOSIS": {
                        boolean wantPositive = Math.random() < POSITIVE_PROBABILITY;

                        ArrayList<Diagnostic> primary = wantPositive ? positiveDiagnostics : negativeDiagnostics;
                        ArrayList<Diagnostic> secondary = wantPositive ? negativeDiagnostics : positiveDiagnostics;

                        ArrayList<Diagnostic> chosen = null;

                        if (primary != null && !primary.isEmpty()) {
                            chosen = primary;
                        } else if (secondary != null && !secondary.isEmpty()) {
                            chosen = secondary;
                        }

                        if (chosen != null) {
                            Diagnostic take = chosen.remove(0);
                            doctorsDiagnostics.put(Integer.parseInt(msg.content), take);
                            msg.replyTo.put(new Message("CASE_ASSIGNED", "OK", null));
                        } else {
                            backlog.add(msg);
                        }
                        break;
                    }

                    case "FINAL_DIAGNOSIS": {
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
                                    diagnosis.setPositive(BENIGNO);
                                } else {
                                    diagnosis.setPositive(MALIGNO);
                                }
                            }
                            BlockingQueue<Message> mb = diagnosis.getReplyTo();
                            mb.put(new Message("FINAL_DIAGNOSIS", diagnosis.getDiagnosis(), null));
                        }
                        break;
                    }

                    default:
                        System.out.println("⚠️ Unknown message type: " + msg.type);
                }

            }
        } catch (

        InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
