package fal.game;

import static fal.game.Main.time_formatter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import fal.game.network.CSConnection;
import fal.game.network.DataPackage;
import fal.game.network.Message;
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
    private ArrayList<String> log = new ArrayList<String>();
    private final String debug_prefix;
    private Map<String, CSConnection> connections;
    public World world;
    public HashMap<String, LevelMap> levels_list = new HashMap<>();
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
        this.log.add(text+"\n");
    }
    private void SaveLogs(){
        FileHandle file = Gdx.files.local(String.format("logs/%s.txt",time_formatter.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()))));
        StringBuilder log_string = new StringBuilder();
        for(String str : this.log){
            log_string.append(str);
        }
        file.writeString(log_string.toString(), false);
    }
    public void LoadLevels(String path){
        Debug(String.format("Loading levels in %s...",path));
        FileHandle dir = Gdx.files.internal(path);

        if (!dir.exists() || !dir.isDirectory()) {
            Debug(String.format(" - There is no assets/%s folder",path));
            return;
        }
        for (FileHandle file : dir.list()) {
            Debug(String.format(" - %s...",file.name()));
            if (!file.isDirectory() && file.extension().equals("map")) {
                String file_name = file.nameWithoutExtension();
                LevelMap map = new LevelMap(file_name);
                map.LoadFromFile(file);
                levels_list.put(file_name, map);
                Debug("   - Loaded map: " + file_name);
                map.PrintMatrix();
            }
        }
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

        this.LoadLevels("maps");

        this.world = new World();
        this.world.map = this.levels_list.get("level_05");

        Debug("Done!");
    }
    public void Connect(CSConnection connection, String player_id){
        Debug(String.format("Connection: %s...",player_id));
        this.connections.put(player_id,connection);
        this.ProvideMessage(new Message("server",String.format("%s joined",player_id)));
        Debug("Done!");
    }
    public void Kick(String player_id){
        if(this.connections.containsKey(player_id)){
            CSConnection selected_connection = this.connections.get(player_id);
            Debug(String.format("Kicking: %s...",selected_connection.client_name));
            selected_connection.Delete("kick");
        }else{
            Debug("Error: there is no "+player_id);
        }
    }
    public void DeleteConnection(String player_id){
        if(this.connections.containsKey(player_id)){
            CSConnection selected_connection = this.connections.get(player_id);
            Debug(String.format("Connection deleted: %s",selected_connection.client_name));
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
    public void CheckInputDatas(){
        Debug("    - Check input packages:");
        for(String player_id: connections.keySet()){
            Debug("      - "+player_id);
            if(this.connections.get(player_id).CSQueue.isEmpty()){
                Debug("        - There is no packages");
            }else {
                while (!this.connections.get(player_id).CSQueue.isEmpty()) {
                    DataPackage input_package = this.connections.get(player_id).CSQueue.poll();
                    if (input_package.data != null) {
                        Debug("        - "+input_package.data);
                        if(input_package.data.containsKey("order")){
                            CompleteOrder((String)input_package.data.get("order"),player_id);
                        }
                        if(input_package.data.containsKey("message")){
                            ProvideMessage((Message)input_package.data.get("message"));
                        }
                    }else{
                        Debug("        - Package is null");
                    }
                }
            }
        }
    }
    public void ProvideMessage(Message msg){
        Debug(String.format("          - Provide message from %s: %s",msg.author,msg.content));
        for(String player_id: connections.keySet()) {
            if(Objects.equals(player_id, msg.author)){continue;}
            Debug("            - " + player_id);
            Map data = new HashMap();
            data.put("message."+msg.author,msg.Copy());
            data.put("message",true);
            this.log.add(msg.log_formatted+"\n");
            this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
        }
    }
    public void CompleteOrder(String order_type, String player_id){
        Debug(String.format("          - Complete order: %s from %s",order_type,player_id));
        Map data = new HashMap();
        switch (order_type){
            case "get_map":
                data.put("get_map_answer",this.world.map.MatrixCopy());
                data.put("get_map_answer.name",this.world.map.name+"");
                data.put("get_map_answer.size",new Vector2(this.world.map.size));
                this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
        }

    }
    public void Tick(){
//        Debug("  - Tick");
        this.CheckInputDatas();

        for(String player_id: connections.keySet()){
            try {
                Debug("  - Send data to "+player_id);
                Map data = new HashMap();
                data.put("tps",this.actual_tps);
                Debug("    - "+data.toString());
                this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
            }catch (Exception e){
                Debug("Error: "+e);
            }
        }
    }
    public void Delete(){
        Debug("Deleting...");
        this.on = false;
        this.SaveLogs();

        for(String player_id: connections.keySet()){
            Debug("- Kick "+player_id);
            try {
                this.Kick(player_id);
            }catch (Exception e){
                Debug("Error: "+e);
            }
        }
        Debug("Done!");
    }
}
