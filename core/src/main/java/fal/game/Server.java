package fal.game;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import fal.game.network.CSConnection;

public class Server implements Runnable{
    public String id;
    public volatile boolean on;
    public short target_tps; // Количество кадров логики в 1 секунду (ticks per second)
    public double target_spt; // Время, необходимое для 1 кадра (seconds per tick) в наносекундах
    private final String debug_prefix;
    private Map<String, CSConnection> connections;
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Server(String id){
        this.debug_prefix = String.format("[SERVER:%s] ",id);
        Debug(String.format("Init server [%s]...",id));

        this.id = id;
        this.connections = new Hashtable<String,CSConnection>();

        this.on = true;
        this.target_tps = 20;
        this.target_spt = 1000000000.0/this.target_tps;

        Debug("Done!");
    }
    public void Connect(CSConnection connection, String player_id){
        Debug(String.format("Connection: %s...",player_id));
        this.connections.put(player_id,connection);

        Debug("Done!");
    }
    public void Kick(String player_id){
        if(this.connections.containsKey(player_id)){
            CSConnection selected_connection = this.connections.get(player_id);
            Debug(String.format("Kicking: %s...",selected_connection.client.id));
            selected_connection.Delete("kick");
        }else{
            Debug("Error: there is no "+player_id);
        }
    }
    public void DeleteConnection(String player_id){
        if(this.connections.containsKey(player_id)){
            CSConnection selected_connection = this.connections.get(player_id);
            Debug(String.format("Connection deleted: %s",selected_connection.client.id));
            this.connections.remove(player_id);
        }else{
            Debug("Error: there is no "+player_id);
        }
    }

    @Override
    public void run() {
        Debug("- Init server thread, loading...");
        long lastTime = System.nanoTime();
        double unprocessed = 0;
        long now = 0;

        Debug("- Starting loop");
        while (this.on) {
            now = System.nanoTime();
            unprocessed += (now - lastTime) / this.target_spt;
            lastTime = now;
            while (unprocessed >= 1.0) {
                Debug("Delay: "+unprocessed);
                this.Tick();
                unprocessed--;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Debug("CRASH: "+e);
                this.on = false;
                break;
            }
        }
    }
    public void Tick(){
        Debug("  - Tick");
    }
    public Server Delete(){
        Debug("Deleting...");

        for(String player_id: connections.keySet()){
            Debug("- Kick "+player_id);
            try {
                this.Kick(player_id);
            }catch (Exception e){
                Debug("Error: "+e);
            }
        }
        this.on = false;

        Debug("Done!");
        return null;
    }
}
