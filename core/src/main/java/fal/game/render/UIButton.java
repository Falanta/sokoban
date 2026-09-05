package fal.game.render;

import static fal.game.render.UI.container_styles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class UIButton {
    public final Vector2 pos;
    public final Vector2 size;
    public final String name;
    public final UIContainer style;
    public final UIContainer hover_style;
    public final UIContainer pressed_style;
    public final Color color;
    public final boolean shadow;
    public boolean pressed;
    public UIButton(String name, Vector2 pos, Vector2 size, UIContainer style, UIContainer hover_style, UIContainer pressed_style, Color color, boolean shadow){
        this.pos = pos;
        this.size = size;

        this.name = name;

        this.style = (style == null)?container_styles.get("template"):style;
        this.hover_style = (hover_style == null)?this.style:hover_style;
        this.pressed_style = (pressed_style == null)?this.style:pressed_style;

        this.color = (color == null)?new Color(1.0f,1.0f,1.0f,1.0f):color;
        this.shadow = shadow;
    }
    public void Draw(SpriteBatch batch,int scale, Vector2 cursor_pos){
        this.DrawForce(batch,(int) this.pos.x,(int) this.pos.y,this.size,this.color,scale,cursor_pos);
    }
    public boolean HitboxCheck(Vector2 pos){
        return (pos.x >= this.pos.x && pos.y >= this.pos.y && pos.x <= this.pos.x+size.x && pos.y <= this.pos.y + size.y);
    }
    public void DrawForce(SpriteBatch batch, int pos_x, int pos_y, Vector2 size, Color color, int scale, Vector2 cursor_pos){
        this.pressed = false;
        if(cursor_pos != null){
            if(this.HitboxCheck(cursor_pos)){
                if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
                    this.pressed_style.Draw(batch, pos_x, pos_y, size, color, scale,this.shadow);
                    this.pressed = true;
                }else{
                    this.hover_style.Draw(batch, pos_x, pos_y, size, color, scale,this.shadow);
                }
                return;
            }
        }
        this.style.Draw(batch,pos_x,pos_y,size,color,scale,this.shadow);
    }
    public boolean Read(){
        if(pressed) {
            this.pressed = false;
            return true;
        }else{
            return this.pressed;
        }
    }
}
