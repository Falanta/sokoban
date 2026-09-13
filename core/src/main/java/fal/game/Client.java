package fal.game;

import static fal.game.Main.ConnectMe;
import static fal.game.Main.Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import fal.game.input.KeyboardControl;
import fal.game.input.PlayerController;
import fal.game.network.CSConnection;
import fal.game.network.DataPackage;
import fal.game.network.Message;
import fal.game.render.Tile;
import fal.game.render.UI;
import fal.game.render.UIButton;
import fal.game.world.Body;
import fal.game.world.LevelMap;
import fal.game.world.World;

public class Client {
    public class ClientState {
        public boolean chat_hide_interaction = false;
        public boolean chat_hide = true;
        public boolean chat_interaction = false;
        public boolean chat_line_open = false;
        public String chat_line_buffer = "";
        public String skin_texture = "player_flush";
        public boolean player_step_memory = false;
        public ArrayList<String> available_skins = new ArrayList<String>();
        public int move_counter = -1;
        public long actual_time = 0;
        public long start_time = 0;
        public long shader_anim_start_time = System.currentTimeMillis();
        public long menu_animation_start_time = 0;
        public boolean logo_anim_loaded = false;
        public String menu = "main";
        public boolean level_running = false;
        public boolean map_loaded = false;
        public Vector2 cursor_pos = new Vector2();
        public boolean music_mute = true;
        public String music_name = "sounds/kros_loop_msc.ogg";
        public String music_loop_name = "sounds/kros_loop_msc.ogg";
        public float music_default_volume = 0.1f;
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
            assetManager.setLoader(ShaderProgram.class, ".frag", new ShaderProgramLoader(resolver));
        }
        public void LoadFont(String path, int size) {
            fal.game.Main.Debug(String.format("Loading font %s",path));
            fal.game.Main.Debug(String.format("- File exists: %s",Gdx.files.internal(path).exists()));
            FreetypeFontLoader.FreeTypeFontLoaderParameter fontParams = new FreetypeFontLoader.FreeTypeFontLoaderParameter();
            fontParams.fontFileName = path;
            fontParams.fontParameters.size = size;
            fontParams.fontParameters.genMipMaps = false;
            fontParams.fontParameters.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
            fontParams.fontParameters.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest;
            fontParams.fontParameters.characters = "1234567890_.,:;-+()[]{}<>/\\!?%~*ABCDEFGHIJKLMNO '\"`=&@$#^PQRSTUVWXYZabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюяЂђЈјЉљЊњЋћЏџҐґЄєІіЇїЎў";;
            assetManager.load(path, BitmapFont.class, fontParams);
        }
        public void ParseShaders(String folderPath) {
            Main.Debug(String.format("- Loading shaders from %s...", folderPath));
            com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal(folderPath);
            if (!dir.exists()) {
                Main.Debug(String.format("ERROR: There is no folder %s", folderPath));
                return;
            }

            for (com.badlogic.gdx.files.FileHandle file : dir.list()) {
                if (!file.isDirectory()) {
                    String extension = file.extension().toLowerCase();
                    Main.Debug(String.format(" - %s",file.name()));
                    if (extension.equals("frag")) {
                        String fragPath = file.path();
                        String baseName = file.nameWithoutExtension();
                        String vertPath = folderPath + "/" + baseName + ".vert";
                        if (!Gdx.files.internal(vertPath).exists()) {
                            vertPath = "shaders/default.vert";
                        }
                        LoadShader(vertPath, fragPath);
                    }
                }
            }
        }
        public void LoadShader(String vertPath, String fragPath) { // Это написала нейросеть и я честно не очень понимаю как оно устроено
            Main.Debug(String.format("Loading shader: vert=%s, frag=%s", vertPath, fragPath));
            ShaderProgramLoader.ShaderProgramParameter params = new ShaderProgramLoader.ShaderProgramParameter();
            params.vertexFile = vertPath;
            assetManager.load(fragPath, ShaderProgram.class, params);
        }
        public void ParseSounds(String folderPath) {
            Main.Debug(String.format("- Loading %s...", folderPath));
            com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal(folderPath);
            if (!dir.exists()) {
                Main.Debug(String.format("ERROR: There is no folder %s", folderPath));
                return;
            }
            for (com.badlogic.gdx.files.FileHandle file : dir.list()) {
                if (!file.isDirectory()) {
                    String path = file.path();
                    String extension = file.extension().toLowerCase();
                    if (extension.equals("wav") || extension.equals("ogg")) {
                        if(file.name().contains("_msc")) {
                            LoadMusic(path);
                        }else if (file.name().contains("_snd")){
                            LoadSound(path);
                        }
                    }
                }
            }
        }

