package fal.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import fal.game.network.CSConnection;

public class Client {
    private final String debug_prefix;
    private Texture image;
    private SpriteBatch batch;
    private CSConnection active_connection;
    public String id;
    private void Debug(String text){
        Main.Debug(this.debug_prefix+text);
    }
    public Client(String id){
        this.debug_prefix = String.format("[CLIENT:%s] ",id);
        Debug(String.format("Init client [%s]...",id));

        this.id = id;
        this.active_connection = null;

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
    public void Render(){
        Debug("  - Render");
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
