package fal.game.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Body {
    public Vector2 pos;
    public String type;
    public String texture;
    public String id = null;
    public boolean show_name = true;
    public TextureRegion texture_region = null;
    public Body(String type,Vector2 pos,String texture){
        this.type = type;
        this.pos = pos;
        this.texture = texture==null?"tiles/template":texture;
    }
}
