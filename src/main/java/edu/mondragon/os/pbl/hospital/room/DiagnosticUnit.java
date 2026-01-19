package edu.mondragon.os.pbl.hospital.room;

import edu.mondragon.os.pbl.hospital.mailbox.DiagnosticUnitMessage;
import edu.mondragon.os.pbl.hospital.mailbox.Message;
import edu.mondragon.os.pbl.hospital.Values.Diagnostic;

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

                    // Responder al que envió la mamografía
                    msg.replyTo.put(new Message("", result, null));

                }

                if (msg.type.equals("Get Diagnosis")) {
                    double randomValue = Math.random();

                    boolean isPositive = randomValue < POSITIVE_PROBABILITY;

                    if (isPositive) {
                        if (!positiveDiagnostics.isEmpty()) {
                            Diagnostic take = positiveDiagnostics.remove(0);
                            doctorsDiagnostics.put(Integer.parseInt(msg.content), take);
                            msg.replyTo.put(new Message("null", "null", null));
                            Diagnostic diagnosis = doctorsDiagnostics.get(Integer.parseInt(msg.content));
                            BlockingQueue<Message> mb = diagnosis.getReplyTo();
                            mb.put(new Message("End", "" + diagnosis.getDiagnosis(), null));
                        } else {
                            // No hay positivos: reencolar la petición
                            mailbox.put(msg);
                            // opcional para no hacer busy-loop
                            Thread.sleep(10);
                        }
                    } else if ((!negativeDiagnostics.isEmpty())) {
                        Diagnostic take = negativeDiagnostics.remove(0);
                        doctorsDiagnostics.put(Integer.parseInt(msg.content), take);
                        msg.replyTo.put(new Message("null", "null", null));
                        Diagnostic diagnosis = doctorsDiagnostics.get(Integer.parseInt(msg.content));
                        BlockingQueue<Message> mb = diagnosis.getReplyTo();
                        mb.put(new Message("End", "" + diagnosis.getDiagnosis(), null));
                    } else {
                        // No hay positivos: reencolar la petición
                        mailbox.put(msg);
                        // opcional para no hacer busy-loop
                        Thread.sleep(10);
                    }

                }

                if (msg.type.equals("FINAL DIAGNOSIS")) {
                    Diagnostic diagnosis = doctorsDiagnostics.remove(Integer.parseInt(msg.content));
                    if (diagnosis != null) {
                        double randomValue = Math.random();
                        double inconclusiveValue = Math.random();

                        boolean isPositive = randomValue < CHANGE_PROBABILITY;
                        boolean isInconclusive = inconclusiveValue < INCONCLUSIVE_PROBABILITY;

                        if (isPositive) {
                            if (diagnosis.getDiagnosis().equals(MALIGNO)) {
                                diagnosis.setPositive(VENIGNO);
                            } else if (isInconclusive) {
                                diagnosis.setPositive("INCONCLUSIVE");
                            } else {
                                diagnosis.setPositive(MALIGNO);
                            }
                        }
                        BlockingQueue<Message> mb = diagnosis.getReplyTo();
                        mb.put(new Message("End", "" + diagnosis.getDiagnosis(), null));

                    }

                }

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
}
