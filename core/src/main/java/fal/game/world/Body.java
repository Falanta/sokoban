package fal.game.world;

import static fal.game.Main.Debug;
import static fal.game.Server.tiles_info;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Body {
    public Vector2 pos;
    public String type;
    public String texture;
    public String id = null;
    public boolean solid = false;
    public boolean fixed = false;
    public boolean show_name = true;
    public boolean shadow = false;
    public Vector2 draw_pos;
    public TextureRegion texture_region = null;
    public final World world;
    public Body(World world, String type,Vector2 pos,String texture){
        this.world = world;
        this.type = type;
        this.pos = pos;
        this.texture = texture==null?"tiles/template":texture;
    }
    public void UpdateDrawPos(float sensitivity){
        this.draw_pos.add((this.pos.x-this.draw_pos.x)*sensitivity,(this.pos.y-this.draw_pos.y)*sensitivity);
    }
    public boolean IsBlockedTile(String direction){
        switch (direction) {
            case "up": {
                LevelMap.Cell target_cell = this.world.map.Get((short) this.pos.x, (short) (this.pos.y - 1));
                if (target_cell != null && tiles_info.get(target_cell.type)) {
                    return true;
                }
                return false;
            }
            case "down": {
                LevelMap.Cell target_cell = this.world.map.Get((short) this.pos.x, (short) (this.pos.y + 1));
                if (target_cell != null && tiles_info.get(target_cell.type)) {
                    return true;
                }
                return false;
            }
            case "left": {
                LevelMap.Cell target_cell = this.world.map.Get((short) (this.pos.x - 1), (short) this.pos.y);
                if (target_cell != null && tiles_info.get(target_cell.type)) {
                    return true;
                }
                return false;
            }
            case "right": {
                LevelMap.Cell target_cell = this.world.map.Get((short) (this.pos.x + 1), (short) this.pos.y);
                if (target_cell != null && tiles_info.get(target_cell.type)) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    public Body IsBlockedBody(String direction){
        switch (direction) {
            case "up": {
                return this.world.FindSolidBody(this.pos.x,this.pos.y-1,this);
            }
            case "down": {
                return this.world.FindSolidBody(this.pos.x,this.pos.y+1,this);
            }
            case "left": {
                return this.world.FindSolidBody(this.pos.x-1,this.pos.y,this);
            }
            case "right": {
                return this.world.FindSolidBody(this.pos.x+1,this.pos.y,this);
            }
        }
        return null;
    }
    public void ForceMove(String direction){
        Debug(" - ForceMove from"+this.pos);
        switch (direction) {
            case "up": {
                this.pos.y -= 1.0f;
                break;
            }
            case "down": {
                this.pos.y += 1.0f;
                break;
            }
            case "left": {
                this.pos.x -= 1.0f;
                break;
            }
            case "right": {
                this.pos.x += 1.0f;
                break;
            }
        }
    }
}
