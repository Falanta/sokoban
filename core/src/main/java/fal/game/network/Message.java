package fal.game.network;

import static fal.game.Main.time_formatter;

import org.lwjgl.Sys;

import java.time.Instant;
import java.time.ZoneId;

public class Message {
    public final String author;
    public final long time;
    public final String content;
    public final String formatted;
    public final String log_formatted;
    public Message(String author, String content){
        this.author = author;
        this.content = content;
        this.time = System.currentTimeMillis();
        this.formatted = String.format("%s: %s",this.author,this.content);
        this.log_formatted = String.format("[%s] %s: %s",time_formatter.format(Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault())),this.author,this.content);
    }
    public Message Copy(){
        return new Message(this.author,this.content);
    }
}
