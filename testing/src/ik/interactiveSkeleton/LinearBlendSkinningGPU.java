package ik.interactiveSkeleton;

import frames.core.Node;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.opengl.PShader;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class LinearBlendSkinningGPU {
    public Frame reference = new Frame();
    public Node shape;
    public PShader shader;
    public Quaternion[] boneQuat = new Quaternion[120];
    public float[] bonePositionOrig = new float[120];
    public float[] bonePosition = new float[120];
    float[] boneRotation = new float[120];

    public ArrayList<Node> skeleton;

    public LinearBlendSkinningGPU(Node shape, ArrayList<Node> skeleton) {
        this.shape = shape;
        this.skeleton = skeleton;
    }

    public void setSkinning(PApplet applet, Scene scene){
        shader = applet.loadShader(applet.sketchPath() + "/testing/src/ik/interactiveSkeleton/frag.glsl",
                applet.sketchPath() + "/testing/src/ik/interactiveSkeleton/skinning.glsl");
        int i = 0, j = 0;
        for(Node node : skeleton){
            Vector position;
            boneQuat[j++] = node.orientation().get();
            position = shape.coordinatesOfFrom(new Vector(), node);
            bonePositionOrig[i+0] = position.x();
            bonePositionOrig[i+1] = position.y();
            bonePositionOrig[i+2] = position.z();
            bonePosition[i++] = position.x();
            bonePosition[i++] = position.y();
            bonePosition[i++] = position.z();

        }
        shader.set("bonePositionOrig", bonePositionOrig);
        shader.set("bonePosition", bonePosition);
        shader.set("boneLength", i);

    }

    public void updateParams(){
        int i = 0, j = 0, k =0;
        for(Node node : skeleton){
            Vector position;
            Quaternion rotation;
            position = shape.coordinatesOfFrom(new Vector(), node);
            bonePosition[i++] = position.x();
            bonePosition[i++] = position.y();
            bonePosition[i++] = position.z();
            rotation = Quaternion.compose(boneQuat[k++].inverse(),node.orientation());
            boneRotation[j++] = rotation.x();
            boneRotation[j++] = rotation.y();
            boneRotation[j++] = rotation.z();
            boneRotation[j++] = rotation.w();
        }
        shader.set("bonePosition", bonePosition);
        shader.set("boneRotation", boneRotation);
    }
}
