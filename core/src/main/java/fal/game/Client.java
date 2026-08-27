package fal.game;

import static fal.game.Main.ConnectMe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fal.game.input.KeyboardControl;
import fal.game.input.PlayerController;
import fal.game.network.CSConnection;
import fal.game.network.DataPackage;
import fal.game.network.Message;
import fal.game.render.Tile;
import fal.game.render.UI;
import fal.game.world.Body;
import fal.game.world.LevelMap;
import fal.game.world.World;

public class Client {
    public class ClientState {
        public boolean chat_hide_interaction = false;
        public boolean chat_hide = false;
        public boolean chat_interaction = false;
        public boolean chat_line_open = false;
        public String chat_line_buffer = "";
        public ClientState(){
        }
    }
    public static class ResourceManager {
        public final AssetManager assetManager;
        public TextureAtlas atlas;
        public ResourceManager() {
            assetManager = new AssetManager();

            FileHandleResolver resolver = new InternalFileHandleResolver();
            assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
            assetManager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
        }
        public void LoadFont(String path, int size) {
            fal.game.Main.Debug(String.format("Loading font %s",path));
            fal.game.Main.Debug(String.format("- File exists: %s",Gdx.files.internal("fonts/regular.ttf").exists()));
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontParams = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            fontParams.fontFileName = path;
            fontParams.fontParameters.size = size;
            fontParams.fontParameters.genMipMaps = false;
            fontParams.fontParameters.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
            fontParams.fontParameters.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
            fontParams.fontParameters.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ☺☻♥♦♣♠";;
            assetManager.load(path, BitmapFont.class, fontParams);
        }
        public void LoadAll() {
            Main.Debug("Loading assets...");
            assetManager.load("atlas/main_atlas.atlas", TextureAtlas.class);
            LoadFont("fonts/regular.ttf",16);
            LoadFont("fonts/consolas.ttf",12);
            assetManager.finishLoading();
            atlas = assetManager.get("atlas/main_atlas.atlas", TextureAtlas.class);
            Main.Debug(String.format("Done! Assets (%s):",assetManager.getLoadedAssets()));
            for (String fileName : assetManager.getAssetNames()) {
                Class<?> type = assetManager.getAssetType(fileName);
                Main.Debug(String.format(" - %s <%s>",fileName,type.getSimpleName()));
            }
            Main.Debug("Regions:");
            for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
                // region.name — это как раз то имя с учетом папки (например: "folder1/sprite1")
                Main.Debug(String.format(" - %s",region.name));
            }
        }
        public TextureRegion GetRegion(String name) {
            TextureAtlas.AtlasRegion region = atlas.findRegion(name);
            if (region == null) {
                throw new IllegalArgumentException("Unknown texture: "+name);
            }
            return region;
        }
        public void dispose() {
            assetManager.dispose();
        }
    }
    public class Camera{
        public Vector2 pos = new Vector2(0,0);
        public Vector2 target_pos = new Vector2(0,0);
        public float zoom = 2.0f;
        public float target_zoom = 1.0f;
        public float sensitivity = 0.05f;
        public float speed = 0.05f;
        public float move_speed = 2.0f;
        public OrthographicCamera camera = new OrthographicCamera();
        public Camera(){
            this.camera.setToOrtho(false, 480, 256);
        }
        public void Update(){
            this.pos.add((this.target_pos.x-this.pos.x)*sensitivity,(this.target_pos.y-this.pos.y)*sensitivity);
            this.zoom += (this.target_zoom - this.zoom)*sensitivity;
            this.camera.position.x = this.pos.x;
            this.camera.position.y = this.pos.y;
            this.camera.zoom = this.zoom;
            this.camera.update();
        }
    }
    private final String debug_prefix;
    public SpriteBatch batch;
    public static long milli_time = 0;
    public static ResourceManager manager;
    public CSConnection active_connection;
    public short last_tps;
    public int actual_fps;
    public String id;
    public Map<String,Object> last_data;
    public ArrayList<String> waiting_orders = new ArrayList<String>();
    public ArrayList<Message> chat = new ArrayList<Message>();
    public World world;
    public Camera cam;
    public Vector2 actual_cursor_position;
    public UI main_interface;
    public Map<String,Tile> tiles_pallete;
    public static final Vector2 tiles_size = new Vector2(12,12);
    public PlayerController controller;
    public ClientState state = new ClientState();
