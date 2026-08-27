package fal.game.world;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class World {
    public LevelMap map;
    public Map<String,Body> bodies = new HashMap<>();
    public Map<String,PlayerAvatar> players = new HashMap<>();
    public Vector2 spawn_position;
    public String GenerateID(String prefix){
        return prefix+((int)(Math.random() * (99999999 - 10000001)) + 10000000);
    }
    public PlayerAvatar SpawnPlayer(String player_id,String texture_name){
        PlayerAvatar player = new PlayerAvatar(this,player_id);
        if(texture_name!=null){player.body.texture = texture_name;}
        player.body.id = player_id;
        this.players.put(player_id,player);
        this.bodies.put(player_id,player.body);
        player.body.pos = this.spawn_position;
        return player;
    }
    public Body AddBody(String type,String id,String texture_name,Vector2 pos){
        Body body = new Body(type,pos,texture_name);
        body.id = id==null?GenerateID(type):id;
        this.bodies.put(body.id,body);
        return body;
    }
    public World(){
        this.map = new LevelMap("default_map");
        this.spawn_position = new Vector2(1.0f,1.0f);
    }
}
