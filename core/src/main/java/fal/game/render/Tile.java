package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Main.Debug;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
            try {
                Debug(" - Texture: " + texture_name);
                this.texture = manager.GetRegion(texture_name);
            }catch (Exception e){
                Debug(" - Unknown texture");
                this.texture = manager.GetRegion("template");
            }
        }else{
            Debug(" - Texture is null");
            this.texture = manager.GetRegion("template");
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
