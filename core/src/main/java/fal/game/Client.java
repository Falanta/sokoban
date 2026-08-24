package fal.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.Map;

import fal.game.network.CSConnection;
import fal.game.network.DataPackage;

public class Client {
    private final String debug_prefix;
    private Texture image;
    private SpriteBatch batch;
    private CSConnection active_connection;
    private short last_tps;
    public String id;
    public Map last_data;
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Client(String id){
        this.debug_prefix = String.format("[CLIENT:%s] ",id);
        Debug(String.format("Init client [%s]...",id));

        this.id = id;
        this.active_connection = null;

        this.last_tps = -1;
        this.last_data = null;
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        Debug("Done!");
    }
    public void Connect(CSConnection connection,String server_id){
        Debug(String.format("Joining: %s...",server_id));
        this.active_connection = connection;

        Debug("Done!");
    }
    public void Leave(){
        Debug(String.format("Leaving: %s...",active_connection.server.id));
        this.active_connection.Delete("leave");
    }
    public void DeleteConnection(){
        Debug(String.format("Connection deleted: %s",active_connection.server.id));
        this.active_connection = null;
    }
    public void PullData(){
        Debug("    - Pull Data");
        DataPackage input_package = this.active_connection.SCQueue.poll();
        DataPackage latest_package = input_package;
        if(input_package != null){
            while((input_package = this.active_connection.SCQueue.poll()) != null){
                latest_package = input_package;
            }
            this.last_data = latest_package.data;
            Debug("      - "+last_data.toString());
        }else{
            Debug("      - There is no package");
        }
    }
    public Object ReadData(String id){
        if(this.last_data != null){
            if(this.last_data.containsKey(id)){
                return this.last_data.get(id);
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
    public void Render(){
        Debug("  - Render");
        this.PullData();

        int actual_fps = Gdx.graphics.getFramesPerSecond();
        this.last_tps = (short) ((this.ReadData("tps") == null)?(short)-1:this.ReadData("tps"));

        Debug("    - FPS: "+actual_fps);
        Debug("    - TPS: "+this.last_tps);

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }
    public Client Delete(){
        Debug("Deleting...");

        if(active_connection != null && active_connection.server != null) {
            Debug("- Leaving " + active_connection.server.id);
            try {
                this.Leave();
            } catch (Exception e) {
                Debug("Error: " + e);
            }
        }

        this.batch.dispose();
        this.image.dispose();

        Debug("Done!");
        return null;
    }
}
