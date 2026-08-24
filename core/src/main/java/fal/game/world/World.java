package fal.game.world;

import java.util.ArrayList;

public class World {
    public LevelMap map;
    public ArrayList<Body> bodies;
    public World(){
        this.map = new LevelMap("default_map");
        this.bodies = new ArrayList<Body>();
    }
    public void LoadLevel(String path){

    }
}