        public void LoadSound(String path) {
            Main.Debug(String.format("  - Loading sound %s", path));
            assetManager.load(path, Sound.class);
        }
        public void LoadMusic(String path) {
            Main.Debug(String.format("  - Loading music %s", path));
            assetManager.load(path, Music.class);
        }
        public void LoadAll() {
            Main.Debug("Loading assets...");
            assetManager.load("atlas/main_atlas.atlas", TextureAtlas.class);
            LoadFont("fonts/small_sokoban.ttf",8);
            LoadFont("fonts/consolas.ttf",12);
            ParseSounds("sounds");
            ShaderProgram.pedantic = false;
            ParseShaders("shaders");
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
        public void PlaySound(String path,float volume,float pitch,float pan){
            try {
                this.GetSound(path).play(volume,pitch,pan);
            }catch (Exception e){
                Main.Debug("ERROR: "+e);
            }
        }
        public Sound GetSound(String path) {
            return assetManager.get(path, Sound.class);
        }
        public Music GetMusic(String path) {
            return assetManager.get(path, Music.class);
        }
        public ShaderProgram GetShader(String fragPath) {
            ShaderProgram shader = assetManager.get(fragPath, ShaderProgram.class);
            if (!shader.isCompiled()) {
                Main.Debug("ERROR: Shader compilation failed:\n" + shader.getLog());
            }
            return shader;
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
    public UI main_interface;
    public Map<String,Tile> tiles_pallete;
    public ArrayList<String> levels_list = new ArrayList<String>();
    public static final Vector2 tiles_size = new Vector2(12,12);
    public PlayerController controller;
    public FrameBuffer frame_buffer;
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
        this.frame_buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 480, 250, false);
        this.frame_buffer.getColorBufferTexture().setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        );

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

        manager = new ResourceManager();
        manager.LoadAll();
        this.main_interface = new UI(this);
        this.LoadTiles("tiles");
        this.LoadLevels("maps");
        this.main_interface.GenerateLevelSelectButtons();

        for (TextureAtlas.AtlasRegion region : manager.atlas.getRegions()) {
            if(region.name.contains("entities/player")){
                Debug(region.name);
                this.state.available_skins.add(region.name.split("/")[1]);
            }
        }

        manager.PlaySound("sounds/empty_snd.ogg",0.0f,1.0f,0.0f);

        manager.GetMusic(this.state.music_loop_name).setLooping(true);
        if(!this.state.music_name.equals(this.state.music_loop_name)) {
            manager.GetMusic(this.state.music_name).setOnCompletionListener(music -> manager.GetMusic(this.state.music_loop_name).play());
        }
        manager.GetMusic(this.state.music_name).play();

        if(!this.state.music_mute) {
            this.UpdateMusicVolume(this.state.music_default_volume);
        }else{
            this.UpdateMusicVolume(0.0f);
        }

        this.world = new World();

