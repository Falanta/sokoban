package fal.game.world;

import static fal.game.Main.Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class World {
    public LevelMap map;
    public Map<String,Body> bodies = new HashMap<>();
    public Vector2 spawn_position;
    public String GenerateID(String prefix){
        return prefix+((int)(Math.random() * (99999999 - 10000001)) + 10000000);
    }
    public Body AddBody(String type,String id,String texture_name,Vector2 pos){
        Body body = new Body(this,type,pos,texture_name);
        body.id = id==null?GenerateID(type):id;
        this.bodies.put(body.id,body);
        return body;
    }
    public World(){
        this.map = new LevelMap("default_map");
        this.spawn_position = new Vector2(1.0f,1.0f);
    }
    public Body FindBody(Vector2 pos,Body self){
        return FindBody(pos.x,pos.y,self);
    }
    public Body FindBody(float x, float y,Body self){
        Debug(String.format("Find body: %s,%s",x,y));
        int pos_x = (int) x;
        int pos_y = (int) y;
        for(String id: this.bodies.keySet()){
            if(id.equals(self.id)){continue;}
            Body body = this.bodies.get(id);
            Debug(String.format(" - %s: %s,%s; Solid: %s",id,(int)body.pos.x,(int)body.pos.y,body.solid));
            if(pos_x == (int)body.pos.x && pos_y == (int)body.pos.y){
                Debug("Found a solid body.");
                return body;
            }
        }
        Debug("There is no body.");
        return null;
    }
    public Body FindSolidBody(float x, float y,Body self){
        Debug(String.format("Find body: %s,%s",x,y));
        int pos_x = (int) x;
        int pos_y = (int) y;
        for(String id: this.bodies.keySet()){
            if(id.equals(self.id)){continue;}
            Body body = this.bodies.get(id);
            Debug(String.format(" - %s: %s,%s; Solid: %s",id,(int)body.pos.x,(int)body.pos.y,body.solid));
            if(pos_x == (int)body.pos.x && pos_y == (int)body.pos.y){
                if(body.solid) {
                    Debug("Found a solid body.");
                    return body;
                }
            }
        }
        Debug("There is no body.");
        return null;
    }
    public void InitLevel(){
        FileHandle file = Gdx.files.internal(this.map.path);
        Debug("Init level from file: "+file.name());
        String[] lines = file.readString("UTF-8").split("\\r?\\n");
        if (lines.length < 2) return;

        JsonReader jsonReader = new JsonReader();
        JsonValue root = jsonReader.parse(lines[0]);

        JsonValue sizeArr = root.get("size");
        int width = sizeArr.get(0).asInt();
        int height = sizeArr.get(1).asInt();

        if(root.has("spawn_pos")){
            JsonValue spawn_pos = root.get("spawn_pos");
            this.spawn_position.x = spawn_pos.get(0).asFloat();
            this.spawn_position.y = spawn_pos.get(1).asFloat();
        }else{
            this.spawn_position.x = 1;
            this.spawn_position.y = 1;
        }

        for (int i = 1+height; i < lines.length; i++) {
            String line = lines[i];
            Debug("- "+line);
            JsonValue action_root = jsonReader.parse(line);
            if(!action_root.has("action")){continue;}
            String action_type = action_root.getString("action");
            switch (action_type){
                case "spawn":{
                    try {
                        String type = action_root.getString("type");
                        JsonValue pos_v = action_root.get("pos");
                        Vector2 pos = new Vector2(pos_v.get(0).asFloat(),pos_v.get(1).asFloat());
                        String texture = action_root.getString("texture");
                        Debug("  - Spawn: "+type);

                        Body body = this.AddBody(type,null,texture,pos);
                        if(type.equals("crate")){
                            body.shadow = true;
                            body.solid = true;
                        }
                    }catch (Exception e){
                        Debug("  - Error: "+e);
                    }
                }
            }
        }
    }
}
