package mygame;

import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Spatial;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;

/**
 *
 * @author Patrick Shen
 */
public class Main extends SimpleApplication {

    private final int TARGET_HEALTH = 10;
    private final String SCENE_LOCATION = "Scenes/newScene.j3o";
    private final String TARGET_LOCATION = "Models/TowerNew/Tower.j3o";
    private final Vector3f PLAYER_LOCATION = new Vector3f(-30f, 2f, 60f);
    private final float COOLDOWN_TIME = 0.9f;
    private final int HEALTH_BAR_WIDTH = 40;
    private RigidBodyControl scene;
    private AnimControl animControl;
    private BulletAppState bulletAppState;
    private Spatial sceneModel;
    private Spatial playerModel;
    private Spatial targetModel;
    private Node shootables;
    private Node shootingNode = new Node("Shooting");
    private CustomNode player;
    private Vector3f direction;
    private int counter;
    private float timeCount;
    private MyStartScreen startScreen;
    private NiftyJmeDisplay niftyDisplay;
    private BitmapText info;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        Main app = new Main();
        app.start();
    }

    /**
     *
     */
    @Override
    /**
     * Acts as a pseudo-contructor which main method calls behind-the-scenes.
     */
    public void simpleInitApp() {
        //used for keeping track of skill cooldown to prevent spamming
        timeCount = 0;
        //attaches everything that can be shot on a custom Node, not the rootNode.  
        //this is an optimization that prevents the engine from checking for collisions with every single Spatial in the game.
        shootables = new Node("Shootables");

        mouseInput.setCursorVisible(true);
        bulletAppState = new BulletAppState(); //sets up physics
        stateManager.attach(bulletAppState);
        setDisplayStatView(false);  //remove debug information
        flyCam.setDragToRotate(true);

        //helper methods
        initScene(SCENE_LOCATION);
        initCrossHairs();
        initDoom();
        initKeys();
        initTarget(TARGET_LOCATION);
        initPrompt("Tower HP");
        setUpHPBar();
        initGUI();

        //adds water and light to the scene
        FilterPostProcessor processor = (FilterPostProcessor) assetManager.loadAsset("Water.j3f");
        viewPort.addProcessor(processor);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(1f, -1f, -1f));
        rootNode.addLight(sun);

        //makes the spatials visible
        rootNode.attachChild(shootables);
        shootables.attachChild(targetModel);
        shootables.attachChild(sceneModel);
    }

    /**
     *
     * @param tpf
     */
    @Override
    /**
     * Updates the game frame. tpf is a unit dependent on the framerate of the
     * render.
     */
    public void simpleUpdate(float tpf) {
        player.update();
        timeCount += tpf;
        //move the shootingnode projectile in the direction that the player shot
        shootingNode.move(direction.getX() * 100 * tpf, direction.getY() * 100 * tpf, direction.getZ() * 100 * tpf);
        // counter keeps track of the number of times the tower has been hit
        if (counter >= TARGET_HEALTH) {
            shootables.detachChild(targetModel);
            initPrompt("You win! Exit game please.");
        }
        updateHPBar();
    }

    /**
     *
     * @param rm
     */
    @Override
    public void simpleRender(RenderManager rm) {
        //unused, is an inherited method that is used only in advanced games
    }

    /**
     * Declares actions and tags them to triggers.
     */
    private void initKeys() {
        //this is not mapped to the CustomNode class to reduce coupling
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT)); // trigger 2: left-button click
        inputManager.addListener(actionListener, "Shoot");
    }
    /**
     * Handles logic for actions when they are triggered.
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (timeCount >= COOLDOWN_TIME) { //only trigger when the skill is off cooldown
                if (name.equals("Shoot") && !keyPressed) {
                    //cast a vector from the camera's location to the camera's direction, and store collisions between the shootables node and the vector 
                    CollisionResults results = new CollisionResults();
                    Ray cameraRay = new Ray(cam.getLocation(), cam.getDirection());
                    shootables.collideWith(cameraRay, results);
                    // if a collision is detected, set the direction of the spell towards the location of collision
                    if (results.size() > 0) {
                        CollisionResult closest = results.getClosestCollision();
                        //vector subtraction that creates resultant vector from the player spatial to the point of collision
                        direction = new Vector3f(closest.getContactPoint().getX() - player.getLocalTranslation().getX(), closest.getContactPoint().getY() - player.getLocalTranslation().getY(), closest.getContactPoint().getZ() - player.getLocalTranslation().getZ()).normalize();
                        //increase number of hits on the tower
                        counter += 1;

                    } else {
                        //direction is simply the direction that the player spatial is facing
                        direction = new Vector3f(player.getViewDirection().getX() * -1f, player.getViewDirection().getY() * -1f, player.getViewDirection().getZ() * -1f).normalize();
                    }
                    //everytime a player shoots a spell, it is a completely new instance
                    initSpell();
                    rootNode.attachChild(shootingNode);
                    shootingNode.setLocalTranslation(player.getLocalTranslation());

                    timeCount = 0; // reset cooldown when the skill is triggered
                }
            }
        }
    };

    /**
     * Loads the scene (terrain) from the location of the terrain asset into the
     * game.
     */
    private void initScene(String fileLocation) {
        sceneModel = assetManager.loadModel(fileLocation);
        sceneModel.scale(1f, 1f, 1f);
        // makes the scene solid
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        scene = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(scene);
        bulletAppState.getPhysicsSpace().add(scene);
    }

    /**
     * Loads the Doctor Doom model into the game. This method cannot be
     * generalised because every animated model's animControl class location
     * differs.
     */
    private void initDoom() {
        playerModel = assetManager.loadModel("Models/FinalDoomModel/DrDoom.j3o");
        //this is the specific location of the animcontrol class for this model.
        animControl = ((Node) ((Node) ((Node) ((Node) playerModel).getChild("Armature")).getChild("DrDoom")).getChild("drdoomclassiccape-entity")).getChild("drdoomclassiccape-ogremesh").getControl(AnimControl.class);

        player = new CustomNode(animControl, playerModel, inputManager, cam);
        player.getCharacterControl().setPhysicsLocation(PLAYER_LOCATION);
        rootNode.attachChild(player);
        bulletAppState.getPhysicsSpace().add(player);
        direction = player.getViewDirection();
    }

    /**
     * Paints a crosshair onto the screen to aid players with aiming.
     */
    private void initCrossHairs() {
        setDisplayStatView(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        //center the crosshair
        ch.setLocalTranslation(settings.getWidth() / 2 - ch.getLineWidth() / 2, settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    /**
     * Creates the two parts of a spell: the ball and the surrounding particles.
     */
    public void initSpell() {
        Geometry ballGeo = new Geometry("ball", new Sphere(32, 32, 0.4f, true, false));
        ballGeo.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        shootingNode.attachChild(ballGeo);
        //ParticleEmitter methods are documented in JME3's own documentation, but this essentially sets up a flame-like effect
        ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        fire.setMaterial(mat_red);
        fire.setImagesX(2);
        fire.setImagesY(2);
        fire.setEndColor(new ColorRGBA(ColorRGBA.Green));
        fire.setStartColor(new ColorRGBA(ColorRGBA.Yellow));
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 1, 0));
        fire.setStartSize(1.5f);
        fire.setEndSize(0.1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);
        fire.setLocalTranslation(0, 0, 0);
        shootingNode.attachChild(fire);
    }

    /**
     * Loads the target from the target asset location into the game.
     * @param fileLocation 
     */
    protected void initTarget(String fileLocation) {
        targetModel = assetManager.loadModel(fileLocation);
        targetModel.scale(3f);
        targetModel.setLocalTranslation(20f, 2f, -79f);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-5f, -0.7f, -5.0f));
        targetModel.addLight(sun);
    }

    /**
     * Paints a message on the GUI below the health bar.
     */
    private void initPrompt(String prompt) {
        if (info != null) { //prevent nullpointerexception
            guiNode.detachChild(info);
        }
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        info = new BitmapText(guiFont, false);
        info.setSize(guiFont.getCharSet().getRenderedSize());
        info.setText(prompt);
        // centers the message horizontally
        info.setLocalTranslation(settings.getWidth() / 2 - info.getLineWidth() / 2, settings.getHeight() / 5 * 4 - info.getLineHeight(), 0);
        guiNode.attachChild(info);
    }

    /**
     * Paints a green rectangle that represents the health bar.
     */
    private void setUpHPBar() {
        Material mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(ColorRGBA.Green));
        //the size of the healthbar is dependent on the tower's health
        Geometry greenPart = new Geometry("greenPart", new Quad(HEALTH_BAR_WIDTH * TARGET_HEALTH, HEALTH_BAR_WIDTH));
        greenPart.setMaterial(mat);
        //centers the healthbar horizontally
        greenPart.setLocalTranslation(settings.getWidth() / 2 - HEALTH_BAR_WIDTH * TARGET_HEALTH / 2, settings.getHeight() / 5 * 4, 0);
        guiNode.attachChild(greenPart);
    }

    /**
     * Creates a NiftyGUI overlay.
     */
    private void initGUI() {
        startScreen = new MyStartScreen();
        stateManager.attach(startScreen);

        niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        guiViewPort.addProcessor(niftyDisplay);
        //the NiftyGUI is based on a custom xml file
        nifty.fromXml("Interface/screen3.xml", "start", startScreen);
        nifty.setIgnoreKeyboardEvents(true);
    }

    /**
     * Paints a red rectangle that represents loss of health in the health bar.
     */
    private void updateHPBar() {
        Material mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(ColorRGBA.Red));
        //loss of health is dependent on counter
        Geometry redPart = new Geometry("redPart", new Quad(HEALTH_BAR_WIDTH * counter, HEALTH_BAR_WIDTH));
        redPart.setMaterial(mat);
        //aligns the red bar with the green bar
        redPart.setLocalTranslation(settings.getWidth() / 2 - HEALTH_BAR_WIDTH * TARGET_HEALTH / 2, settings.getHeight() / 5 * 4, 0);
        guiNode.attachChild(redPart);
    }
}