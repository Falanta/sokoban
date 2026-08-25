package fal.game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class UIButton {
    public final Vector2 pos;
    public final Vector2 size;
    public final String style;
    public final String hover_style;
    public final String pressed_style;
    public final Color color;
    public final Color hover_color;
    public final Color pressed_color;
    public UIButton(Vector2 pos, Vector2 size, String style, String hover_style, String pressed_style, Color color, Color hover_color, Color pressed_color){
        this.pos = pos;
        this.size = size;

        this.style = (style == null)?"template_style":style;
        this.hover_style = (hover_style == null)?style:hover_style;
        this.pressed_style = (pressed_style == null)?style:pressed_style;

        this.color = (color == null)?new Color(1.0f,1.0f,1.0f,1.0f):color;
        this.hover_color = (hover_color == null)?color:hover_color;
        this.pressed_color = (pressed_color == null)?color:pressed_color;
    }
}
