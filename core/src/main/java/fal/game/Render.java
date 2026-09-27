package fal.game;

import static fal.game.Client.manager;
import static fal.game.Client.milli_time;
import static fal.game.Client.tiles_size;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import fal.game.render.UI;
import fal.game.world.Body;
import fal.game.world.LevelMap;

public class Render {
    public final Client owner;
    public UI main_interface = null;
    public Client.ClientState state;
    public SpriteBatch batch = new SpriteBatch();
    public FrameBuffer frame_buffer;
    public Client.Camera cam;
    public Render(Client client){
        this.owner = client;
        this.state = this.owner.state;
        this.frame_buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 480*4, 250*4, false);
        this.frame_buffer.getColorBufferTexture().setFilter(
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
            com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        );
        this.cam = new Client.Camera();
    }
    public void RenderUI(){
        this.batch.setProjectionMatrix(this.main_interface.ui_viewport.getCamera().combined);
        this.batch.begin();
//        this.main_interface.DrawDebugInformation();
        switch (this.owner.state.menu) {
            case "intro": {
                this.main_interface.DrawIntro();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "meeting": {
                this.main_interface.DrawMeeting();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "game": {
                if (this.owner.active_connection != null) {
                    this.main_interface.DrawLevelInformation();
                    if (!this.state.chat_hide) {
                        this.main_interface.DrawChat();
                    }
                    if (this.state.chat_line_open) {
                        this.main_interface.DrawChatLine("" + this.state.chat_line_buffer);
                    }
                    this.main_interface.DrawGameOverlay();
                } else {
                    this.owner.OpenMenu("main");
                }
                this.main_interface.DrawMuteButton();
                break;
            }
            case "main": {
                this.main_interface.DrawMainMenu();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "customize": {
                this.main_interface.DrawCustomizeMenu();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "level_select": {
                this.main_interface.DrawSelectLevel();
                this.main_interface.DrawMuteButton();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "credits": {
                this.main_interface.DrawCredits();
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
            case "video": {
                this.main_interface.DrawTextCentered("x",(int)this.state.cursor_pos.x,(int)this.state.cursor_pos.y,1,8,null,false);
                break;
            }
        }
        this.batch.end();
    }
    public void RunVideo(String name){

    }
    public void RenderBody(Body body, int offset_x, int offset_y){
        body.UpdateDrawPos(0.67f);
        this.batch.draw(body.texture_region,this.owner.PosterizeNum(body.draw_pos.x*tiles_size.x)+offset_x,this.owner.PosterizeNum(-body.draw_pos.y*tiles_size.y)+offset_y);
        if(body.show_name){
            this.main_interface.DrawTextCentered(body.id,(int) this.owner.PosterizeNum(body.draw_pos.x*tiles_size.x+tiles_size.x/2)+offset_x,(int) this.owner.PosterizeNum((-body.draw_pos.y+1)*tiles_size.y+tiles_size.y/2)+offset_y,1f,10,null,true);
        }
    }
    public void RenderBodies(int offset_x, int offset_y){
        List<String> sorted_bodies = this.owner.world.bodies.keySet().stream()
            .sorted(Comparator.comparingInt((String id) -> {
                    Body body = this.owner.world.bodies.get(id);
                    if ("player".equals(body.type)) {
                        return 2;
                    }
                    if (body.solid) {
                        return 1;
                    }
                    return 0;
                })
                .thenComparing(id -> id))
            .collect(Collectors.toList());

        for(String body_id : sorted_bodies){
            this.RenderBody(this.owner.world.bodies.get(body_id), offset_x, offset_y);
        }
    }
    public void RenderMap(int offset_x, int offset_y){
        float tile_size_x = tiles_size.x;
        float tile_size_y = tiles_size.y;
        for(short y = 0; y < this.owner.world.map.size.y; y++){
            for(short x = 0; x < this.owner.world.map.size.x; x++){
                LevelMap.Cell cell = this.owner.world.map.Get(x,y);
                if(cell == null) {continue;}
                if(!this.owner.tiles_pallete.containsKey(cell.type)){continue;}
                if(!this.owner.tiles_pallete.get(cell.type).solid){
                    batch.draw(this.owner.tiles_pallete.get(cell.type).texture, x * tile_size_x + offset_x, -y * tile_size_y + offset_y, tile_size_x, tile_size_y);
                }
            }
        }
        TextureRegion shadowTexture = manager.GetRegion("pixel_black");
        batch.setColor(0, 0, 0, 0.3f);
        for(short y = 0; y < this.owner.world.map.size.y; y++){
            for(short x = 0; x < this.owner.world.map.size.x; x++){
                LevelMap.Cell cell = this.owner.world.map.Get(x,y);
                if(cell == null) {continue;}
                float drawX = x * tile_size_x + offset_x + 3;
                float drawY = -y * tile_size_y + offset_y - 3;
                if(!this.owner.tiles_pallete.containsKey(cell.type)){
                    batch.draw(shadowTexture, drawX, drawY, tile_size_x, tile_size_y);
                }else if(this.owner.tiles_pallete.get(cell.type).solid) {
                    batch.draw(shadowTexture, drawX, drawY, tile_size_x, tile_size_y);
                }
            }
        }
        for(String id: this.owner.world.bodies.keySet()) {
            Body body = this.owner.world.bodies.get(id);
            if (body.shadow) {
                batch.setColor(0, 0, 0, 0.3f);
                batch.draw(shadowTexture, this.owner.PosterizeNum(body.draw_pos.x * tiles_size.x) + offset_x + 3, this.owner.PosterizeNum(-body.draw_pos.y * tiles_size.y) + offset_y - 3, tiles_size.x, tiles_size.y);
                batch.setColor(1, 1, 1, 1);
            }
        }
        batch.setColor(1, 1, 1, 1);
        for(short y = 0; y < this.owner.world.map.size.y; y++){
            for(short x = 0; x < this.owner.world.map.size.x; x++){
                LevelMap.Cell cell = this.owner.world.map.Get(x,y);
                if(cell == null) {continue;}
                String tile_texture = "template";
                if(this.owner.tiles_pallete.containsKey(cell.type)){
                    if(!this.owner.tiles_pallete.get(cell.type).solid){continue;}
                    tile_texture = cell.type;
                }
                batch.draw(this.owner.tiles_pallete.get(tile_texture).texture, x*tile_size_x+offset_x, -y*tile_size_y+offset_y, tile_size_x, tile_size_y);
            }
        }
    }
    public void RenderMain(){
        this.cam.Update();
        this.frame_buffer.begin();

        this.state.music_details_volume += (this.state.music_details_target_volume - this.state.music_details_volume)*0.05f;
        this.owner.UpdateMusicDetails();

        if(this.state.level_running || !this.state.menu.equals("game")) {
            ScreenUtils.clear(0.078f, 0.078f, 0.078f, 1f);
        }else{
            ScreenUtils.clear(0.15f, 0.15f, 0.15f, 1f);
        }
        this.batch.setProjectionMatrix(this.cam.camera.combined);
        batch.begin();
        if (this.owner.world.map.loaded) {
            int offset_x = (int) -(this.owner.world.map.size.x/2*tiles_size.x);
            int offset_y = (int) (this.owner.world.map.size.y/2*tiles_size.y);
            this.RenderMap(offset_x,offset_y);
            this.RenderBodies(offset_x,offset_y);
        }
        batch.end();
        if(manager.video_player != null){
            try {
                manager.video_player.update();
                Texture frame = manager.video_player.getTexture();
                if(frame != null){
                    frame.setFilter(
                        com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest,
                        com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
                    );
                    batch.begin();
                    batch.draw(frame, -240, -125, 480, 250, 0, 1, 1, 0);
                    batch.end();
                }
            }catch (Exception e){
                Main.Debug("Error: "+e);
            }
        }
        this.RenderUI();
        this.frame_buffer.end();

        ScreenUtils.clear(0.0f, 0.0f, 0.0f, 1f);

        batch.setProjectionMatrix(main_interface.ui_viewport.getCamera().combined);
        batch.setShader(manager.GetShader("shaders/passthrough.frag"));
        batch.begin();
        batch.getShader().setUniformf("u_resolution", 480f, 250f);
        batch.getShader().setUniformf("u_time", (float) (milli_time-this.state.shader_anim_start_time)/1000.0f);

        batch.draw(this.frame_buffer.getColorBufferTexture(),
            -240, -125,
            480, 250,
            0, 0, 1, 1);
        batch.end();

        batch.setShader(null);
    }
}
