package edu.mondragon.os.pbl.hospital.mailbox;

import java.util.concurrent.BlockingQueue;

public class WaitingRoomMessage {

    public final String type;
    public final String content;
    public final BlockingQueue<Message> replyTo; // mailbox del cliente

    public WaitingRoomMessage(String type, String content, BlockingQueue<Message> replyTo) {
        this.type = type;
        this.content = content;
        this.replyTo = replyTo;
    }
}
