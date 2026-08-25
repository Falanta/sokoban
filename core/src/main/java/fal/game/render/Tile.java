package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Main.Debug;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class Tile {
    public Vector2 size;
    public TextureRegion texture;
    public String name;
    public ArrayList<String> logic_tags;
    public boolean solid;
    public Tile(String texture_name, ArrayList<String> logic_tags){
        Debug("Loading tile: "+texture_name);
        if(texture_name != null) {
            this.texture = manager.GetRegion(texture_name);
            if(this.texture == null){
                Debug(" - Texture is null");
            }else{
                Debug(" - Texture: "+this.texture);
            }
        }
        this.name = texture_name;
        this.logic_tags = logic_tags;
        if(logic_tags != null) {
            this.solid = this.logic_tags.contains("solid");
        }
    }
    public Tile Copy(){
        return new Tile(this.name,this.logic_tags);
    }
}
