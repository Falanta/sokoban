package fal.game.render;

import static fal.game.Client.manager;
import static fal.game.Client.milli_time;
import static fal.game.Main.Debug;
import static fal.game.Main.statistic_timer;
import static fal.game.Main.time_formatter;

import com.badlogic.gdx.Gdx;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fal.game.Client;
import fal.game.Main;
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
    public static Map<String,UIContainer> container_styles = new HashMap<String,UIContainer>();
    private boolean text_shadow = true;
    public Map<String,UIButton> buttons = new HashMap<String,UIButton>();
    public void DrawText(String text, int pos_x, int pos_y, float size, int height, String font_name, boolean shadow){
        font_name = (font_name == null)?"fonts/small_sokoban.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        if(shadow){
            font.setColor(0,0,0,0.3f);
            font.draw(this.owner.batch,text,pos_x+size,pos_y-size);
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos_x,pos_y);
        font.getData().setScale(1.0f);
    }
    public void DrawTextCentered(String text, int pos_x, int pos_y, float size, int height, String font_name,boolean shadow){
        font_name = (font_name == null)?"fonts/small_sokoban.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        glyphLayout.setText(font, text);
        if(shadow){
            font.setColor(0,0,0,0.3f);
            font.draw(this.owner.batch,text,pos_x+size-(glyphLayout.width/2),pos_y-size+(glyphLayout.height/2));
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos_x-(glyphLayout.width/2),pos_y+(glyphLayout.height/2));
        font.getData().setScale(1.0f);
    }
    public void DrawTextBuilder(StringBuilder text, int pos_x, int pos_y, float size, int height, String font_name, boolean shadow){
        font_name = (font_name == null)?"fonts/small_sokoban.ttf":font_name;
        BitmapFont font = manager.assetManager.get(font_name,BitmapFont.class);
        font.getData().setScale(size);
        font.getData().setLineHeight(height);
        if(shadow){
            font.setColor(0,0,0,0.3f);
            font.draw(this.owner.batch,text,pos_x+size,pos_y-size);
            font.setColor(1,1,1,1);
        }
        font.draw(this.owner.batch,text,pos_x,pos_y);
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
    public void ParseStyles(String folderPath) {
        Debug(String.format("- Loading %s...", folderPath));
        com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal("textures/"+folderPath);
        if (!dir.exists()) {
            Debug(String.format("ERROR: There is no folder %s", "textures/"+folderPath));
            return;
        }
        for (com.badlogic.gdx.files.FileHandle file : dir.list()) {
            if (!file.isDirectory()) {
                String path = file.path();
                String extension = file.extension().toLowerCase();
                if (extension.equals("png") || extension.equals("ogg")) {
                    Debug("Loading style: "+file.name().substring(0,file.name().length()-4));
                    container_styles.put(file.name().substring(0,file.name().length()-4),new UIContainer("styles/"+file.name().substring(0,file.name().length()-4)));
                }
            }
        }
    }
    public UI(Client owner){
        this.owner = owner;

        this.ui_viewport = new FitViewport(ui_size.x,ui_size.y,new OrthographicCamera());
        this.ui_viewport.apply();
        this.ui_viewport.update((int) ui_size.x, (int) ui_size.y,false);

        container_styles.put("template",new UIContainer("styles/template_style"));
        ParseStyles("styles");
//        container_styles.put("button",new UIContainer("styles/container_button"));
//        container_styles.put("button_01_s",new UIContainer("styles/button_01_s"));
//        container_styles.put("button_01_h",new UIContainer("styles/button_01_h"));
//        container_styles.put("button_01_p",new UIContainer("styles/button_01_p"));
//        container_styles.put("button_02_s",new UIContainer("styles/button_02_s"));
//        container_styles.put("button_02_h",new UIContainer("styles/button_02_h"));
//        container_styles.put("button_02_p",new UIContainer("styles/button_02_p"));

        buttons.put("main.play",new UIButton("Play",new Vector2(this.ui_size.x/4-2,-14),new Vector2(this.ui_size.x/4-8, 16),container_styles.get("button_01_s"),container_styles.get("button_01_h"),container_styles.get("button_01_p"),new Color(1,1,1,1),true));
        buttons.put("main.customize",new UIButton("Customize",new Vector2(this.ui_size.x/4-2,-46),new Vector2(this.ui_size.x/4-8, 16),container_styles.get("button_01_s"),container_styles.get("button_01_h"),container_styles.get("button_01_p"),new Color(1,1,1,1),true));
        buttons.put("main.settings",new UIButton("Settings",new Vector2(this.ui_size.x/4-2,-78),new Vector2(this.ui_size.x/4-8, 16),container_styles.get("button_01_s"),container_styles.get("button_01_h"),container_styles.get("button_01_p"),new Color(1,1,1,1),true));

        buttons.put("customize.back",new UIButton("Back",new Vector2(-this.ui_size.x/16,-this.ui_size.y/2+16),new Vector2(this.ui_size.x/8, 16),container_styles.get("button_01_s"),container_styles.get("button_01_h"),container_styles.get("button_01_p"),new Color(1,1,1,1),true));
        buttons.put("customize.next",new UIButton("Next",new Vector2(this.ui_size.x/4,-8),new Vector2(16, 16),container_styles.get("button_03_s"),container_styles.get("button_03_h"),container_styles.get("button_03_p"),new Color(1,1,1,1),false));
        buttons.put("customize.prev",new UIButton("Prev",new Vector2(-this.ui_size.x/4-16,-8),new Vector2(16, 16),container_styles.get("button_03_s"),container_styles.get("button_03_h"),container_styles.get("button_03_p"),new Color(1,1,1,1),false));

        buttons.put("game.back",new UIButton("Back",new Vector2(this.ui_size.x/2-20,this.ui_size.y/2-20),new Vector2(12, 12),container_styles.get("button_03_s"),container_styles.get("button_03_h"),container_styles.get("button_03_p"),new Color(1,1,1,1),false));
        buttons.put("game.restart",new UIButton("Restart",new Vector2(this.ui_size.x/2-44,this.ui_size.y/2-20),new Vector2(12, 12),container_styles.get("button_03_s"),container_styles.get("button_03_h"),container_styles.get("button_03_p"),new Color(1,1,1,1),false));
        buttons.put("game.next",new UIButton("Next",new Vector2(this.ui_size.x/2-68,this.ui_size.y/2-20),new Vector2(12, 12),container_styles.get("button_03_s"),container_styles.get("button_03_h"),container_styles.get("button_03_p"),new Color(1,1,1,1),false));
    }
    public void DrawLogoAnim(float delta,int x,int y){
        float back_delta = 1-delta;
        if(delta != 1) {
            this.owner.batch.draw(manager.GetRegion("tiles/crate_02_d"), x - 18, y - 18, 36, 36);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_s"), x - 90 - (int)(back_delta*8)*24, y - 12, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_o"), x - 66, y - 12 + (int)(back_delta*8)*24, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_k"), x - 42, y - 12 - (int)(back_delta*8)*24, 24, 24);

            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_b"), x + 21, y - 12 + (int)(back_delta*8)*24, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_a"), x + 45, y - 12 - (int)(back_delta*8)*24, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_n"), x + 69 + (int)(back_delta*8)*24, y - 12, 24, 24);
        }else{
            this.owner.batch.draw(manager.GetRegion("tiles/crate_02_e"), x - 18, y - 18, 36, 36);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_s"), x - 90, y - 12, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_o"), x - 66, y - 12, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_k"), x - 42, y - 12, 24, 24);

            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_b"), x + 21, y - 12, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_a"), x + 45, y - 12, 24, 24);
            this.owner.batch.draw(manager.GetRegion("ui/logo/letter_n"), x + 69, y - 12, 24, 24);
        }
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
        this.DrawTextBuilder(this.chat_line_builder,(int) -this.ui_size.x/2+1,(int) -this.ui_size.y/2+11,1.0f,9,"fonts/consolas.ttf",this.text_shadow);
    }
    public void DrawMainMenu(){
        List<String> button_names = Arrays.asList("main.play", "main.customize", "main.settings");
        if(!this.owner.state.logo_anim_loaded){
            long delta = milli_time - this.owner.state.menu_animation_start_time;
            float local_delta = delta / 1000.0f;
            if(delta < 1000) {
                this.DrawLogoAnim(1,(int) -this.ui_size.x / 4, (int) (this.ui_size.y / 4 + (100 * (1 - local_delta))));
//                this.DrawLogoAnim(local_delta, (int) -this.ui_size.x / 4, (int) this.ui_size.y / 4);
                //this.DrawTextCentered("SOKOBAN", (int) -this.ui_size.x / 4, (int) (this.ui_size.y / 4 + (100 * (1 - local_delta))), 4, 8, null, true);
                for (int y = 0; y < 3; y++) {
                    int anim_delay = Math.max(0, (int) (300 * (1 - ((delta + y * 300) / 1000.0f))));
                    container_styles.get("button_01_s").Draw(this.owner.batch, (int) (this.ui_size.x / 4 - 2 + anim_delay), -y * 32 - 14, new Vector2(this.ui_size.x / 4 - 8, 16), new Color(1, 1, 1, 1), 2, true);
                    this.DrawText(this.buttons.get(button_names.get(y)).name, (int) (this.ui_size.x / 4 + anim_delay), -y * 32, 2, 16, null, true);
                }
            }else{
                this.owner.state.logo_anim_loaded = true;
                DrawMainMenu();
            }
        } else{
            this.DrawLogoAnim(1,(int) -this.ui_size.x / 4, (int) this.ui_size.y / 4);
            //this.DrawTextCentered("SOKOBAN", (int) -this.ui_size.x / 4, (int) (this.ui_size.y / 4), 4, 8, null, true);
            for(int y = 0;y<3;y++) {
                this.buttons.get(button_names.get(y)).Draw(this.owner.batch,2,this.owner.state.cursor_pos);
            }
            this.DrawText("Play\nCustomize\nSettings",(int) this.ui_size.x/4,0,2,16,null,true);
        }

    }
    public void DrawCustomizeMenu(){
        this.buttons.get("customize.back").Draw(this.owner.batch,2,this.owner.state.cursor_pos);
        this.buttons.get("customize.next").Draw(this.owner.batch,2,this.owner.state.cursor_pos);
        this.owner.batch.draw(manager.GetRegion("ui/arrow_right"), this.ui_size.x/4,-8,16,16);
        this.buttons.get("customize.prev").Draw(this.owner.batch,2,this.owner.state.cursor_pos);
        this.owner.batch.draw(manager.GetRegion("ui/arrow_left"), -this.ui_size.x/4-16,-8,16,16);
        this.DrawTextCentered("Back",0,(int) -this.ui_size.y/2+25,2,16,null,true);

        this.owner.batch.draw(manager.GetRegion("entities/"+this.owner.state.skin_texture), -30, -30, 60, 60);
        String formatted_skin = this.owner.state.skin_texture.replace("player_", "").replace("_", " ").substring(0, 1).toUpperCase() + this.owner.state.skin_texture.replace("player_", "").replace("_", " ").substring(1);
        this.DrawTextCentered(formatted_skin,0,(int) this.ui_size.y/4,2,16,null,true);
    }
    public void DrawGameOverlay(){
        this.buttons.get("game.back").Draw(this.owner.batch,2,this.owner.state.cursor_pos);
        this.buttons.get("game.restart").Draw(this.owner.batch,2,this.owner.state.cursor_pos);
        this.owner.batch.draw(manager.GetRegion("ui/home"), this.ui_size.x/2-22,this.ui_size.y/2-22,16,16);
        this.owner.batch.draw(manager.GetRegion("ui/arrow_reload"), this.ui_size.x/2-46,this.ui_size.y/2-22,16,16);
        if(!this.owner.state.level_running) {
            this.buttons.get("game.next").Draw(this.owner.batch, 2, this.owner.state.cursor_pos);
            this.owner.batch.draw(manager.GetRegion("ui/arrow_right"), this.ui_size.x / 2 - 70, this.ui_size.y / 2 - 22, 16, 16);
        }
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

        this.DrawTextBuilder(this.level_info_builder,(int)-this.ui_size.x/2+2,(int) this.ui_size.y/2-2,1.0f,9,"fonts/small_sokoban.ttf",this.text_shadow);
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

        this.DrawTextBuilder(this.debug_info_builder,(int) -this.ui_size.x/2+1,(int) this.ui_size.y/2-1,1.0f,9,"fonts/consolas.ttf",this.text_shadow);
    }
    public void DrawChat(){
        this.chat_builder.setLength(0);
        this.chat_builder.append(" Chat:\n");
        for(Message msg: this.owner.chat){
            this.chat_builder.append(msg.formatted).append("\n");
        }
        this.DrawTextBuilder(this.chat_builder,(int) -this.ui_size.x/2+8,0,1.0f,10,"fonts/small_sokoban.ttf",this.text_shadow);
//        this.container_styles.get("button").Draw(this.owner.batch,new Vector2(-this.ui_size.x/2+6,-this.ui_size.y/2+6),new Vector2(25,25),new Color(1,1,1,1),2);
    }
}
