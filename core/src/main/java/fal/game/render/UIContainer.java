package fal.game.render;

import static fal.game.Client.manager;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

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
        this.base_texture = manager.GetRegion(this.texture_name);

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
    public void Draw(SpriteBatch batch, Vector2 pos, Vector2 size, Color color,int scale){
        TextureRegion shadowTexture = manager.GetRegion("pixel_black");
        int border_width = 2*scale;
        batch.setColor(0, 0, 0, 0.3f);
        batch.draw(shadowTexture, pos.x-border_width+2, pos.y-(border_width+2), size.x+border_width*2, size.y+border_width*2);

        batch.setColor(color.r, color.g, color.b, 1.0f);

        batch.draw(this.ld_corner,pos.x-border_width,pos.y-border_width,border_width,border_width);
        batch.draw(this.lu_corner,pos.x-border_width,pos.y+size.y,border_width,border_width);
        batch.draw(this.ru_corner,pos.x+size.x,pos.y+size.y,border_width,border_width);
        batch.draw(this.rd_corner,pos.x+size.x,pos.y-border_width,border_width,border_width);
        batch.draw(this.u_side,pos.x,pos.y+size.y,size.x,border_width);
        batch.draw(this.d_side,pos.x,pos.y-border_width,size.x,border_width);
        batch.draw(this.l_side,pos.x-border_width,pos.y,border_width,size.y);
        batch.draw(this.r_side,pos.x+size.x,pos.y,border_width,size.y);

        batch.draw(this.middle,pos.x,pos.y,size.x,size.y);

        batch.setColor(1, 1, 1, 1);
    };
}
