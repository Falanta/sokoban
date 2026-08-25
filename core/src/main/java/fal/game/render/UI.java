package fal.game.render;

import static fal.game.Client.manager;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import fal.game.Client;
import fal.game.network.Message;

public class UI {

    public Viewport ui_viewport;
    public Vector2 ui_size = new Vector2(480.0f,250.0f);
    private final Client owner;
    private final StringBuilder debug_info_builder = new StringBuilder();
    private final StringBuilder chat_builder = new StringBuilder();
    public void DrawText(String text, Vector2 pos, float size, int height, String font_name){
        font_name = (font_name == null)?"fonts/regular.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        font.draw(this.owner.batch,text,pos.x,pos.y);
        font.getData().setScale(1.0f);
    }
    public void DrawTextBuilder(StringBuilder text, Vector2 pos, float size, int height, String font_name){
        font_name = (font_name == null)?"fonts/regular.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        font.draw(this.owner.batch,text,pos.x,pos.y);
        font.getData().setScale(1.0f);
    }
    public UI(Client owner){
        this.owner = owner;

        this.ui_viewport = new FitViewport(ui_size.x,ui_size.y,new OrthographicCamera());
        this.ui_viewport.apply();
        this.ui_viewport.update((int) ui_size.x, (int) ui_size.y);
    }
    public void DrawDebugInformation(){
        this.debug_info_builder.setLength(0);
        this.debug_info_builder.append("FPS:").append(this.owner.actual_fps).append("\nTPS:").append(this.owner.last_tps).append("\nConnected server: ").append(this.owner.active_connection.server_name);
        this.DrawTextBuilder(this.debug_info_builder,new Vector2(-this.ui_size.x/2+1,this.ui_size.y/2-1),1.0f,9,"fonts/consolas.ttf");
    }
    public void DrawChat(){
        this.chat_builder.setLength(0);
        this.chat_builder.append("Chat:\n");
        for(Message msg: this.owner.chat){
            this.chat_builder.append(msg.formatted).append("\n");
        }
        this.DrawTextBuilder(this.chat_builder,new Vector2(-this.ui_size.x/2+1,0),1.0f,9,"fonts/consolas.ttf");
    }
}
