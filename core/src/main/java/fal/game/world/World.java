package fal.game.world;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class World {
    public LevelMap map;
    public ArrayList<Body> bodies;
    public Vector2 spawn_position;
    public World(){
        this.map = new LevelMap("default_map");
        this.bodies = new ArrayList<Body>();
        this.spawn_position = new Vector2(0.0f,0.0f);
    }
    public void LoadLevel(String path){

    }
}
