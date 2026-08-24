package fal.game.network;

import java.util.Dictionary;

public class DataPackage {
    public String author;
    public Dictionary<String, Object> data;
    public DataPackage(String author, Dictionary data){
        this.author = author;
        this.data = data;
    }
}
