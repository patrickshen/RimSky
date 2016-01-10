package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * @author Patrick Shen This class is a Node the implements the Third Person
 * Camera.
 */
public class CustomNode extends Node implements ActionListener, AnalogListener, AnimEventListener {

    private CustomCamera camera;
    private Camera cam;
    private Spatial model;
    private CharacterControl characterControl;
    private AnimChannel animChannel;
    private AnimControl animControl;
    private InputManager inputManager;
    private Vector3f walkDirection = new Vector3f();
    private boolean left = false;
    private boolean right = false;
    private boolean up = false;
    private boolean down = false;
    private boolean attack;
    private boolean attacking;
    private boolean rotate;
    private final float WALK_SPEED = 0.1f;
    private final float MOUSE_LOOK_SPEED = FastMath.PI;
    private final float JUMP_SPEED = 20f;
    private final float GRAVITY = 20f;
    private final float STEP_SIZE = .05f;
    private final String IDLE_ANIM = "Idle";
    private final String WALK_ANIM = "Walk";
    private final String ATTACK_ANIM = "Attack";
    private final String JUMP_ANIM = "Jump";

    /**
     * Constructor for CustomNode
     * @param animControl
     * @param model 
     * @param inputManager
     * @param cam  
     */
    public CustomNode(AnimControl animControl, Spatial model, InputManager inputManager, Camera cam) {
        super();
        this.cam = cam;
        camera = new CustomCamera("CamNode", cam, this);

        this.model = model;
        this.model.scale(0.5f);
        this.model.rotate(0, 135, 0);
        this.model.setLocalTranslation(0f, -1f, 0f);
        this.attachChild(this.model);

        //implement physics for the model
        CapsuleCollisionShape playerShape = new CapsuleCollisionShape(.5f, 1f);
        characterControl = new CharacterControl(playerShape, STEP_SIZE);
        characterControl.setJumpSpeed(JUMP_SPEED);
        characterControl.setGravity(GRAVITY);
        this.addControl(characterControl);

        //implement animations for the model
        this.animControl = animControl;
        animControl.addListener(this);
        animChannel = animControl.createChannel();
        animChannel.setSpeed(0.05f);
        animChannel.setAnim(IDLE_ANIM);

        this.inputManager = inputManager;
        setUpKeys();
    }

    /**
     * Updates the direction the model walks towards and its animations.
     */
    public void update() {
        Vector3f camDir = cam.getDirection().clone();
        camDir.y = 0;
        Vector3f camLeft = cam.getLeft().clone();
        camLeft.y = 0;
        walkDirection.set(0, 0, 0);

        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }

        characterControl.setWalkDirection(walkDirection.normalize().multLocal(WALK_SPEED));
        handleAnimations();
    }

    /**
     * Helper method that implements logic for switching between animations.
     */
    private void handleAnimations() {
        if (attacking) {
            //waiting for attack animation to finish
        } else if (attack) {
            animChannel.setAnim(ATTACK_ANIM, 0f);
            animChannel.setLoopMode(LoopMode.DontLoop);
            attacking = true;
            attack = false;
        } else if (characterControl.onGround()) {
            if (left || right || up || down) {
                if (!animChannel.getAnimationName().equals(WALK_ANIM)) {
                    animChannel.setAnim(WALK_ANIM, 0f);
                    animChannel.setLoopMode(LoopMode.Loop);
                }
            } else {
                if (!animChannel.getAnimationName().equals(IDLE_ANIM)) {
                    animChannel.setAnim(IDLE_ANIM, 0f);
                    animChannel.setLoopMode(LoopMode.Cycle); //Cycle idle animation
                }
            }
        }
    }

    /* Maps actions to triggers. */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Rotate", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("TurnLeft", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("TurnRight", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MouselookDown", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("MouselookUp", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Rotate");
        inputManager.addListener(this, "Attack");
        inputManager.addListener(this, "TurnLeft");
        inputManager.addListener(this, "TurnRight");
        inputManager.addListener(this, "MouselookDown");
        inputManager.addListener(this, "MouselookUp");
    }

    /**
     * Handles actions once they are triggered.
     * @param binding 
     * @param value 
     */
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Left")) {
            left = value;
        } else if (binding.equals("Right")) {
            right = value;
        } else if (binding.equals("Up")) {
            up = value;
        } else if (binding.equals("Down")) {
            down = value;
        } else if (binding.equals("Jump")) {
            if (characterControl.onGround()) {
                characterControl.jump();
                if (!attacking) {
                    animChannel.setAnim(JUMP_ANIM, 0f);
                    animChannel.setLoopMode(LoopMode.DontLoop);
                }
            }
        } else if (binding.equals("Attack")) {
            attack = value;

        } else if (binding.equals("Rotate")) {
            rotate = value;
        }
    }

    /**
     * Handles camera mouse actions once they are triggered.
     * @param binding 
     */
    public void onAnalog(String binding, float value, float tpf) {
        if (binding.equals("TurnLeft") && rotate) {
            Quaternion turn = new Quaternion();
            turn.fromAngleAxis(MOUSE_LOOK_SPEED * value, Vector3f.UNIT_Y);
            characterControl.setViewDirection(turn.mult(characterControl.getViewDirection()));
        } else if (binding.equals("TurnRight") && rotate) {
            Quaternion turn = new Quaternion();
            turn.fromAngleAxis(-MOUSE_LOOK_SPEED * value, Vector3f.UNIT_Y);
            characterControl.setViewDirection(turn.mult(characterControl.getViewDirection()));
        } else if (binding.equals("MouselookDown") && rotate) {
            camera.verticalRotate(MOUSE_LOOK_SPEED * value);
        } else if (binding.equals("MouselookUp") && rotate) {
            camera.verticalRotate(-MOUSE_LOOK_SPEED * value);
        }
    }

    /**
     * Inherited method. Stops the model from attacking
     * @param control 
     * @param channel
     * @param animName  
     */
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (channel == animChannel && attacking && animName.equals(ATTACK_ANIM)) {
            attacking = false;
        }
    }

    /**
     *
     * @param control
     * @param channel
     * @param animName
     */
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        //inherited unused method
    }

    /**
     * Accessor method for charactercontrol.
     * @return 
     */
    public CharacterControl getCharacterControl() {
        return characterControl;
    }

    /**
     * Accessor method for viewdirection.
     * @return 
     */
    public Vector3f getViewDirection() {
        return characterControl.getViewDirection();
    }
}