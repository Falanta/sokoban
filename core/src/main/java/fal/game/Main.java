package fal.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

import fal.game.network.CSConnection;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public Server main_server;
    public Client player_client;
    public CSConnection single_connection;
    public static void Debug(String text){
        System.out.println(text);
    }

    @Override
    public void create() {
        main_server = new Server("main");
        Thread main_server_thread = new Thread(main_server);
        main_server_thread.start();
        player_client = new Client("player");

        single_connection = new CSConnection(player_client,main_server);
    }

    @Override
    public void render() {
        player_client.Render();
    }

    @Override
    public void dispose() {
        Debug("Shut down...");
        main_server = main_server.Delete();
        player_client = player_client.Delete();
    }
}
