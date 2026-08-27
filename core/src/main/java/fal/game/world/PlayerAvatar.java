package fal.game.world;

import static fal.game.Server.tiles_info;

import com.badlogic.gdx.math.Vector2;

public class PlayerAvatar {
    public Body body;
    public String name;
    public World world;
    public PlayerAvatar(World world,String name){
        this.world = world;
        this.name = name;
        this.body = new Body("player",new Vector2(0,0),null);
    }
    public boolean Move(String direction){
        switch (direction){
            case "up": {
                LevelMap.Cell target_cell = this.world.map.Get((short) this.body.pos.x,(short)(this.body.pos.y-1));
                if(target_cell != null && tiles_info.get(target_cell.type).solid){return false;}
                this.body.pos.y -= 1.0f;
                return true;
            }
            case "down": {
                LevelMap.Cell target_cell = this.world.map.Get((short) this.body.pos.x,(short)(this.body.pos.y+1));
                if(target_cell != null && tiles_info.get(target_cell.type).solid){return false;}
                this.body.pos.y += 1.0f;
                return true;
            }
            case "left": {
                LevelMap.Cell target_cell = this.world.map.Get((short) (this.body.pos.x-1),(short)this.body.pos.y);
                if(target_cell != null && tiles_info.get(target_cell.type).solid){return false;}
                this.body.pos.x -= 1.0f;
                return true;
            }
            case "right": {
                LevelMap.Cell target_cell = this.world.map.Get((short) (this.body.pos.x+1),(short)this.body.pos.y);
                if(target_cell != null && tiles_info.get(target_cell.type).solid){return false;}
                this.body.pos.x += 1.0f;
                return true;
            }
        }
        return false;
    }
}
