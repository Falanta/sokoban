package fal.game.world;

import static fal.game.Main.Debug;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;

public class LevelMap {
    public static class Cell {
        public String type;
        public Vector2 pos;
        public ArrayList<Body> content;
        public Cell(String type,short x, short y){
            this.type = type;
            this.pos = new Vector2(x,y);
        }
    }
    public String name;
    public ArrayList<ArrayList<Cell>> matrix;
    public Vector2 size;
    public boolean loaded = false;
    public LevelMap(String name){
        this.name = name;
        this.matrix = null;
        this.size = null;
    }
    public void GenerateEmpty(Vector2 size){
        this.matrix = new ArrayList<>();
        this.size = size;
        for(short y = 0; y < size.y; y+=1){
            this.matrix.add(new ArrayList<Cell>());
            for(short x = 0; x < size.x; x+=1){
                this.matrix.get(y).add(null);
            }
        }
    }
    public void Set(Vector2 pos,Cell value){
        this.matrix.get((short)pos.y).set((short)pos.x,value);
    }
    public Cell Get(short x, short y){
        if(x < 0 || y < 0 || x >= this.size.x || y >= this.size.y){return null;}
        return this.matrix.get(y).get(x);
    }
    public void PrintMatrix(){
        Debug(" - Print map "+this.name);
        Debug(String.format(",%s,",String.join("", java.util.Collections.nCopies((short)this.size.x, "-"))));
        for(ArrayList<Cell> row: this.matrix){
            String row_out = "";
            for(Cell cell: row){
                if(cell != null){
                    row_out += "#";
                }else{
                    row_out += " ";
                }
            }
            Debug(String.format("|%s|",row_out));
        }
        Debug(String.format("'%s'",String.join("", java.util.Collections.nCopies((short)this.size.x, "-"))));
    }
    public ArrayList<ArrayList<Cell>> MatrixCopy(){
        ArrayList<ArrayList<Cell>> copy = new ArrayList<>();
        for(ArrayList<Cell> row: this.matrix){
            ArrayList<Cell> row_copy = new ArrayList<>();
            for(Cell cell: row){
                if(cell == null) {
                    row_copy.add(null);
                }else{
                    row_copy.add(new Cell(cell.type,(short)cell.pos.x,(short)cell.pos.y));
                }
            }
            copy.add(row_copy);
        }
        return copy;
    }
    public void LoadFromFile(FileHandle file) {
        Debug("Load map from file: "+file.name());
        String[] lines = file.readString("UTF-8").split("\\r?\\n");
        if (lines.length < 2) return;

        JsonReader jsonReader = new JsonReader();
        JsonValue root = jsonReader.parse(lines[0]);

        JsonValue sizeArr = root.get("size");
        float width = sizeArr.get(0).asFloat();
        float height = sizeArr.get(1).asFloat();
        GenerateEmpty(new Vector2(width, height));
        JsonValue assets = root.get("assets");
        short y = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (y >= height) break;

            for (short x = 0; x < line.length() && x < width; x++) {
                char c = line.charAt(x);
                String symbolKey = String.valueOf(c);

                if(symbolKey.equals(" ")){
                    Set(new Vector2(x, y), null);
                }else if (assets.has(symbolKey)) {
                    Cell cell = new Cell(assets.getString(symbolKey), x, y);
                    Set(new Vector2(x, y), cell);
                } else {
                    Set(new Vector2(x, y), new Cell("unknown",x,y));
                }
            }
            y++;
        }
    }
}
