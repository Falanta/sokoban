package fal.game.network;

import static fal.game.Main.Debug;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import fal.game.Client;
import fal.game.Server;

public class CSConnection {
    public final ConcurrentLinkedQueue<DataPackage> CSQueue = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<DataPackage> SCQueue = new ConcurrentLinkedQueue<>();
    public Server server;
    public Client client;
    public String server_name;
    public String client_name;
    public CSConnection(Client client, Server server){
        Debug(String.format("Connecting %s & %s(%s)...",server.toString(),client.id,client.toString()));
        this.server = server;
        this.server_name = server.id+"";
        this.client = client;
        this.client_name = client.id+"";
        this.client.Connect(this,this.server_name);
        this.server.Connect(this,this.client_name);
    }
    public void Delete(String reason){
        Debug(String.format("Deleting connection %s & %s(%s); Reason: %s",server.toString(),this.client_name,client.toString(),reason));
        this.client.DeleteConnection(reason);
        this.server.DeleteConnection(this.client_name,reason);
    }
}
