package fal.game;

import com.badlogic.gdx.ApplicationAdapter;

import java.time.format.DateTimeFormatter;

import fal.game.input.KeyboardControl;
import fal.game.network.CSConnection;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public static Server main_server;
    public Client player_client;
    public CSConnection single_connection;
    public static final DateTimeFormatter time_formatter = DateTimeFormatter.ofPattern("y-M-d.HH-mm-ss");
    public static final DateTimeFormatter statistic_timer = DateTimeFormatter.ofPattern("y-M-d.HH-mm-ss-SS");
    public static void Debug(String text){
        System.out.println(text);
    }
    public static CSConnection ConnectMe(Client player){
        return new CSConnection(player,main_server);
    }

    @Override
    public void create() {
        main_server = new Server("main");
        Thread main_server_thread = new Thread(main_server);
        main_server_thread.start();
        player_client = new Client("", new KeyboardControl());

        //single_connection = new CSConnection(player_client,main_server);
    }

    @Override
    public void render() {
        player_client.Tick(true);
    }

    @Override
    public void dispose() {
        Debug("Shut down...");
        main_server.Delete();
        player_client.Delete();
    }
}
