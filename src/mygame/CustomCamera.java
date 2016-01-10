package mygame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl;

/**
 * @author Patrick Shen This class allows for a third-person view of the player,
 * instead of the default first-person view through the player.
 */
public class CustomCamera {

    private Node pivot;
    private CameraNode cameraNode;
    /**
     *
     */
    public final float MAX_VERT = 85 * FastMath.DEG_TO_RAD;
    /**
     *
     */
    public final float MIN_VERT = 5 * FastMath.DEG_TO_RAD;
    /**
     *
     */
    public final float FOLLOW_DISTANCE = 6;
    /**
     *
     */
    public float vertAngle = 30 * FastMath.DEG_TO_RAD;

    /**
     * Contructor for CustomCamera
     *
     * @param name
     * @param cam
     * @param player
     */
    public CustomCamera(String name, Camera cam, Node player) {
        //attach a node to the player on which the camera can pivot, to prevent the camera from pivoting the player itself
        pivot = new Node("pivot");
        player.attachChild(pivot);
        //attach camera to the node
        cameraNode = new CameraNode(name, cam);
        cameraNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        pivot.attachChild(cameraNode);
        //set location and direction the camera is looking at
        pivot.setLocalTranslation(0, 1.5f, 0);
        cameraNode.setLocalTranslation(new Vector3f(0, 0, FOLLOW_DISTANCE));
        cameraNode.lookAt(pivot.getLocalTranslation(), Vector3f.UNIT_Y);
        pivot.getLocalRotation().fromAngleAxis(-vertAngle, Vector3f.UNIT_X);
    }

    /**
     * handles rotation of the camera up and down by restricting its motion
     * @param angle 
     */
    public void verticalRotate(float angle) {
        vertAngle += angle;

        if (vertAngle > MAX_VERT) {
            vertAngle = MAX_VERT;
        } else if (vertAngle < MIN_VERT) {
            vertAngle = MIN_VERT;
        }
        pivot.getLocalRotation().fromAngleAxis(-vertAngle, Vector3f.UNIT_X);
    }

    /**
     * accessor method for the camera
     * @return 
     */
    public CameraNode getCameraNode() {
        return cameraNode;
    }

    /**
     * accessor method for the node on which the camera is attached
     * @return 
     */
    public Node getCameraTrack() {
        return pivot;
    }
}