package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Main.Debug;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

public class Tile {
    public TextureRegion texture;
    public String name;
    public boolean solid;
    public Tile(String texture_name, boolean solid, boolean load_texture){
        Debug("Loading tile: "+texture_name);
        if(load_texture) {
            if (texture_name != null) {
                try {
                    Debug(" - Texture: " + texture_name);
                    this.texture = manager.GetRegion(texture_name);
                } catch (Exception e) {
                    Debug(" - Unknown texture");
                    this.texture = manager.GetRegion("tiles/template");
                }
            } else {
                Debug(" - Texture is null");
                this.texture = manager.GetRegion("tiles/template");
            }
        }
        this.name = texture_name;
        this.solid = solid;
    }
    public Tile Copy(boolean load_textures){
        return new Tile(this.name,this.solid,load_textures);
    }
}