//    private Map<String,Object> control_memory = new HashMap<>();
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Client(String id, PlayerController controller){
        this.debug_prefix = String.format("[CLIENT:%s] ",id);
        Debug(String.format("Init client [%s]...",id));

        this.controller = controller;
        Gdx.input.setInputProcessor((InputProcessor) controller);
        this.cam = new Camera();

        this.id = id;
        this.active_connection = null;

        this.last_tps = -1;
        this.last_data = new HashMap();
        batch = new SpriteBatch();

        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = true;
        TexturePacker.process(settings,
            "textures",
            "atlas",
            "main_atlas"
        );

        this.manager = new ResourceManager();
        this.manager.LoadAll();

        this.main_interface = new UI(this);

        this.LoadTiles("tiles");

        this.world = new World();

        Debug("Done!");
    }
    public void LoadTiles(String path){
        Debug("- Loading tiles...");
        this.tiles_pallete = new HashMap<>();
        this.tiles_pallete.put("template",new Tile("tiles/template",true,true));
//        this.tiles_pallete.put("stone",new Tile("stone",new ArrayList<>(Arrays.asList("solid"))));
//        this.tiles_pallete.put("wooden_floor",new Tile("wooden_floor",new ArrayList<>(Arrays.asList())));
//        this.tiles_pallete.put("wooden_wall",new Tile("wooden_wall",new ArrayList<>(Arrays.asList("solid"))));
//        this.tiles_pallete.put("stone_floor",new Tile("stone_floor",new ArrayList<>(Arrays.asList())));
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
                    this.tiles_pallete.put(tile_object.name,new Tile(
                        tile_object.has("texture")?tile_object.getString("texture"):null,
                        tile_object.has("solid")?tile_object.getBoolean("solid"):false,
                        true
                    ));
                }
            }
        }
        Debug("  - "+this.tiles_pallete.toString());
    }
    public void Connect(CSConnection connection,String server_id){
        Debug(String.format("Joining: %s...",server_id));
        this.active_connection = connection;

        this.MakeOrder("get_map",true);

        Debug("Done!");
    }
    public void Leave(){
        Debug(String.format("Leaving: %s...",active_connection.server_name));
        this.active_connection.Delete("leave");
    }
    public void RunCommand(String[] arg){
        Debug(String.format("- Run command: %s",arg.toString()));
        if(arg.length == 0){return;}
        String command_type = arg[0];
        Debug(String.format("  - Command: %s",command_type));
        switch (command_type){
            case "/leave":
                Leave();
                this.world.map = new LevelMap("empty");
                break;
            case "/join":
                Leave();
                ConnectMe(this);
                break;
            case "/penis":
                SendMessage("GROB GROB КЛАДБИЩЕ PIDOR");
                break;
            case "/set_map":
                if(arg.length > 1){
                    String map_name = arg[1];
                    this.MakeOrder("set_map "+map_name,false);
                    this.AddOrder("get_map");
                }else{
                    SendMessage("Type map name: /set_map <map name>");
                }
                break;
            case "/next":
                this.MakeOrder("next_level",false);
                this.AddOrder("get_map");
                break;
            case "/prev":
                this.MakeOrder("prev_level",false);
                this.AddOrder("get_map");
                break;
            default:
                SendMessage("Unknown command: "+command_type);
                break;
        }
    }
    public void SendMessage(String text){
        if(text == null){return;}
        if(text.charAt(0) == '/'){
            this.SendToChat(new Message("<"+this.id+">",text));
            this.RunCommand(text.split(" "));
            return;
        }
        Debug(String.format("- Send message: %s",text));
        Message msg = new Message(this.id,text);
        this.SendToChat(msg);
        if(this.active_connection == null){return;}
        Map data = new HashMap();
        data.put("message",msg.Copy());
        this.active_connection.CSQueue.offer(new DataPackage(this.id,data));
    }
    public void SendToChat(Message msg){
        this.chat.add(msg);
        if(this.chat.size() > 10){
            this.chat.remove(0);
        }
    }
    public void DeleteConnection(String reason){
        Debug(String.format("Connection deleted: %s, reason: %s",active_connection.server_name,reason));
        this.active_connection = null;
    }
    public void PullData(){
        //Debug("    - Pull Data");
        if(!this.active_connection.SCQueue.isEmpty()){
            while(!this.active_connection.SCQueue.isEmpty()){
                DataPackage input_package = this.active_connection.SCQueue.poll();
                this.last_data.putAll(input_package.data);
            }
            //Debug("      - "+last_data.toString());
        }else{
            //Debug("      - There is no package");
        }
    }
    public Object ReadData(String id){
        if(this.last_data != null){
            if(this.last_data.containsKey(id)){
                return this.last_data.get(id);
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
    public void MakeOrder(String order_type,boolean wait_answer){
        Debug(String.format("- Making order <%s>",order_type));
        Map data = new HashMap();
        data.put("order",order_type);
        this.active_connection.CSQueue.offer(new DataPackage(this.id,data));
        if(wait_answer) {
            this.waiting_orders.add(order_type);
        }
    }
    public void AddOrder(String order_type){
        this.waiting_orders.add(order_type);
    }
    public void CheckOrders(){
        //Debug("     - Check orders");
        if(this.last_data != null && !this.waiting_orders.isEmpty()){
            Iterator<String> order_iterator = this.waiting_orders.iterator();
            while(order_iterator.hasNext()){
                String order_type = order_iterator.next();
                if(last_data.containsKey(order_type+"_answer")){
                    Object answer = last_data.get(order_type+"_answer");
                    Debug(String.format("       - Completed order <%s>: %s",order_type,answer.toString()));
                    switch (order_type){
                        case "get_map":
                            this.world.map.matrix = (ArrayList<ArrayList<LevelMap.Cell>>) answer;
                            this.world.map.name = (String)last_data.get(order_type+"_answer.name");
                            this.world.map.size = (Vector2)last_data.get(order_type+"_answer.size");
                            this.world.map.loaded = true;
                            this.world.map.PrintMatrix();

                            this.last_data.remove("get_map_answer");
                            this.last_data.remove("get_map_answer.name");
                            this.last_data.remove("get_map_answer.size");
                    }
                    order_iterator.remove();
                }
            }
        }
    }
    public void CheckMessages(){
        //Debug("     - Check messages");
        if(this.last_data.containsKey("message")) {
            Iterator<String> input_iterator = ((Map<String,Object>)last_data).keySet().iterator();
            while(input_iterator.hasNext()) {
                String input = input_iterator.next();
                if(input.contains("message.")){
                    Debug("       - "+input);
                    Message msg = (Message)this.last_data.get(input);
                    Debug("         - "+msg.log_formatted);
                    this.SendToChat(msg);
                    input_iterator.remove();
                }
            }
            this.last_data.remove("message");
        }else{
            //Debug("There is no messages");
        }
    }
    public void CheckData(){
        if(last_data == null){return;}
        Iterator arg_iterator = last_data.keySet().iterator();
        while(arg_iterator.hasNext()){
            String arg = ""+arg_iterator.next();
            Object input_data = last_data.get(arg);
            switch (arg){
                case "spawn_body": {
                    Debug("- Spawned body");
                    HashMap body_info = (HashMap<String, Object>) input_data;
                    String id = body_info.get("id").toString();
                    String type = body_info.get("type").toString();
                    Vector2 pos = (Vector2) body_info.get("pos");
                    String texture = body_info.get("texture").toString();
                    Debug("  - " + id);
                    this.world.AddBody(type, id, texture, pos);
                    this.world.bodies.get(id).texture_region = manager.GetRegion(texture);
                    arg_iterator.remove();
                    break;
                }
                case "delete_body": {
                    Debug("- Deleted body");
                    this.world.bodies.remove(input_data.toString());
                    arg_iterator.remove();
                    break;
                }
                case "update_body_pos": {
                    HashMap body_info = (HashMap<String, Object>) input_data;
                    this.world.bodies.get(body_info.get("id").toString()).pos = (Vector2) body_info.get("pos");
                    Debug(String.format("- Updated body position: %s [%s]",this.world.bodies.get(body_info.get("id").toString()).toString(),this.world.bodies.get(body_info.get("id").toString()).pos.toString()));
                    arg_iterator.remove();
                    break;
                }
                case "tps": {
                    this.last_tps = ((input_data == null) ? (short) -1 : (short) input_data);
                    break;
                }
            }
        }
    }
    public void RenderUI(){
        this.batch.setProjectionMatrix(this.main_interface.ui_viewport.getCamera().combined);
        this.batch.begin();
        this.main_interface.DrawDebugInformation();
        if(!this.state.chat_hide) {
            this.main_interface.DrawChat();
        }
        if(this.state.chat_line_open){
            this.main_interface.DrawChatLine(""+this.state.chat_line_buffer);
        }
        this.batch.end();
    }
    public void RenderBodies(int offset_x, int offset_y){
        for(String id: this.world.bodies.keySet()){
            Debug("Draw: "+id);
            Body body = this.world.bodies.get(id);
            this.batch.draw(body.texture_region,body.pos.x*tiles_size.x+offset_x,-body.pos.y*tiles_size.y+offset_y);
            if(body.show_name){
                this.main_interface.DrawTextCentered(body.id,new Vector2(body.pos.x*tiles_size.x+tiles_size.x/2+offset_x,(-body.pos.y+1)*tiles_size.y+tiles_size.y/2+offset_y),1f,10,"fonts/consolas.ttf",false);
            }
        }
    }
    public void RenderMap(int offset_x, int offset_y){
        float tile_size_x = tiles_size.x;
        float tile_size_y = tiles_size.y;
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(x,y);
                if(cell == null) {continue;}
                if(!tiles_pallete.containsKey(cell.type)){continue;}
                if(!tiles_pallete.get(cell.type).solid){
                    batch.draw(tiles_pallete.get(cell.type).texture, x * tile_size_x + offset_x, -y * tile_size_y + offset_y, tile_size_x, tile_size_y);
                }
            }
        }
        TextureRegion shadowTexture = manager.GetRegion("pixel_black");
        batch.setColor(0, 0, 0, 0.3f);
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(x,y);
                if(cell == null) {continue;}
                float drawX = x * tile_size_x + offset_x + 3;
                float drawY = -y * tile_size_y + offset_y - 3;
                if(!tiles_pallete.containsKey(cell.type)){
                    batch.draw(shadowTexture, drawX, drawY, tile_size_x, tile_size_y);
                }else if(tiles_pallete.get(cell.type).solid) {
                    batch.draw(shadowTexture, drawX, drawY, tile_size_x, tile_size_y);
                }
            }
        }
        batch.setColor(1, 1, 1, 1);
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(x,y);
                if(cell == null) {continue;}
                String tile_texture = "template";
                if(tiles_pallete.containsKey(cell.type)){
                    if(!tiles_pallete.get(cell.type).solid){continue;}
                    tile_texture = cell.type;
                }
                batch.draw(tiles_pallete.get(tile_texture).texture, x*tile_size_x+offset_x, -y*tile_size_y+offset_y, tile_size_x, tile_size_y);
            }
        }
    }
    public void Control(){
        if(this.controller == null) {return;}
        boolean chatLineOpen = this.state.chat_line_open;
        if(!chatLineOpen){
            if(this.controller.CameraZoomIn() && !this.controller.CameraZoomOut()){
                this.cam.target_zoom = Math.min(this.cam.target_zoom + this.cam.speed,1.0f);
            } else if(!this.controller.CameraZoomIn() && this.controller.CameraZoomOut()){
                this.cam.target_zoom = Math.max(this.cam.target_zoom - this.cam.speed,0.25f);
            }
            if(this.controller.CameraMoveRight() && !this.controller.CameraMoveLeft()){
                this.cam.target_pos.x = Math.min(this.cam.target_pos.x + this.cam.move_speed*this.cam.zoom,200.0f);
            } else if(!this.controller.CameraMoveRight() && this.controller.CameraMoveLeft()){
                this.cam.target_pos.x = Math.max(this.cam.target_pos.x - this.cam.move_speed*this.cam.zoom,-100.0f);
            }
            if(this.controller.CameraMoveUp() && !this.controller.CameraMoveDown()){
                this.cam.target_pos.y = Math.min(this.cam.target_pos.y + this.cam.move_speed*this.cam.zoom,200.0f);
            } else if(!this.controller.CameraMoveUp() && this.controller.CameraMoveDown()){
                this.cam.target_pos.y = Math.max(this.cam.target_pos.y - this.cam.move_speed*this.cam.zoom,-100.0f);
            }
            if (this.controller.ChatHideInteraction()) {
                if (!(boolean) this.state.chat_hide_interaction) {
                    if (this.state.chat_hide) {
                        this.state.chat_hide = false; // Hide chat
                    } else {
                        this.state.chat_hide = true; // Show chat
                    }
                }
            }
            if(this.controller.MoveUp()&&!this.controller.MoveDown()){
                this.active_connection.CSQueue.offer(new DataPackage(this.id,new HashMap<String,Object>(){{put("move","up");}}));
            }else if(!this.controller.MoveUp()&&this.controller.MoveDown()){
                this.active_connection.CSQueue.offer(new DataPackage(this.id,new HashMap<String,Object>(){{put("move","down");}}));
            }
            if(this.controller.MoveLeft()&&!this.controller.MoveRight()){
                this.active_connection.CSQueue.offer(new DataPackage(this.id,new HashMap<String,Object>(){{put("move","left");}}));
            }else if(!this.controller.MoveLeft()&&this.controller.MoveRight()){
                this.active_connection.CSQueue.offer(new DataPackage(this.id,new HashMap<String,Object>(){{put("move","right");}}));
            }
        }else{
            if (this.controller instanceof KeyboardControl) {
                this.state.chat_line_buffer = ((KeyboardControl) this.controller).typedBuffer.toString();
            }
        }
        if (this.controller.ChatInteraction()) {
            if(this.state.chat_line_open){
                this.state.chat_line_open = false; // Close chat line, send message
                if(!this.state.chat_line_buffer.isEmpty()){
                    this.SendMessage(this.state.chat_line_buffer);
                }
                this.state.chat_line_buffer = "";
                if (this.controller instanceof KeyboardControl) {
                    ((KeyboardControl) this.controller).setChatting(false);
                }
            }else{
                this.state.chat_line_open = true; // Open chat line
                if (this.controller instanceof KeyboardControl) {
                    ((KeyboardControl) this.controller).setChatting(true);
                }
            }
        }
    }
    public void Tick(boolean render){
        //Debug("  - Tick");
        milli_time = System.currentTimeMillis();
        //Debug(milli_time+"");
        if(this.active_connection != null) {
            this.PullData();
            this.CheckOrders();
            this.CheckData();
            this.CheckMessages();
        }

        this.Control();

        this.actual_fps = Gdx.graphics.getFramesPerSecond();

        if(render) {
            this.cam.Update();

            ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f);
            batch.setProjectionMatrix(this.cam.camera.combined);
            batch.begin();
            if (this.world.map.loaded) {
                int offset_x = (int) -(this.world.map.size.x/2*tiles_size.x);
                int offset_y = (int) (this.world.map.size.y/2*tiles_size.y);
                this.RenderMap(offset_x,offset_y);
                this.RenderBodies(offset_x,offset_y);
            }
            batch.end();
            this.RenderUI();
        }
    }
    public void Delete(){
        Debug("Deleting...");

        if(active_connection != null && active_connection.server != null) {
            Debug("- Leaving " + active_connection.server_name);
            try {
                this.Leave();
            } catch (Exception e) {
                Debug("Error: " + e);
            }
        }

        this.batch.dispose();
        manager.dispose();

        Debug("Done!");
    }
}
