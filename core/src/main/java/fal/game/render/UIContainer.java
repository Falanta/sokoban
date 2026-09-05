package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Main.Debug;
import static fal.game.render.UI.container_styles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.awt.Button;
import java.util.HashMap;
import java.util.Map;

public class UIContainer {
    public TextureRegion lu_corner;
    public TextureRegion ld_corner;
    public TextureRegion ru_corner;
    public TextureRegion rd_corner;
    public TextureRegion u_side;
    public TextureRegion d_side;
    public TextureRegion l_side;
    public TextureRegion r_side;
    public TextureRegion middle;
    public String texture_name;
    public TextureRegion base_texture;
    public UIContainer(String texture_name){
        this.texture_name = texture_name;
        try {
            this.base_texture = manager.GetRegion(this.texture_name);
        }catch (Exception e){
            Debug("Error: "+e);
            this.base_texture = manager.GetRegion("styles/template_style");
        }


        this.lu_corner = new TextureRegion(this.base_texture,0,0,2,2);
        this.ld_corner = new TextureRegion(this.base_texture,0,3,2,2);
        this.ru_corner = new TextureRegion(this.base_texture,3,0,2,2);
        this.rd_corner = new TextureRegion(this.base_texture,3,3,2,2);

        this.u_side = new TextureRegion(this.base_texture,2,0,1,2);
        this.d_side = new TextureRegion(this.base_texture,2,3,1,2);
        this.l_side = new TextureRegion(this.base_texture,0,2,2,1);
        this.r_side = new TextureRegion(this.base_texture,3,2,2,1);

        this.middle = new TextureRegion(this.base_texture,2,2,1,1);

    }
    public void Draw(SpriteBatch batch, int pos_x, int pos_y, Vector2 size, Color color,int scale,boolean shadow){
        int border_width = 2*scale;
        if(shadow) {
            TextureRegion shadowTexture = manager.GetRegion("pixel_black");
            batch.setColor(0, 0, 0, 0.3f);
            batch.draw(shadowTexture, pos_x - border_width + 2, pos_y - (border_width + 2), size.x + border_width * 2, size.y + border_width * 2);
        }

        batch.setColor(color.r, color.g, color.b, 1.0f);

        batch.draw(this.ld_corner,pos_x-border_width,pos_y-border_width,border_width,border_width);
        batch.draw(this.lu_corner,pos_x-border_width,pos_y+size.y,border_width,border_width);
        batch.draw(this.ru_corner,pos_x+size.x,pos_y+size.y,border_width,border_width);
        batch.draw(this.rd_corner,pos_x+size.x,pos_y-border_width,border_width,border_width);
        batch.draw(this.u_side,pos_x,pos_y+size.y,size.x,border_width);
        batch.draw(this.d_side,pos_x,pos_y-border_width,size.x,border_width);
        batch.draw(this.l_side,pos_x-border_width,pos_y,border_width,size.y);
        batch.draw(this.r_side,pos_x+size.x,pos_y,border_width,size.y);

        batch.draw(this.middle,pos_x,pos_y,size.x,size.y);

        batch.setColor(1, 1, 1, 1);
    };
}
