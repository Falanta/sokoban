package fal.game.world;

import static fal.game.Main.Debug;

import com.badlogic.gdx.math.Vector2;

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
    public Cell Get(Vector2 pos){
        return this.matrix.get((short)pos.y).get((short)pos.x);
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
}
