package fal.game.network;

import java.util.Dictionary;
import java.util.Map;

public class DataPackage {
    public String author;
    public Map<String, Object> data;
    public DataPackage(String author, Map data){
        this.author = author;
        this.data = data;
    }
}
