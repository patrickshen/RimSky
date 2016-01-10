package mygame;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 * @author Patrick Shen The implementation of this controller class is seen in
 * the xml file for the NiftyGUI. It allows Java to interact with the GUI and
 * progress the GUI from screen to screen (i.e. from "start" to "hud")
 */
public class MyStartScreen extends AbstractAppState implements ScreenController {

    private Nifty nifty;
    private Application app;
    private Screen screen;

    /**
     *
     * @param nextScreen
     */
    public void startGame(String nextScreen) {
        nifty.gotoScreen(nextScreen);
    }

    /**
     *
     */
    public void quitGame() {
        app.stop();
    }

    /**
     * Nifty GUI ScreenControl methods
     * @param nifty
     * @param screen  
     */
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    /**
     *
     */
    @Override
    public void onStartScreen() {
        // inherited, unused method
    }

    /**
     *
     */
    @Override
    public void onEndScreen() {
        //inherited, unused method
    }

    /**
     *
     * @param stateManager
     * @param app
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = app;
    }

    @Override
    public void update(float tpf) {
        //inherited, unused method
    }
}