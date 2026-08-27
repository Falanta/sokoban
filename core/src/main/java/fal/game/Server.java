package fal.game;

import static fal.game.Main.time_formatter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fal.game.network.CSConnection;
import fal.game.network.DataPackage;
import fal.game.network.Message;
import fal.game.render.Tile;
import fal.game.world.LevelMap;
import fal.game.world.PlayerAvatar;
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
    public static Map<String,Tile> tiles_info;
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
    public void LoadTiles(String path){
        Debug("- Loading tiles...");
        this.tiles_info = new HashMap<>();
        this.tiles_info.put("template",new Tile(null,true,false));
        FileHandle dir = Gdx.files.internal(path);

        if (!dir.exists() || !dir.isDirectory()) {
            Debug(String.format("  - There is no assets/%s folder",path));
            return;
        }
        for (FileHandle file : dir.list()) {
            Debug(String.format("  - %s...",file.name()));
            JsonReader jsonReader = new JsonReader();
            if (!file.isDirectory() && file.extension().equals("json")) {
                String file_name = file.nameWithoutExtension();
                JsonValue root = jsonReader.parse(file.readString("UTF-8"));
                for (JsonValue tile_object : root) {
                    Debug("  - "+tile_object.name);
                    Debug("    - "+tile_object.toString());
                    this.tiles_info.put(tile_object.name,new Tile(
                        null,
                        tile_object.has("solid")?tile_object.getBoolean("solid"):false,
                        false
                    ));
                }
            }
        }
        Debug("  - "+this.tiles_info.toString());
    }
    public void DeleteBody(String id){
        this.world.bodies.remove(id);
        for(String player_id: connections.keySet()){
            try {
                Debug("  - Send data to "+player_id);
                Map data = new HashMap();
                data.put("delete_body",id);
                Debug("    - "+data.toString());
                this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
            }catch (Exception e){
                Debug("Error: "+e);
            }
        }
    }
    public void DeletePlayer(String player_id){
        this.world.players.remove(player_id);
        this.DeleteBody(player_id);
    }
    public void SpawnPlayer(String player_id,String texture_name){
        PlayerAvatar player = this.world.SpawnPlayer(player_id,texture_name);
        for(String player_id_send: connections.keySet()){
            try {
                Debug("  - Send data to "+player_id);
                Map data = new HashMap();
                data.put("spawn_body",new HashMap<String,Object>(){{
                    put("id",player_id);
                    put("type","player");
                    put("pos",new Vector2(player.body.pos));
                    put("texture",player.body.texture);
                }});
                Debug("    - "+data.toString());
                this.connections.get(player_id_send).SCQueue.offer(new DataPackage("server",data));
            }catch (Exception e){
                Debug("Error: "+e);
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
        this.LoadTiles("tiles");

        this.world = new World();
        this.world.map = this.levels_list.get("level_05");

        Debug("Done!");
    }
    public void Connect(CSConnection connection, String player_id){
        Debug(String.format("Connection: %s...",player_id));
        this.connections.put(player_id,connection);
        this.ProvideMessage(new Message("server",String.format("%s joined",player_id)));
        this.SpawnPlayer(player_id,"entities/player_welp");
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
    public void DeleteConnection(String player_id,String reason){
        if(this.connections.containsKey(player_id)){
            DeletePlayer(player_id);
            ProvideMessage(new Message("server",String.format("Player %s left (%s)",player_id,reason)));
            CSConnection selected_connection = this.connections.get(player_id);
            Debug(String.format("Connection deleted: %s, reason: %s",selected_connection.client_name,reason));
            this.connections.remove(player_id);
        }else{
            Debug("Error: there is no "+player_id);
        }
    }
    public void UpdateBodyPos(String id){
        for(String player_id: connections.keySet()){
            try {
                Debug("  - Send data to "+player_id);
                Map data = new HashMap();
                data.put("update_body_pos",new HashMap<String,Object>(){{
                    put("id",id);
                    put("pos",new Vector2(world.bodies.get(id).pos));
                }});
                Debug("    - "+data.toString());
                this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
            }catch (Exception e){
                Debug("Error: "+e);
            }
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
        //Debug("    - Check input packages:");
        for(String player_id: connections.keySet()){
            //Debug("      - "+player_id);
            if(this.connections.get(player_id).CSQueue.isEmpty()){
                //Debug("        - There is no packages");
            }else {
                while (!this.connections.get(player_id).CSQueue.isEmpty()) {
                    DataPackage input_package = this.connections.get(player_id).CSQueue.poll();
                    if (input_package.data != null) {
                        Debug("        - "+input_package.data);
                        for(String arg: input_package.data.keySet()) {
                            Object input_data = input_package.data.get(arg);
                            switch (arg) {
                                case "order":
                                    CompleteOrder((String) input_data, player_id);
                                    break;
                                case "message":
                                    ProvideMessage((Message) input_data);
                                    break;
                                case "move":
                                    if(this.world.players.get(player_id).Move((String) input_data)) {
                                        this.UpdateBodyPos(player_id);
                                    }
                                    break;
                            }
                        }
                    }else{
                        Debug("        - Package is null");
                    }
                }
            }
        }
    }
    private void ProvideMessage(Message msg){
        Debug(String.format("          - Provide message from %s: %s",msg.author,msg.content));
        for(String player_id: connections.keySet()) {
            if(Objects.equals(player_id, msg.author)){continue;}
            Debug("            - " + player_id);
            this.SendMessage(msg,player_id);
        }
    }
    private void ProvideMap(){
        Debug("          - Provide map");
        for(String player_id: connections.keySet()) {
            Debug("            - " + player_id);
            this.SendMap(player_id);
        }
    }
    private void SendMap(String player_id){
        Map data = new HashMap();
        data.put("get_map_answer",this.world.map.MatrixCopy());
        data.put("get_map_answer.name",this.world.map.name+"");
        data.put("get_map_answer.size",new Vector2(this.world.map.size));
        this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
    }
    private void SendMessage(Message msg,String player_id){
        Map data = new HashMap();
        data.put("message."+msg.author,msg.Copy());
        data.put("message",true);
        this.log.add(msg.log_formatted+"\n");
        this.connections.get(player_id).SCQueue.offer(new DataPackage("server",data));
    }
    private void NextMap(){
        List<String> levels = new ArrayList<>(this.levels_list.keySet());
        int actual = levels.indexOf(this.world.map.name);
        this.world.map = this.levels_list.get(
            actual+1 >= levels.size()?levels.get(0):levels.get(actual+1)
        );
    }
    private void PrevMap(){
        List<String> levels = new ArrayList<>(this.levels_list.keySet());
        int actual = levels.indexOf(this.world.map.name);
        this.world.map = this.levels_list.get(
            actual-1 < 0?levels.get(levels.size()-1):levels.get(actual-1)
        );
    }
    public void CompleteOrder(String order_type, String player_id){
        Debug(String.format("          - Complete order: %s from %s",order_type,player_id));
        String[] order_args = order_type.split(" ");
        if(order_args.length == 0){return;}
//        Map data = new HashMap();
        switch (order_args[0]){
            case "get_map":
                this.SendMap(player_id);
                break;
            case "set_map":
                if(order_args.length > 1){
                    String map_name = order_args[1];
                    if(this.levels_list.containsKey(map_name)){
                        this.world.map = this.levels_list.get(map_name);
                        ProvideMap();
                    }else{
                        SendMessage(new Message("server",String.format("Error: There is no '%s' map",map_name)),player_id);
                    }
                }else{
                    SendMessage(new Message("server","Error: Empty map name argument"),player_id);
                }
                break;
            case "next_level":
                NextMap();
                ProvideMap();
                break;
            case "prev_level":
                PrevMap();
                ProvideMap();
                break;
        }

    }
    public void Tick(){
//        Debug("  - Tick");
        this.CheckInputDatas();

        for(String player_id: connections.keySet()){
            try {
                //Debug("  - Send data to "+player_id);
                Map data = new HashMap();
                data.put("tps",this.actual_tps);
                //Debug("    - "+data.toString());
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
