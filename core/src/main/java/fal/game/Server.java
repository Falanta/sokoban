package fal.game;

import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import fal.game.network.CSConnection;
import fal.game.network.DataPackage;
import fal.game.world.LevelMap;
import fal.game.world.World;

public class Server implements Runnable{
    public String id;
    public volatile boolean on;
    public short target_tps; // Количество кадров логики в 1 секунду (ticks per second)
    public double target_spt; // Время, необходимое для 1 кадра (seconds per tick) в наносекундах
    public short actual_tps;
    private short tick_counter = 0;
    private long tick_timer = System.nanoTime();
    private final String debug_prefix;
    private Map<String, CSConnection> connections;
    public World world;
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
        this.actual_tps = -1;

        this.world = new World();
        this.world.map.GenerateEmpty(new Vector2(5,5));
        Debug(this.world.map.matrix.toString());
        this.world.map.Set(new Vector2(0,0),new LevelMap.Cell("stone", (short) 0, (short) 0));
        this.world.map.Set(new Vector2(4,4),new LevelMap.Cell("stone", (short) 4, (short) 4));
        this.world.map.PrintMatrix();

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
                this.tick_counter ++;
                unprocessed--;
            }

            if (now - this.tick_timer >= 1000000000) {
                this.actual_tps = this.tick_counter;
                this.tick_counter = 0;
                this.tick_timer = now;
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

        for(String player_id: connections.keySet()){
            try {
                Map data = new HashMap();
                data.put("tps",this.actual_tps);
                this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
            }catch (Exception e){
                Debug("Error: "+e);
            }
        }
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
