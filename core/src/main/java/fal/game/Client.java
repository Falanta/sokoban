package fal.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
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
import fal.game.render.Tile;
import fal.game.world.LevelMap;
import fal.game.world.World;

public class Client {
    public static class ResourceManager {
        private final AssetManager assetManager;
        private TextureAtlas atlas;
        public ResourceManager() {
            assetManager = new AssetManager();
        }
        public void loadAll() {
            assetManager.load("assets/atlas/main_atlas.atlas", TextureAtlas.class);
            assetManager.finishLoading();
            atlas = assetManager.get("assets/atlas/main_atlas.atlas", TextureAtlas.class);
        }
        public TextureRegion getRegion(String name) {
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
        private PlayerController controller;
        public Camera(PlayerController controller){
            this.controller = controller;
            this.camera.setToOrtho(false, 480, 256);
        }
        public void Update(){
            if(this.controller.CameraZoomIn() && !this.controller.CameraZoomOut()){
                this.target_zoom = Math.min(this.target_zoom + speed,1.0f);
            } else if(!this.controller.CameraZoomIn() && this.controller.CameraZoomOut()){
                this.target_zoom = Math.max(this.target_zoom - speed,0.25f);
            }
            if(this.controller.CameraMoveRight() && !this.controller.CameraMoveLeft()){
                this.target_pos.x = Math.min(this.target_pos.x + move_speed*zoom,200.0f);
            } else if(!this.controller.CameraMoveRight() && this.controller.CameraMoveLeft()){
                this.target_pos.x = Math.max(this.target_pos.x - move_speed*zoom,-100.0f);
            }
            if(this.controller.CameraMoveUp() && !this.controller.CameraMoveDown()){
                this.target_pos.y = Math.min(this.target_pos.y + move_speed*zoom,200.0f);
            } else if(!this.controller.CameraMoveUp() && this.controller.CameraMoveDown()){
                this.target_pos.y = Math.max(this.target_pos.y - move_speed*zoom,-100.0f);
            }

            this.pos.add((this.target_pos.x-this.pos.x)*sensitivity,(this.target_pos.y-this.pos.y)*sensitivity);
            this.zoom += (this.target_zoom - this.zoom)*sensitivity;
            this.camera.position.x = this.pos.x;
            this.camera.position.y = this.pos.y;
            this.camera.zoom = this.zoom;
            this.camera.update();
        }
    }
    private final String debug_prefix;
    private SpriteBatch batch;
    public static ResourceManager manager;
    private CSConnection active_connection;
    private short last_tps;
    public String id;
    public Map last_data;
    public ArrayList<String> waiting_orders = new ArrayList<String>();
    public World world;
    public Camera cam;
    public Map<String,Tile> tiles_pallete;
    public PlayerController controller;
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Client(String id){
        this.debug_prefix = String.format("[CLIENT:%s] ",id);
        Debug(String.format("Init client [%s]...",id));

        this.controller = new KeyboardControl();
        this.cam = new Camera(this.controller);

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
        this.manager.loadAll();

        Debug("- Loading tiles...");
        this.tiles_pallete = new HashMap<>();
        this.tiles_pallete.put("template",new Tile(null,null));
        this.tiles_pallete.get("template").size = new Vector2(12,12);
        this.tiles_pallete.put("stone",new Tile("stone",new ArrayList<>(Arrays.asList("solid"))));
        this.tiles_pallete.put("wooden_floor",new Tile("wooden_floor",new ArrayList<>(Arrays.asList())));
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
        Debug(String.format("Leaving: %s...",active_connection.server.id));
        this.active_connection.Delete("leave");
    }
    public void DeleteConnection(){
        Debug(String.format("Connection deleted: %s",active_connection.server.id));
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
        Debug("- Check orders");
        if(this.last_data != null && !this.waiting_orders.isEmpty()){
            Iterator<String> order_iterator = this.waiting_orders.iterator();
            while(order_iterator.hasNext()){
                String order_type = order_iterator.next();
                if(last_data.containsKey(order_type+"_answer")){
                    Object answer = last_data.get(order_type+"_answer");
                    Debug(String.format("  - Completed order <%s>: %s",order_type,answer.toString()));
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
    public void RenderMap(Vector2 offset){
        float tile_size_x = this.tiles_pallete.get("template").size.x;
        float tile_size_y = this.tiles_pallete.get("template").size.y;
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell != null && !tiles_pallete.get(cell.type).solid) { // Только не-плотные
                    batch.draw(tiles_pallete.get(cell.type).texture, x*tile_size_x+offset.x, -y*tile_size_y+offset.y, tile_size_x, tile_size_y);
                }
            }
        }
        TextureRegion shadowTexture = manager.getRegion("pixel_black");
        batch.setColor(0, 0, 0, 0.3f);
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell != null && tiles_pallete.get(cell.type).solid) {

                    // Рендерим тень со смещением +2 по X и -2 по Y
                    float drawX = x * tile_size_x + offset.x + 1;
                    float drawY = -y * tile_size_y + offset.y - 1;
                    batch.draw(shadowTexture, drawX, drawY, tile_size_x, tile_size_y);
                }
            }
        }
        batch.setColor(1, 1, 1, 1);
        for(short y = 0; y < this.world.map.size.y; y++){
            for(short x = 0; x < this.world.map.size.x; x++){
                LevelMap.Cell cell = this.world.map.Get(new Vector2(x,y));
                if(cell != null && tiles_pallete.get(cell.type).solid) {
                    batch.draw(tiles_pallete.get(cell.type).texture, x*tile_size_x+offset.x, -y*tile_size_y+offset.y, tile_size_x, tile_size_y);
                }
            }
        }
    }
    public void Render(){
        Debug("  - Render");
        this.PullData();
        this.CheckOrders();

        int actual_fps = Gdx.graphics.getFramesPerSecond();
        this.last_tps = (short) ((this.ReadData("tps") == null)?(short)-1:this.ReadData("tps"));

        Debug("    - FPS: "+actual_fps);
        Debug("    - TPS: "+this.last_tps);

        this.cam.Update();

        ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f);
        batch.setProjectionMatrix(this.cam.camera.combined);
        batch.begin();
        if(this.world.map.loaded){
            this.RenderMap(new Vector2(50,50));
        }
        batch.end();
    }
    public Client Delete(){
        Debug("Deleting...");

        if(active_connection != null && active_connection.server != null) {
            Debug("- Leaving " + active_connection.server.id);
            try {
                this.Leave();
            } catch (Exception e) {
                Debug("Error: " + e);
            }
        }

        this.batch.dispose();
        this.manager.dispose();

        Debug("Done!");
        return null;
    }
}
