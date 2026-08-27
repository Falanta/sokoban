package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Client.milli_time;
import static fal.game.Main.statistic_timer;
import static fal.game.Main.time_formatter;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import fal.game.Client;
import fal.game.network.Message;
import fal.game.world.Body;

public class UI {

    public Viewport ui_viewport;
    public Vector2 ui_size = new Vector2(480.0f,250.0f);
    private final Client owner;
    private final StringBuilder debug_info_builder = new StringBuilder();
    private final StringBuilder level_info_builder = new StringBuilder();
    private final StringBuilder chat_builder = new StringBuilder();
    private final StringBuilder chat_line_builder = new StringBuilder();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private Map<String,UIContainer> container_styles = new HashMap<String,UIContainer>();
    private boolean text_shadow = true;
    public void DrawText(String text, Vector2 pos, float size, int height, String font_name, boolean shadow){
        font_name = (font_name == null)?"fonts/regular.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        if(shadow){
            font.setColor(Color.BLACK);
            font.draw(this.owner.batch,text,pos.x+2,pos.y-2);
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos.x,pos.y);
        font.getData().setScale(1.0f);
    }
    public void DrawTextCentered(String text, Vector2 pos, float size, int height, String font_name,boolean shadow){
        font_name = (font_name == null)?"fonts/regular.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        glyphLayout.setText(font, text);
        if(shadow){
            font.setColor(Color.BLACK);
            font.draw(this.owner.batch,text,pos.x+2-(glyphLayout.width/2),pos.y-2+(glyphLayout.height/2));
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos.x-(glyphLayout.width/2),pos.y+(glyphLayout.height/2));
        font.getData().setScale(1.0f);
    }
    public void DrawTextBuilder(StringBuilder text, Vector2 pos, float size, int height, String font_name, boolean shadow){
        font_name = (font_name == null)?"fonts/regular.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        if(shadow){
            font.setColor(0,0,0,0.3f);
            font.draw(this.owner.batch,text,pos.x+1,pos.y-1);
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos.x,pos.y);
        font.getData().setScale(1.0f);
    }
    public String FormatTime(long time){
        long totalSeconds = time / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long ms = time % 1000;
        if (hours > 0) {
            return String.format("%d:%02d:%02d:%03d", hours, minutes, seconds, ms);
        } else if (minutes > 0) {
            return String.format("%d:%02d:%03d", minutes, seconds, ms);
        } else {
            return String.format("%02d:%03d", seconds, ms);
        }
    }
    public UI(Client owner){
        this.owner = owner;

        this.ui_viewport = new FitViewport(ui_size.x,ui_size.y,new OrthographicCamera());
        this.ui_viewport.apply();
        this.ui_viewport.update((int) ui_size.x, (int) ui_size.y);

        this.container_styles.put("template",new UIContainer("styles/template_style"));
        this.container_styles.put("button",new UIContainer("styles/container_button"));
    }
    public void DrawChatLine(String text){
        this.owner.batch.setColor(0, 0, 0, 0.3f);
        this.owner.batch.draw(manager.GetRegion("pixel_black"),-this.ui_size.x/2+1,-this.ui_size.y/2+1,this.ui_size.x-2,11);
        this.owner.batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.chat_line_builder.setLength(0);
        this.chat_line_builder.append(text);
        if(milli_time%1000>500){
            this.chat_line_builder.append(".");
        }
        this.DrawTextBuilder(this.chat_line_builder,new Vector2(-this.ui_size.x/2+1,-this.ui_size.y/2+11),1.0f,9,"fonts/consolas.ttf",this.text_shadow);
    }
    public void DrawLevelInformation(){
        this.level_info_builder.setLength(0);
        this.level_info_builder
            .append("Level: ")
            .append(this.owner.world.map.name)
            .append("\nTime: ")
            .append(FormatTime(this.owner.state.actual_time))
            .append("\nMoves: ")
            .append(this.owner.state.move_counter);

        this.DrawTextBuilder(this.level_info_builder,new Vector2(0,this.ui_size.y/2-1),1.0f,9,"fonts/consolas.ttf",this.text_shadow);
    }
    public void DrawDebugInformation(){
        this.debug_info_builder.setLength(0);
        this.debug_info_builder.append("FPS:")
            .append(this.owner.actual_fps)
            .append("\nTPS:")
            .append(this.owner.last_tps)
            .append("\nClient ID: ")
            .append(this.owner.id);
        if(this.owner.active_connection!=null){
            this.debug_info_builder
                .append("\nConnected server: ")
                .append(this.owner.active_connection.server_name)
                .append("\nLevel: ")
                .append(this.owner.world.map.name);
            this.debug_info_builder.append("\nBodies: [");
            for(String id: this.owner.world.bodies.keySet()){
                this.debug_info_builder.append("\n - ").append(id);
            }
            this.debug_info_builder.append("\n]");
        }

        this.DrawTextBuilder(this.debug_info_builder,new Vector2(-this.ui_size.x/2+1,this.ui_size.y/2-1),1.0f,9,"fonts/consolas.ttf",this.text_shadow);
    }
    public void DrawChat(){
        this.chat_builder.setLength(0);
        this.chat_builder.append(" Chat:\n");
        for(Message msg: this.owner.chat){
            this.chat_builder.append(msg.formatted).append("\n");
        }
        this.DrawTextBuilder(this.chat_builder,new Vector2(-this.ui_size.x/2+1,0),1.0f,10,"fonts/consolas.ttf",this.text_shadow);
//        this.container_styles.get("button").Draw(this.owner.batch,new Vector2(-this.ui_size.x/2+6,-this.ui_size.y/2+6),new Vector2(25,25),new Color(1,1,1,1),2);
    }
}
