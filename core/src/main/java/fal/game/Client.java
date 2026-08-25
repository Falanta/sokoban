package fal.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
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
import com.badlogic.gdx.utils.ScreenUtils;

import org.lwjgl.Sys;

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
import fal.game.world.LevelMap;
import fal.game.world.World;

public class Client {
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
            fontParams.fontParameters.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";;
            assetManager.load(path, BitmapFont.class, fontParams);
        }
        public void LoadAll() {
            fal.game.Main.Debug("Loading assets...");
            assetManager.load("assets/atlas/main_atlas.atlas", TextureAtlas.class);
            LoadFont("fonts/regular.ttf",16);
            LoadFont("fonts/consolas.ttf",12);
            assetManager.finishLoading();
            atlas = assetManager.get("assets/atlas/main_atlas.atlas", TextureAtlas.class);
            fal.game.Main.Debug(String.format("Done! Assets (%s):",assetManager.getLoadedAssets()));
            for (String fileName : assetManager.getAssetNames()) {
                Class<?> type = assetManager.getAssetType(fileName);
                fal.game.Main.Debug(String.format("%s <%s>",fileName,type.getSimpleName()));
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
    public Map last_data;
    public ArrayList<String> waiting_orders = new ArrayList<String>();
    public ArrayList<Message> chat = new ArrayList<Message>();
    public World world;
    public Camera cam;
    public Vector2 actual_cursor_position;
    public UI main_interface;
    public Map<String,Tile> tiles_pallete;
    public PlayerController controller;
    private Map<String,Object> control_memory = new HashMap<>();
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Client(String id, PlayerController controller){
        this.debug_prefix = String.format("[CLIENT:%s] ",id);
        Debug(String.format("Init client [%s]...",id));

        this.controller = controller;
        this.cam = new Camera();

        this.control_memory.put("chat_hide_interaction",false);
        this.control_memory.put("chat_hide",false);
        this.control_memory.put("chat_interaction",false);
        this.control_memory.put("chat_line_open",false);
        this.control_memory.put("chat_line_buffer","");

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
            "textures",       // Исходная папка с кучей мелких PNG
            "assets/atlas",            // Папка назначения для готового атласа
            "main_atlas"               // Имя будущего файла атласа (game_atlas.atlas)
        );

        this.manager = new ResourceManager();
        this.manager.LoadAll();

        this.main_interface = new UI(this);

        Debug("- Loading tiles...");
        this.tiles_pallete = new HashMap<>();
        this.tiles_pallete.put("template",new Tile("template",null));
        this.tiles_pallete.get("template").size = new Vector2(12,12);
        this.tiles_pallete.put("stone",new Tile("stone",new ArrayList<>(Arrays.asList("solid"))));
        this.tiles_pallete.put("wooden_floor",new Tile("wooden_floor",new ArrayList<>(Arrays.asList())));
        this.tiles_pallete.put("wooden_wall",new Tile("wooden_wall",new ArrayList<>(Arrays.asList("solid"))));
        this.tiles_pallete.put("stone_floor",new Tile("stone_floor",new ArrayList<>(Arrays.asList())));
        Debug("  - "+this.tiles_pallete.toString());

        this.world = new World();

        Debug("Done!");
    }
    public void Connect(CSConnection connection,String server_id){
        Debug(String.format("Joining: %s...",server_id));
        this.active_connection = connection;

        this.MakeOrder("get_map");

        Debug("Done!");
    }
    public void Leave(){
        Debug(String.format("Leaving: %s...",active_connection.server_name));
        this.active_connection.Delete("leave");
    }
    public void SendMessage(String text){
        Debug(String.format("- Send message: %s",text));
        Message msg = new Message(this.id,text);
        this.SendToChat(msg);
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
    public void DeleteConnection(){
        Debug(String.format("Connection deleted: %s",active_connection.server_name));
        this.active_connection = null;
    }
    public void PullData(){
        Debug("    - Pull Data");
        if(!this.active_connection.SCQueue.isEmpty()){
            while(!this.active_connection.SCQueue.isEmpty()){
                DataPackage input_package = this.active_connection.SCQueue.poll();
                this.last_data.putAll(input_package.data);
            }
            Debug("      - "+last_data.toString());
        }else{
            Debug("      - There is no package");
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
    public void MakeOrder(String order_type){
        Debug(String.format("- Making order <%s>",order_type));
        Map data = new HashMap();
        data.put("order",order_type);
        this.active_connection.CSQueue.offer(new DataPackage(this.id,data));
        this.waiting_orders.add(order_type);
    }
    public void CheckOrders(){
        Debug("     - Check orders");
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
        Debug("     - Check messages");
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
            Debug("There is no messages");
        }
    }
    public void RenderUI(){
        this.batch.setProjectionMatrix(this.main_interface.ui_viewport.getCamera().combined);
        this.batch.begin();
        this.main_interface.DrawDebugInformation();
        if(!(boolean)this.control_memory.get("chat_hide")) {
            this.main_interface.DrawChat();
        }
        if((boolean)this.control_memory.get("chat_line_open")){
            this.main_interface.DrawChatLine("GOVNO"+this.control_memory.get("chat_line_buffer"));
        }
        this.batch.end();
    }
    public void RenderMap(Vector2 offset){
        float tile_size_x = this.tiles_pallete.get("template").size.x;
        float tile_size_y = this.tiles_pallete.get("template").size.y;
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell == null) {continue;}
                if(!tiles_pallete.containsKey(cell.type)){continue;}
                if(!tiles_pallete.get(cell.type).solid){
                    batch.draw(tiles_pallete.get(cell.type).texture, x * tile_size_x + offset.x, -y * tile_size_y + offset.y, tile_size_x, tile_size_y);
                }
            }
        }
        TextureRegion shadowTexture = manager.GetRegion("pixel_black");
        batch.setColor(0, 0, 0, 0.3f);
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell == null) {continue;}
                float drawX = x * tile_size_x + offset.x + 3;
                float drawY = -y * tile_size_y + offset.y - 3;
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
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell == null) {continue;}
                String tile_texture = "template";
                if(tiles_pallete.containsKey(cell.type)){
                    if(!tiles_pallete.get(cell.type).solid){continue;}
                    tile_texture = cell.type;
                }
                batch.draw(tiles_pallete.get(tile_texture).texture, x*tile_size_x+offset.x, -y*tile_size_y+offset.y, tile_size_x, tile_size_y);
            }
        }
    }
    public void Control(){
        if(this.controller != null) {
            if(!(boolean)this.control_memory.get("chat_line_open")){
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
                    if (!(boolean)this.control_memory.get("chat_hide_interaction")) {
                        if((boolean)this.control_memory.get("chat_hide")){
                            this.control_memory.put("chat_hide",false); // Hide chat
                        }else{
                            this.control_memory.put("chat_hide",true); // Show chat
                        }
                        this.control_memory.put("chat_hide_interaction", true);
                    }
                } else {
                    if ((boolean)this.control_memory.get("chat_hide_interaction")) {
                        this.control_memory.put("chat_hide_interaction", false);
                    }
                }
            }
            if (this.controller.ChatInteraction()) {
                if (!(boolean)this.control_memory.get("chat_interaction")) {
                    if((boolean)this.control_memory.get("chat_line_open")){
                        this.control_memory.put("chat_line_open",false); // Close chat line, send message
                    }else{
                        this.control_memory.put("chat_line_open",true); // Open chat line
                        String buffer = ((String)this.control_memory.get("chat_line_buffer"));
                        if(!buffer.isEmpty()){
                            this.SendMessage(buffer);
                        }
                    }
                    this.control_memory.put("chat_interaction", true);
                }
            } else {
                if ((boolean)this.control_memory.get("chat_interaction")) {
                    this.control_memory.put("chat_interaction", false);
                }
            }
        }
    }
    public void Tick(boolean render){
        Debug("  - Tick");
        milli_time = System.currentTimeMillis();
        Debug(milli_time+"");
        if(this.active_connection != null) {
            this.PullData();
            this.CheckOrders();
            this.CheckMessages();
        }

        this.Control();

        this.actual_fps = Gdx.graphics.getFramesPerSecond();
        this.last_tps = (short) ((this.ReadData("tps") == null)?(short)-1:this.ReadData("tps"));

        if(render) {
            this.cam.Update();

            ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f);
            batch.setProjectionMatrix(this.cam.camera.combined);
            batch.begin();
            if (this.world.map.loaded) {
                this.RenderMap(new Vector2(50, 50));
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
