package edu.mondragon.os.pbl.hospital;

public class WaitingRoom{
    private int currentTurn;
    private int countWaitingPeople;
    private Hospital hospital;

    public WaitingRoom(Hospital hospital) {
        currentTurn = 0;
    }

    public synchronized void waitForTurn(int myTurn) throws InterruptedException {

        while (myTurn!=currentTurn) {
            wait();
        }
    }

    public synchronized void indicateNext() {
        currentTurn++;
        System.out.println("📢 Display calling turn #" + currentTurn);
        notifyAll();
    }
}