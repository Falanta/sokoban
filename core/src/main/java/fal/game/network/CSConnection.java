package fal.game.network;

import static fal.game.Main.Debug;

import java.util.concurrent.ConcurrentLinkedQueue;

import fal.game.Client;
import fal.game.Server;

public class CSConnection {
    public final ConcurrentLinkedQueue<DataPackage> CSQueue = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<DataPackage> SCQueue = new ConcurrentLinkedQueue<>();
    public Server server;
    public Client client;
    public CSConnection(Client client, Server server){
        Debug(String.format("Connecting %s & %s(%s)...",server.toString(),client.id,client.toString()));
        this.server = server;
        this.client = client;
        this.client.Connect(this,this.server.id);
        this.server.Connect(this,this.client.id);
    }
    public void Delete(String reason){
        Debug(String.format("Deleting connection %s & %s(%s); Reason: %s",server.toString(),client.id,client.toString(),reason));
        this.client.DeleteConnection();
        this.server.DeleteConnection(this.client.id);
    }
}