        this.OpenMenu("main");

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
                this.levels_list.add(file_name);
            }
        }
    }
    public void UpdateMusicVolume(float volume){
        manager.GetMusic(this.state.music_name).setVolume(volume);
        manager.GetMusic(this.state.music_loop_name).setVolume(volume);
    }
    public void Connect(CSConnection connection,String server_id){
        Debug(String.format("Joining: %s...",server_id));
        this.state.map_loaded = false;
        this.active_connection = connection;

        this.MakeOrder("get_map",true);
        this.ResetLevel();
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
            case "/leave": {
                Leave();
                this.world.map = new LevelMap("empty");
                this.world.bodies.clear();
                this.state.map_loaded = false;
                this.state.level_running = false;
                break;
            }
            case "/join": {
                if(this.active_connection != null) {
                    Leave();
                }
                ConnectMe(this);
                break;
            }
            case "/penis": {
                SendMessage("GROB GROB КЛАДБИЩЕ PIDOR");
                break;
            }
            case "/set_map": {
                if (arg.length > 1) {
                    this.world.bodies.clear();
                    this.waiting_orders.remove("get_map_answer");
                    String map_name = arg[1];
                    this.state.map_loaded = false;
                    this.MakeOrder("set_map " + map_name, false);
                    this.AddOrder("get_map");
                } else {
                    SendMessage("Type map name: /set_map <map name>");
                }
                break;
            }
            case "/next": {
                this.world.bodies.clear();
                this.waiting_orders.remove("get_map_answer");
                this.state.map_loaded = false;
                this.MakeOrder("next_level", false);
                this.AddOrder("get_map");
                break;
            }
            case "/prev": {
                this.world.bodies.clear();
                this.waiting_orders.remove("get_map_answer");
                this.state.map_loaded = false;
                this.MakeOrder("prev_level", false);
                this.AddOrder("get_map");
                break;
            }
            case "/skin": {
                if (arg.length > 1) {
                    String texture_name = arg[1];
                    this.MakeOrder("set_skin " + texture_name, false);
                } else {
                    SendMessage("Type map name: /skin <texture name>");
                }
                break;
            }
            default: {
                SendMessage("Unknown command: " + command_type);
            }
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
//        //Debug("    - Pull Data");
//        if(!this.active_connection.SCQueue.isEmpty()){
//            while(!this.active_connection.SCQueue.isEmpty()){
//                DataPackage input_package = this.active_connection.SCQueue.poll();
//                this.last_data.putAll(input_package.data);
//            }
//            //Debug("      - "+last_data.toString());
//        }else{
//            //Debug("      - There is no package");
//        }
        if(!this.active_connection.SCQueue.isEmpty()) {
            while (!this.active_connection.SCQueue.isEmpty()) {
                DataPackage input_package = this.active_connection.SCQueue.poll();
                this.last_data.putAll(input_package.data);
                this.CheckData();
            }
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
//        Debug("     - Check orders");
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
                            this.state.map_loaded = true;
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
    public void ResetLevel(){
        this.state.move_counter = 0;
        this.state.start_time = System.currentTimeMillis();
        this.state.actual_time = 0;
        this.state.level_running = true;
    }
    public void OpenMenu(String menu_id){
        this.state.logo_anim_loaded = false;
        this.state.menu = menu_id;
        this.state.menu_animation_start_time = System.currentTimeMillis();
    }
    public boolean CheckEndGame(){
        boolean no_crates = false;
        for(Body body: this.world.bodies.values()){
            if(body.type.equals("crate")){
                if(!no_crates){no_crates = true;}
                if((body.texture.charAt(body.texture.length()-1)=='d')) {
                    return false;
                }
            }
        }
        return no_crates;
    }
    public void UpdateTime(){
        this.state.actual_time = System.currentTimeMillis()-this.state.start_time;
    }
    public float PosterizeNum(float num){
        return Math.round(num/2)*2;
    }
    public void UpdateCursor() {
        float virtualWidth = this.main_interface.ui_viewport.getWorldWidth();
        float virtualHeight = this.main_interface.ui_viewport.getWorldHeight();

        this.state.cursor_pos.set(
            (Gdx.input.getX() * (virtualWidth / Gdx.graphics.getWidth())) - (virtualWidth / 2f),
            (virtualHeight / 2f) - (Gdx.input.getY() * (virtualHeight / Gdx.graphics.getHeight()))
        );
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
                    boolean show_name = (boolean)body_info.get("show_name");
                    boolean shadow = (boolean)body_info.get("shadow");
                    Debug("  - " + id);
                    Body body = this.world.AddBody(type, id, texture, pos);
                    try {
                        body.texture_region = manager.GetRegion(texture);
                    }catch (Exception e) {
                        Debug("Error: "+e);
                        body.texture_region = manager.GetRegion("tiles/template");
                    }
                    arg_iterator.remove();
                    body.show_name = show_name;
                    body.show_name = this.world.bodies.get(id).type.equals("player");
                    body.shadow = shadow;
                    body.draw_pos = new Vector2(body.pos);
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
                    String id = body_info.get("id").toString();
                    this.world.bodies.get(id).pos = (Vector2) body_info.get("pos");
                    Debug(String.format("- Updated body position: %s [%s]",id,this.world.bodies.get(id).pos.toString()));
                    arg_iterator.remove();
                    break;
                }
                case "update_body_texture": {
                    HashMap body_info = (HashMap<String, Object>) input_data;
                    String id = body_info.get("id").toString();
                    String texture = body_info.get("texture").toString();
                    try {
                        this.world.bodies.get(id).texture = texture;
                        this.world.bodies.get(id).texture_region = manager.GetRegion(texture);
                    }catch (Exception e){
                        this.world.bodies.get(id).texture = "tiles/template";
                        this.world.bodies.get(id).texture_region = manager.GetRegion(this.world.bodies.get(id).texture);
                    }
                    Debug(String.format("- Updated body texture: %s [%s]",id,this.world.bodies.get(id).texture));

                    if(id.contains("crate")){
                        if(texture.contains("_e")){
                            manager.PlaySound("sounds/crate_place_snd.ogg",0.5f,1.0f,0.0f);
                        }else if (texture.contains("_d")){
                            manager.PlaySound("sounds/crate_remove_snd.ogg",0.5f,1.0f,0.0f);
                        }
                    }

                    arg_iterator.remove();
                    break;
                }
                case "move_count": {
                    Debug("- Move count");
                    if(input_data == null){
                        if(this.state.player_step_memory) {
                            manager.PlaySound("sounds/step_snd_2.ogg",0.5f,1.0f,0.0f);
                        }else{
                            manager.PlaySound("sounds/step_snd_2.ogg",0.5f,1.2f,0.0f);
                        }
                        this.state.player_step_memory = !this.state.player_step_memory;
                        if(this.state.level_running) {
                            if(this.state.move_counter == 0){
                                this.state.start_time = System.currentTimeMillis();
                            }
                            this.state.move_counter += 1;
                        }
                    }else{
                        this.state.move_counter = (int)input_data;
                    }
                    arg_iterator.remove();
                    break;
                }
                case "reset_level": {
                    Debug("- Reset level");
                    this.ResetLevel();
                    arg_iterator.remove();
                    break;
                }
                case "joined": {
                    Debug("- Joined to server");
                    this.MakeOrder("set_skin " + this.state.skin_texture, false);
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
//        this.main_interface.DrawDebugInformation();
        switch (this.state.menu) {
            case "game": {
                if (this.active_connection != null) {
                    this.main_interface.DrawLevelInformation();
                    if (!this.state.chat_hide) {
                        this.main_interface.DrawChat();
                    }
                    if (this.state.chat_line_open) {
                        this.main_interface.DrawChatLine("" + this.state.chat_line_buffer);
                    }
                    this.main_interface.DrawGameOverlay();
                } else {
                    this.OpenMenu("main");
                }
                this.main_interface.DrawMuteButton();
                break;
            }
            case "main": {
                this.main_interface.DrawMainMenu();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "customize": {
                this.main_interface.DrawCustomizeMenu();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "level_select": {
                this.main_interface.DrawSelectLevel();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
        }
        this.batch.end();
    }
    public void RenderBody(Body body, int offset_x,int offset_y){
        body.UpdateDrawPos(0.67f);
        this.batch.draw(body.texture_region,PosterizeNum(body.draw_pos.x*tiles_size.x)+offset_x,PosterizeNum(-body.draw_pos.y*tiles_size.y)+offset_y);
        if(body.show_name){
            this.main_interface.DrawTextCentered(body.id,(int) PosterizeNum(body.draw_pos.x*tiles_size.x+tiles_size.x/2)+offset_x,(int) PosterizeNum((-body.draw_pos.y+1)*tiles_size.y+tiles_size.y/2)+offset_y,1f,10,null,true);
        }
    }
    public void RenderBodies(int offset_x, int offset_y){
        List<String> sorted_bodies = this.world.bodies.keySet().stream()
            .sorted(Comparator.comparingInt((String id) -> {
                    Body body = this.world.bodies.get(id);
                    if ("player".equals(body.type)) {
                        return 2;
                    }
                    if (body.solid) {
                        return 1;
                    }
                    return 0;
                })
                .thenComparing(id -> id))
            .collect(Collectors.toList());

        for(String body_id : sorted_bodies){
            this.RenderBody(this.world.bodies.get(body_id), offset_x, offset_y);
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
        for(String id: this.world.bodies.keySet()) {
            Body body = this.world.bodies.get(id);
            if (body.shadow) {
                batch.setColor(0, 0, 0, 0.3f);
                batch.draw(shadowTexture, PosterizeNum(body.draw_pos.x * tiles_size.x) + offset_x + 3, PosterizeNum(-body.draw_pos.y * tiles_size.y) + offset_y - 3, tiles_size.x, tiles_size.y);
                batch.setColor(1, 1, 1, 1);
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
    public void RunLevel(String level_name){
        this.state.map_loaded = false;
        ConnectMe(this);
        SendMessage("/set_map "+level_name);
        this.OpenMenu("game");
    }
    public void Control(){
        if(this.controller == null) {return;}
        ((KeyboardControl)this.controller).UpdateMouse();
        UIButton mute_button = this.main_interface.buttons.get("global.mute");
        switch (this.state.menu) {
            case "game": {
                if (!this.state.chat_line_open) {
                    if (this.controller.CameraZoomIn() && !this.controller.CameraZoomOut()) {
                        this.cam.target_zoom = Math.min(this.cam.target_zoom + this.cam.speed, 1.0f);
                    } else if (!this.controller.CameraZoomIn() && this.controller.CameraZoomOut()) {
                        this.cam.target_zoom = Math.max(this.cam.target_zoom - this.cam.speed, 0.25f);
                    }
                    if (this.controller.CameraMoveRight() && !this.controller.CameraMoveLeft()) {
                        this.cam.target_pos.x = Math.min(this.cam.target_pos.x + this.cam.move_speed * this.cam.zoom, 200.0f);
                    } else if (!this.controller.CameraMoveRight() && this.controller.CameraMoveLeft()) {
                        this.cam.target_pos.x = Math.max(this.cam.target_pos.x - this.cam.move_speed * this.cam.zoom, -100.0f);
                    }
                    if (this.controller.CameraMoveUp() && !this.controller.CameraMoveDown()) {
                        this.cam.target_pos.y = Math.min(this.cam.target_pos.y + this.cam.move_speed * this.cam.zoom, 200.0f);
                    } else if (!this.controller.CameraMoveUp() && this.controller.CameraMoveDown()) {
                        this.cam.target_pos.y = Math.max(this.cam.target_pos.y - this.cam.move_speed * this.cam.zoom, -100.0f);
                    }
                    if (this.controller.ChatHideInteraction()) {
                        if (!(boolean) this.state.chat_hide_interaction) {
                            this.state.chat_hide = !this.state.chat_hide;
                        }
                    }
                    if (this.controller.MoveUp() && !this.controller.MoveDown()) {
                        this.active_connection.CSQueue.offer(new DataPackage(this.id, new HashMap<String, Object>() {{
                            put("move", "up");
                        }}));
                    } else if (!this.controller.MoveUp() && this.controller.MoveDown()) {
                        this.active_connection.CSQueue.offer(new DataPackage(this.id, new HashMap<String, Object>() {{
                            put("move", "down");
                        }}));
                    }
                    if (this.controller.MoveLeft() && !this.controller.MoveRight()) {
                        this.active_connection.CSQueue.offer(new DataPackage(this.id, new HashMap<String, Object>() {{
                            put("move", "left");
                        }}));
                    } else if (!this.controller.MoveLeft() && this.controller.MoveRight()) {
                        this.active_connection.CSQueue.offer(new DataPackage(this.id, new HashMap<String, Object>() {{
                            put("move", "right");
                        }}));
                    }
                } else {
                    if (this.controller instanceof KeyboardControl) {
                        this.state.chat_line_buffer = ((KeyboardControl) this.controller).typedBuffer.toString();
                    }
                }
                if (this.controller.ChatInteraction()) {
                    if (this.state.chat_line_open) {
                        this.state.chat_line_open = false; // Close chat line, send message
                        if (!this.state.chat_line_buffer.isEmpty()) {
                            this.SendMessage(this.state.chat_line_buffer);
                        }
                        this.state.chat_line_buffer = "";
                        if (this.controller instanceof KeyboardControl) {
                            ((KeyboardControl) this.controller).setChatting(false);
                        }
                    } else {
                        this.state.chat_line_open = true; // Open chat line
                        if (this.controller instanceof KeyboardControl) {
                            ((KeyboardControl) this.controller).setChatting(true);
                        }
                    }
                }
                if(this.controller.MouseInteraction()) {
                    if (this.main_interface.buttons.get("game.restart").Read()) {
                        manager.PlaySound("sounds/reload_snd.ogg",0.5f,1.0f,0.0f);
                        SendMessage("/set_map " + world.map.name);
                    }
                    if (this.main_interface.buttons.get("game.next").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg",0.5f,1.0f,0.0f);
                        SendMessage("/next");
                    }
                    if (this.main_interface.buttons.get("game.back").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg",0.5f,0.5f,0.0f);
                        SendMessage("/leave");
                        this.OpenMenu("main");
                    }
                    if (mute_button.Read()) {
                        this.state.music_mute = !this.state.music_mute;
                        this.UpdateMusicVolume(this.state.music_mute?0.0f:this.state.music_default_volume);
                    }
                }
                break;
            }
            case "main":{
                if(this.controller.MouseInteraction()) {
                    if (this.main_interface.buttons.get("main.play").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg",0.5f,1.0f,0.0f);
//                        ConnectMe(this);
//                        SendMessage("/set_map level_01");
//                        this.OpenMenu("game");
                        this.OpenMenu("level_select");
                        break;
                    }
                    if (this.main_interface.buttons.get("main.customize").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg",0.5f,1.0f,0.0f);
                        this.OpenMenu("customize");
                        break;
                    }
                    if (mute_button.Read()) {
                        this.state.music_mute = !this.state.music_mute;
                        this.UpdateMusicVolume(this.state.music_mute?0.0f:this.state.music_default_volume);
                        break;
                    }
                    if (this.main_interface.buttons.get("main.settings").Read()){
                        manager.GetSound("sounds/settings_snd.ogg").stop();
                        manager.PlaySound("sounds/settings_snd.ogg",0.25f,1.0f,0.0f);
                        break;
                    }
                }
                break;
            }
            case "customize":{
                if(this.controller.MouseInteraction()) {
                    if (this.main_interface.buttons.get("customize.back").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg",0.25f,0.5f,0.0f);
                        this.state.chat_line_open = false;
                        if (this.controller instanceof KeyboardControl) {
                            ((KeyboardControl) this.controller).setChatting(false);
                        }
                        this.OpenMenu("main");
                        break;
                    }
                    if (this.main_interface.buttons.get("customize.next").Read()) {
                        int actual = this.state.available_skins.indexOf(this.state.skin_texture);
                        this.state.skin_texture = this.state.available_skins.get(actual+1 >= this.state.available_skins.size()?0:actual+1);
                        manager.PlaySound("sounds/pop_snd.ogg",0.25f,1.0f,0.0f);
                        break;
                    }
                    if (this.main_interface.buttons.get("customize.prev").Read()) {
                        int actual = this.state.available_skins.indexOf(this.state.skin_texture);
                        this.state.skin_texture = this.state.available_skins.get(actual-1 < 0?this.state.available_skins.size()-1:actual-1);
                        manager.PlaySound("sounds/pop_snd.ogg",0.25f,0.9f,0.0f);
                        break;
                    }
                    if (mute_button.Read()) {
                        this.state.music_mute = !this.state.music_mute;
                        this.UpdateMusicVolume(this.state.music_mute?0.0f:this.state.music_default_volume);
                        break;
                    }
                    if (this.main_interface.buttons.get("customize.nickname").Read()){
                        if(this.state.chat_line_open) {
                            this.state.chat_line_open = false;
                            if (this.controller instanceof KeyboardControl) {
                                ((KeyboardControl) this.controller).setChatting(false);
                            }
                        }else {
                            this.state.chat_line_open = true;
                            if (this.controller instanceof KeyboardControl) {
                                ((KeyboardControl) this.controller).setChatting(true);
                            }
                            ((KeyboardControl) this.controller).typedBuffer.setLength(0);
                            ((KeyboardControl) this.controller).typedBuffer.append(this.id);
                        }
                        break;
                    }
                }
                if(this.state.chat_line_open) {
                    if (this.controller instanceof KeyboardControl) {
                        this.id = ((KeyboardControl) this.controller).typedBuffer.toString();
                    }
                    if (this.controller.ChatInteraction()) {
                        this.state.chat_line_open = false;
                        if (this.controller instanceof KeyboardControl) {
                            ((KeyboardControl) this.controller).setChatting(false);
                        }
                    }
                }
                break;
            }
            case "level_select":{
                if(this.controller.MouseInteraction()) {
                    if (this.main_interface.buttons.get("level_select.back").Read()) {
                        manager.PlaySound("sounds/pop_snd.ogg", 0.25f, 0.5f, 0.0f);
                        this.OpenMenu("main");
                        break;
                    }
                    if (mute_button.Read()) {
                        this.state.music_mute = !this.state.music_mute;
                        this.UpdateMusicVolume(this.state.music_mute?0.0f:this.state.music_default_volume);
                        break;
                    }
                    for(UIButton button: this.main_interface.select_level_buttons){
                        if(button.Read()){
                            manager.PlaySound("sounds/pop_snd.ogg",0.5f,1.0f,0.0f);
                            this.RunLevel(button.name);
                        }
                    }
                }
                break;
            }
        }
    }
    public void LevelCleared(){
        this.state.level_running = false;
        manager.PlaySound("sounds/level_clear_snd.ogg",0.5f,1.0f,0.0f);
    }
    public void Tick(boolean render){
        //Debug("  - Tick");
        milli_time = System.currentTimeMillis();
        this.UpdateCursor();
        //Debug(milli_time+"");
        if(this.active_connection != null) {
            this.PullData();
            this.CheckOrders();
            this.CheckMessages();
        }
        if(this.state.level_running & this.state.map_loaded){
            if(this.CheckEndGame()){
                this.LevelCleared();
            }else {
                if(this.state.move_counter != 0) {
                    this.UpdateTime();
                }
            }
        }

        this.Control();

        this.actual_fps = Gdx.graphics.getFramesPerSecond();

        if(render) {
            this.cam.Update();
            this.frame_buffer.begin();

            if(this.state.level_running || !this.state.menu.equals("game")) {
                ScreenUtils.clear(0.078f, 0.078f, 0.078f, 1f);
            }else{
                ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f);
            }
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
            this.frame_buffer.end();

            ScreenUtils.clear(0.0f, 0.0f, 1.0f, 1f);

            batch.setProjectionMatrix(main_interface.ui_viewport.getCamera().combined);
            batch.setShader(manager.GetShader("shaders/passthrough.frag"));
//            batch.getShader().setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.begin();
            // Передаем виртуальное разрешение буфера, а не размер окна!
            batch.getShader().setUniformf("u_resolution", 480f, 250f);
            batch.getShader().setUniformf("u_time", (float) (milli_time-this.state.shader_anim_start_time));
            Debug((float) (milli_time-this.state.shader_anim_start_time)+"");

            // Рисуем текстуру буфера на весь экран интерфейса
            batch.draw(this.frame_buffer.getColorBufferTexture(),
                -240, -125, // координаты левого нижнего угла (половина от 480x250)
                480, 250,   // ширина и высота
                0, 0, 1, 1);
            batch.end();

            batch.setShader(null);
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
