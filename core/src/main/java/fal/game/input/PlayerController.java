package fal.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public interface PlayerController {
    boolean MoveUp();
    boolean MoveDown();
    boolean MoveLeft();
    boolean MoveRight();
    boolean CameraMoveUp();
    boolean CameraMoveDown();
    boolean CameraMoveLeft();
    boolean CameraMoveRight();
    boolean CameraZoomIn();
    boolean CameraZoomOut();
    boolean ChatInteraction();
    boolean ChatHideInteraction();
    boolean MouseInteraction();
    boolean Skip();
}
