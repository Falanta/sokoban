package fal.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class KeyboardControl extends InputAdapter implements PlayerController {
    public final StringBuilder typedBuffer = new StringBuilder();
    private boolean isChatting = false;
    private boolean mouse_memory = false;
    private boolean mouse_interact = false;
    public void UpdateMouse(){
        mouse_interact = mouse_memory && !Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        mouse_memory = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    public void setChatting(boolean chatting) {
        this.isChatting = chatting;
        if (chatting) {
            typedBuffer.setLength(0); // Очищаем при открытии
        }
    }

    @Override
    public boolean keyTyped(char character) {
        if (isChatting) {
            // Обработка удаления (Backspace)
            if (character == '\b') {
                if (typedBuffer.length() > 0) {
                    typedBuffer.deleteCharAt(typedBuffer.length() - 1);
                }
            }
            // Обычные печатные символы (исключая управляющие)
            else if (character >= 32 && character != 127) {
                typedBuffer.append(character);
            }
        }
        return false;
    }
    @Override public boolean MoveUp() { return Gdx.input.isKeyJustPressed(Input.Keys.UP); }
    @Override public boolean MoveDown() { return Gdx.input.isKeyJustPressed(Input.Keys.DOWN); }
    @Override public boolean MoveLeft() { return Gdx.input.isKeyJustPressed(Input.Keys.LEFT); }
    @Override public boolean MoveRight() { return Gdx.input.isKeyJustPressed(Input.Keys.RIGHT); }
    @Override public boolean CameraMoveUp() { return Gdx.input.isKeyPressed(Input.Keys.W); }
    @Override public boolean CameraMoveDown() { return Gdx.input.isKeyPressed(Input.Keys.S); }
    @Override public boolean CameraMoveLeft() { return Gdx.input.isKeyPressed(Input.Keys.A); }
    @Override public boolean CameraMoveRight() { return Gdx.input.isKeyPressed(Input.Keys.D); }
    @Override public boolean CameraZoomIn() { return Gdx.input.isKeyPressed(Input.Keys.Q); }
    @Override public boolean CameraZoomOut() { return Gdx.input.isKeyPressed(Input.Keys.E); }
    @Override public boolean ChatInteraction() { return Gdx.input.isKeyJustPressed(Input.Keys.ENTER); }
    @Override public boolean ChatHideInteraction() { return Gdx.input.isKeyJustPressed(Input.Keys.H); }
    @Override public boolean MouseInteraction() { return this.mouse_interact; }
}
