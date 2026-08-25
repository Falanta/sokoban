package fal.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class KeyboardControl implements PlayerController {
    @Override
    public boolean MoveUp() {
        return Gdx.input.isKeyPressed(Input.Keys.UP);
    }
    @Override
    public boolean MoveDown() {
        return Gdx.input.isKeyPressed(Input.Keys.DOWN);
    }
    @Override
    public boolean MoveLeft() {
        return Gdx.input.isKeyPressed(Input.Keys.LEFT);
    }
    @Override
    public boolean MoveRight() {
        return Gdx.input.isKeyPressed(Input.Keys.RIGHT);
    }
    @Override
    public boolean CameraMoveUp() {
        return Gdx.input.isKeyPressed(Input.Keys.W);
    }
    @Override
    public boolean CameraMoveDown() {
        return Gdx.input.isKeyPressed(Input.Keys.S);
    }
    @Override
    public boolean CameraMoveLeft() {
        return Gdx.input.isKeyPressed(Input.Keys.A);
    }
    @Override
    public boolean CameraMoveRight() {
        return Gdx.input.isKeyPressed(Input.Keys.D);
    }
    @Override
    public boolean CameraZoomIn() {
        return Gdx.input.isKeyPressed(Input.Keys.Q);
    }
    @Override
    public boolean CameraZoomOut() {
        return Gdx.input.isKeyPressed(Input.Keys.E);
    }
    @Override
    public boolean ChatInteraction() {
        return Gdx.input.isKeyPressed(Input.Keys.ENTER);
    }
    @Override
    public boolean ChatHideInteraction() {
        return Gdx.input.isKeyPressed(Input.Keys.H);
    }
}
