/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.geom;

import remixlab.bias.Agent;
import remixlab.bias.Grabber;
import remixlab.bias.InputHandler;
import remixlab.bias.event.MotionEvent;
import remixlab.primitives.*;
import remixlab.fpstiming.Animator;
import remixlab.fpstiming.TimingHandler;
import remixlab.fpstiming.TimingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A 2D or 3D graph graph.
 * <p>
 * Main package class representing an interface between Dandelion and the outside world.
 * For an introduction to DANDELION please refer to
 * <a href="http://nakednous.github.io/projects/dandelion">this</a>.
 * <p>
 * Instantiated graph {@link Node}s form a graph-tree of
 * transformations which may be traverse with {@link #traverse()}. The node
 * collection belonging to the graph may be retrieved with {@link #nodes(boolean)}. The
 * graph provides other useful routines to handle the hierarchy, such as
 * {@link #pruneBranch(Node)}, {@link #appendBranch(List)},
 * {@link #isNodeReachable(Node)}, {@link #branch(Node, boolean)}, and
 * {@link #clear()}.
 * <p>
 * Each Graph provides the following main object instances:
 * <ol>
 * <li>An {@link #eye()} which represents the 2D/3D controlling object.</li>
 * <li>A {@link #timingHandler()} which control (single-threaded) timing operations. For
 * details please refer to the {@link remixlab.fpstiming.TimingHandler} class.</li>
 * <li>An {@link #inputHandler()} which handles all user input through
 * {@link Agent}s (for details please refer to the
 * {@link InputHandler} class). The {@link #inputHandler()} holds a
 * (default) {@link #motionAgent()} and a (default) {@link #keyAgent()} which should
 * be instantiated by derived classes at construction time.</li>
 * <li>A {@link #matrixHandler()} which handles matrix operations either through the
 * {@link MatrixHandler} or through a third party matrix stack
 * (like it's done with Processing). For details please refer to the
 * {@link MatrixHandler} interface.</li>
 * </ol>
 */
public class Graph {
  // 1. Eye
  protected Frame eye;
  protected long lastEqUpdate;
  protected Vector scnCenter;
  protected float scnRadius;
  protected Vector anchorPnt;
  //boundary eqns
  protected float fpCoefficients[][];
  protected boolean fpCoefficientsUpdate;
  protected Vector normal[];
  protected float dist[];
  // rescale ortho when anchor changes
  private float rapK = 1;
  // Inverse the direction of an horizontal mouse motion. Depends on the
  // projected
  // screen orientation of the vertical axis when the mouse button is pressed.
  public boolean cadRotationIsReversed;
  // handed and screen drawing
  protected boolean rightHanded;
  protected int startCoordCalls;
  // size and dim
  protected int width, height;
  protected boolean twod;
  //fly
  protected Vector scnUpVector;

  // 2. Matrix helper
  protected MatrixHandler matrixHandler;

  // 3. Handlers
  protected TimingHandler tHandler;
  protected InputHandler iHandler;

  // 4. Agents
  protected Agent defMotionAgent, defKeyboardAgent;

  // 5. Graph
  protected List<Node> seeds;
  public int nodeCount;
  static public long frameCount;
  protected final long deltaCount;

  // 6. Display flags
  protected int visualHintMask;
  public final static int AXES = 1 << 0;
  public final static int GRID = 1 << 1;
  public final static int PICKING = 1 << 2;
  //TODO restore
  /*
  public final static int PATHS = 1 << 3;
  public final static int ZOOM = 1 << 4; // prosceneMouse.zoomOnRegion
  public final static int ROTATE = 1 << 5; // prosceneMouse.screenRotate
  */
  // public final static int PUP = 1 << 6;
  // public final static int ARP = 1 << 7;

  /**
   * Enumerates the different visibility states an object may have respect to the eye
   * boundary.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  /**
   * Enumerates the two possible types of Camera.
   * <p>
   * This type mainly defines different camera projection matrix. Many other methods take
   * this Type into account.
   */
  public enum Type {
    PERSPECTIVE, ORTHOGRAPHIC
  }

  private float zNearCoef;
  private float zClippingCoef;
  //TODO fix me
  private Type tp = Type.PERSPECTIVE; // PERSPECTIVE or ORTHOGRAPHIC

  /**
   * Default constructor which defines a right-handed OpenGL compatible Scene with its own
   * {@link MatrixHandler}. The constructor also instantiates
   * the {@link #inputHandler()} and the {@link #timingHandler()}, and sets the AXES and
   * GRID visual hint flags.
   * <p>
   * Third party (concrete) Scenes should additionally:
   * <ol>
   * <li>(Optionally) Define a custom {@link #matrixHandler()}. Only if the target platform
   * (such as Processing) provides its own matrix handling.</li>
   * <li>Call {@link #setEye(Frame)} to set the {@link #eye()}, once it's known if the Scene
   * {@link #is2D()} or {@link #is3D()}.</li>
   * <li>Instantiate the {@link #motionAgent()} and the {@link #keyAgent()} and
   * enable them (register them at the {@link #inputHandler()}) and possibly some other
   * {@link Agent}s as well and .</li>
   * <li>Call {@link #init()} at the end of the constructor.</li>
   * </ol>
   *
   * @see #timingHandler()
   * @see #inputHandler()
   * @see #setMatrixHandler(MatrixHandler)
   * @see #setRightHanded()
   * @see #setVisualHints(int)
   * @see #setEye(Frame)
   */
  public Graph(int w, int h) {
    setWidth(w);
    setHeight(h);

    //order of the following lines matter
    //1st try: working
    /*
    scnUpVector = new Vector(0.0f, 1.0f, 0.0f);
    anchorPnt = new Vector();
    scnCenter = new Vector();
    //setRadius(100);
    //setCenter(new Vector(0.0f, 0.0f, 0.0f));
    setEye(new Frame());
    showAll();
    */

    //2nd try: working
    scnUpVector = new Vector(0.0f, 1.0f, 0.0f);
    anchorPnt = new Vector();
    eye = new Frame();
    setRadius(100);
    setCenter(new Vector(0.0f, 0.0f, 0.0f));
    showAll();

    seeds = new ArrayList<Node>();
    tHandler = new TimingHandler();
    deltaCount = frameCount;
    iHandler = new InputHandler();

    setMatrixHandler(new MatrixHandler(this));
    setRightHanded();
    setVisualHints(AXES | GRID);

    if (is2D()) {
      fpCoefficients = new float[4][3];
      normal = new Vector[4];
      for (int i = 0; i < normal.length; i++)
        normal[i] = new Vector();
      dist = new float[4];
    } else {
      fpCoefficients = new float[6][4];
      normal = new Vector[6];
      for (int i = 0; i < normal.length; i++)
        normal[i] = new Vector();
      dist = new float[6];
    }
    enableBoundaryEquations(false);

    // eye stuff
    //TODO is3D() is missing above
    //if(is3D()) {
      //TODO only 3D
      setType(Type.PERSPECTIVE);
      setZNearCoefficient(0.005f);
      setZClippingCoefficient((float) Math.sqrt(3.0f));

      // TODO Stereo parameters
      //setIODistance(0.062f);
      //setPhysicalDistanceToScreen(0.5f);
      //setPhysicalScreenWidth(0.4f);
      // focusDistance is set from setFieldOfView()

      //computeProjection();
    //}
  }

  // dimensions

  /**
   * Returns the {@link #width()} to {@link #height()} aspect ratio of the display window.
   */
  public float aspectRatio() {
    return (float) width() / (float) height();
  }

  /**
   * @return width of the screen window.
   */
  public int width() {
    return width;
  }

  /**
   * @return height of the screen window.
   */
  public int height() {
    return height;
  }

  /**
   * Sets eye {@link #width()} and {@link #height()} (expressed in pixels).
   * <p>
   * Non-positive dimension are silently replaced by a 1 pixel value to ensure boundary
   * coherence.
   */
  public void setWidth(int w) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    if ((w != width()))
      modified();
    width = w > 0 ? w : 1;
  }

  public void setHeight(int h) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    if (h != height())
      modified();
    height = h > 0 ? h : 1;
  }

  //TODO missing
  public void modified() {}

  /**
   * Returns the up vector used in {@link Node#moveForward(MotionEvent)} in which horizontal
   * displacements of the motion device (e.g., mouse) rotate the {@link #eye()} around this
   * vector. Vertical displacements rotate always around the {@link #eye()} {@code X} axis.
   * <p>
   * This value is also used within {@link Node#rotateCAD(MotionEvent)} to define the up
   * vector (and incidentally the 'horizon' plane) around which the {@link #eye()} will
   * rotate.
   * <p>
   * Default value is (0,1,0), but it is updated by the Eye when set as its
   * {@link Graph#eye()} and
   * {@link Graph#setUpVector(Vector)} modify this value and should be
   * used instead.
   */
  public Vector sceneUpVector() {
    return scnUpVector;
  }

  /**
   * Sets the {@link #sceneUpVector()}, defined in the world coordinate system.
   * <p>
   * Default value is (0,1,0), but it is updated by the {@link #eye() when performing
   * {@link Node#moveForward(MotionEvent)} or {@link Node#drive(MotionEvent)}
   * actions.
   */
  public void setSceneUpVector(Vector up) {
    scnUpVector = up;
  }

  /**
   * This method will be called by the Eye when its orientation is changed, so that the
   * {@link #sceneUpVector()} is changed accordingly. You should not need to call this
   * method.
   */
  final void updateSceneUpVector() {
    scnUpVector = eye().orientation().rotate(new Vector(0.0f, 1.0f, 0.0f));
  }

  // 1. type

  /**
   * Returns the Camera.Type.
   * <p>
   * Set by {@link #setType(Type)}.
   * <p>
   * A {@link Type#PERSPECTIVE} Camera uses a classical
   * projection mainly defined by its {@link #fieldOfView()}.
   * <p>
   * With a {@link Type#ORTHOGRAPHIC} {@link #type()}, the
   * {@link #fieldOfView()} is meaningless and the width and height of the Camera frustum
   * are inferred from the distance to the {@link #anchor()} using
   * {@link #getBoundaryWidthHeight()}.
   * <p>
   * Both types use {@link #zNear()} and {@link #zFar()} (to define their clipping planes)
   * and {@link #aspectRatio()} (for frustum shape).
   */
  public final Type type() {
    return tp;
  }

  /**
   * Defines the Camera {@link #type()}.
   * <p>
   * Changing the Camera Type alters the viewport and the objects' size can be changed.
   * This method guarantees that the two frustum match in a plane normal to
   * {@link #viewDirection()}, passing through the arcball reference point.
   */
  public final void setType(Type type) {
    if (type != type()) {
      modified();
      this.tp = type;
    }
  }

  /**
   * Returns the vertical field of view of the Camera (in radians) computed as
   * {@code 2.0f * (float) Math.atan(frame().magnitude())}.
   * <p>
   * Value is set using {@link #setFieldOfView(float)}. Default value is pi/3 radians.
   * This value is meaningless if the Camera {@link #type()} is
   * {@link Type#ORTHOGRAPHIC}.
   * <p>
   * The field of view corresponds the one used in {@code gluPerspective} (see manual). It
   * sets the Y (vertical) aperture of the Camera. The X (horizontal) angle is inferred
   * from the window aspect ratio (see {@link #aspectRatio()} and
   * {@link #horizontalFieldOfView()}).
   * <p>
   * Use {@link #setFOVToFitScene()} to adapt the {@link #fieldOfView()} to a given graph.
   *
   * @see #setFieldOfView(float)
   */
  public float fieldOfView() {
    return 2.0f * (float) Math.atan(eye().magnitude());
  }

  /**
   * Changes the Camera {@link #fieldOfView()} so that the entire graph (defined by
   * {@link #center()} and
   * {@link #radius()} is visible from the Camera
   * {@link Node#position()}.
   * <p>
   * The eye position and orientation of the Camera are not modified and
   * you first have to orientate the Camera in order to actually see the graph (see
   * {@link #lookAt(Vector)}, {@link #showAll()} or {@link #fitBall(Vector, float)}).
   * <p>
   * This method is especially useful for <i>shadow maps</i> computation. Use the Camera
   * positioning tools ({@link #lookAt(Vector)}) to position a
   * Camera at the light position. Then use this method to define the
   * {@link #fieldOfView()} so that the shadow map resolution is optimally used:
   * <p>
   * {@code // The light camera needs size hints in order to optimize its
   * fieldOfView} <br>
   * {@code lightCamera.setSceneRadius(sceneRadius());} <br>
   * {@code lightCamera.setSceneCenter(sceneCenter());} <br>
   * {@code // Place the light camera} <br>
   * {@code lightCamera.setPosition(lightFrame.position());} <br>
   * {@code lightCamera.lookAt(sceneCenter());} <br>
   * {@code lightCamera.setFOVToFitScene();} <br>
   * <p>
   * <b>Attention:</b> The {@link #fieldOfView()} is clamped to M_PI/2.0. This happens
   * when the Camera is at a distance lower than sqrt(2.0) * sceneRadius() from the
   * sceneCenter(). It optimizes the shadow map resolution, although it may miss some
   * parts of the graph.
   */
  public void setFOVToFitScene() {
    if (distanceToSceneCenter() > (float) Math.sqrt(2.0f) * radius())
      setFieldOfView(2.0f * (float) Math.asin(radius() / distanceToSceneCenter()));
    else
      setFieldOfView((float) Math.PI / 2.0f);
  }

  /**
   * Sets the vertical {@link #fieldOfView()} of the Camera (in radians). The
   * {@link #fieldOfView()} is encapsulated as the camera
   * {@link Frame#magnitude()} using the following expression:
   * {@code frame().setMagnitude((float) Math.tan(fov / 2.0f))}.
   *
   * @see #fieldOfView()
   */
  public void setFieldOfView(float fov) {
    eye().setMagnitude((float) Math.tan(fov / 2.0f));
  }

  /**
   * Returns the horizontal field of view of the Camera (in radians).
   * <p>
   * Value is set using {@link #setHorizontalFieldOfView(float)} or
   * {@link #setFieldOfView(float)}. These values are always linked by:
   * {@code horizontalFieldOfView() = 2.0 * atan ( tan(fieldOfView()/2.0) * aspectRatio() )}
   * .
   */
  public float horizontalFieldOfView() {
    // return 2.0f * (float) Math.atan((float) Math.tan(fieldOfView() / 2.0f) *
    // aspectRatio());
    return 2.0f * (float) Math.atan((eye() == null ? 1 : eye().magnitude() )* aspectRatio());
  }

  /**
   * Sets the {@link #horizontalFieldOfView()} of the Camera (in radians).
   * <p>
   * {@link #horizontalFieldOfView()} and {@link #fieldOfView()} are linked by the
   * {@link #aspectRatio()}. This method actually calls
   * {@code setFieldOfView(( 2.0 * atan (tan(hfov / 2.0) / aspectRatio()) ))} so that a
   * call to {@link #horizontalFieldOfView()} returns the expected value.
   */
  public void setHorizontalFieldOfView(float hfov) {
    setFieldOfView(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()));
  }

  /**
   * Returns the near clipping plane distance used by the Camera projection matrix in
   * graph (world) units.
   * <p>
   * The clipping planes' positions depend on the {@link #radius()} and
   * {@link #center()} rather than being fixed small-enough and large-enough values.
   * A good graph dimension approximation will hence result in an optimal precision of the
   * z-buffer.
   * <p>
   * The near clipping plane is positioned at a distance equal to
   * {@link #zClippingCoefficient()} * {@link #radius()} in front of the
   * {@link #center()}: {@code distanceToSceneCenter() -
   * zClippingCoefficient() * sceneRadius()}
   * <p>
   * In order to prevent negative or too small {@link #zNear()} values (which would
   * degrade the z precision), {@link #zNearCoefficient()} is used when the Camera is
   * inside the {@link #radius()} sphere:
   * <p>
   * {@code zMin = zNearCoefficient() * zClippingCoefficient() * sceneRadius();} <br>
   * {@code zNear = zMin;}<br>
   * {@code // With an ORTHOGRAPHIC type, the value is simply clamped to 0.0} <br>
   * <p>
   * See also the {@link #zFar()}, {@link #zClippingCoefficient()} and
   * {@link #zNearCoefficient()} documentations.
   * <p>
   * If you need a completely different zNear computation, overload the {@link #zNear()}
   * and {@link #zFar()} methods.
   * <p>
   * <b>Attention:</b> The value is always positive although the clipping plane is
   * positioned at a negative z value in the Camera coordinate system. This follows the
   * {@code gluPerspective} standard.
   *
   * @see #zFar()
   */
  public float zNear() {
    float z = distanceToSceneCenter() - zClippingCoefficient() * radius();

    // Prevents negative or null zNear values.
    final float zMin = zNearCoefficient() * zClippingCoefficient() * radius();
    if (z < zMin)
      switch (type()) {
        case PERSPECTIVE:
          z = zMin;
          break;
        case ORTHOGRAPHIC:
          z = 0.0f;
          break;
      }
    return z;
  }

  /**
   * Returns the far clipping plane distance used by the Camera projection matrix in graph
   * (world) units.
   * <p>
   * The far clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * sceneRadius()} behind the {@link #center()}:
   * <p>
   * {@code zFar = distanceToSceneCenter() + zClippingCoefficient()*sceneRadius()}
   *
   * @see #zNear()
   */
  public float zFar() {
    return distanceToSceneCenter() + zClippingCoefficient() * radius();
  }

  /**
   * Returns the eye position to {@link #center()} distance in Scene
   * units.
   * <p>
   * 3D Cameras return the projected eye position to {@link #center()}
   * distance along the Camera Z axis and use it in
   * {@link #zNear()} and
   * {@link #zFar()} to optimize the Z range.
   */
  public float distanceToSceneCenter() {
    Vector zCam = eye().zAxis();
    Vector cam2SceneCenter = Vector.subtract(eye().position(), center());
    return Math.abs(Vector.dot(cam2SceneCenter, zCam));
  }

  /**
   * Returns the coefficient which is used to set {@link #zNear()} when the Camera is
   * inside the sphere defined by {@link #center()} and
   * {@link #zClippingCoefficient()} * {@link #radius()}.
   * <p>
   * In that case, the {@link #zNear()} value is set to
   * {@code zNearCoefficient() * zClippingCoefficient() * sceneRadius()}. See the
   * {@code zNear()} documentation for details.
   * <p>
   * Default value is 0.005, which is appropriate for most applications. In case you need
   * a high dynamic ZBuffer precision, you can increase this value (~0.1). A lower value
   * will prevent clipping of very close objects at the expense of a worst Z precision.
   * <p>
   * Only meaningful when Camera type is PERSPECTIVE.
   */
  public float zNearCoefficient() {
    return zNearCoef;
  }

  /**
   * Sets the {@link #zNearCoefficient()} value.
   */
  public void setZNearCoefficient(float coef) {
    if (coef != zNearCoef)
      modified();
    zNearCoef = coef;
  }

  /**
   * Returns the coefficient used to position the near and far clipping planes.
   * <p>
   * The near (resp. far) clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * sceneRadius()} in front of (resp. behind) the
   * {@link #center()}. This guarantees an optimal use of the z-buffer range and
   * minimizes aliasing. See the {@link #zNear()} and {@link #zFar()} documentations.
   * <p>
   * Default value is square root of 3.0 (so that a cube of size 2*{@link #radius()}
   * is not clipped).
   * <p>
   * However, since the {@link #radius()} is used for other purposes (see
   * showEntireScene(), flySpeed(), ...) and you may want to change this value to define
   * more precisely the location of the clipping planes. See also
   * {@link #zNearCoefficient()}.
   */
  public float zClippingCoefficient() {
    return zClippingCoef;
  }

  /**
   * Sets the {@link #zClippingCoefficient()} value.
   */
  public void setZClippingCoefficient(float coef) {
    if (coef != zClippingCoef)
      modified();
    zClippingCoef = coef;
  }

  /**
   * Convenience function that simply returns {@code getOrthoWidthHeight(new
   * float[2])}.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  public float[] getBoundaryWidthHeight() {
    return getBoundaryWidthHeight(new float[2]);
  }

  /**
   * Fills in {@code target} with the {@code halfWidth} and {@code halfHeight} of the eye
   * boundary and returns it. While {@code target[0]} holds {@code halfWidth},
   * {@code target[1]} holds {@code halfHeight}. Values are computed as:
   * {@code target[0] = rescalingOrthoFactor() * (frame().magnitude() * this.screenWidth() / 2)}
   * and {@code rescalingOrthoFactor() * (frame().magnitude() * this.screenHeight() / 2)}
   * .
   * <p>
   * These values are valid for 2d Windows and ortho Cameras (but not persp) and they are
   * expressed in virtual graph units.
   * <p>
   * In the case of ortho Cameras these values are proportional to the Camera (z
   * projected) distance to the {@link #anchor()}. When zooming on the object, the Camera
   * is translated forward and its boundary is narrowed, making the object appear bigger
   * on screen, as intuitively expected.
   * <p>
   * Overload this method to change this behavior if desired.
   *
   * @see #rescalingOrthoFactor()
   */
  public float[] getBoundaryWidthHeight(float[] target) {
    if ((target == null) || (target.length != 2)) {
      target = new float[2];
    }

    float orthoCoef = this.rescalingOrthoFactor();

    target[0] = orthoCoef * (eye().magnitude() * width() / 2);
    target[1] = orthoCoef * (eye().magnitude() * height() / 2);

    return target;
  }

  /**
   * Simply returns {@code 1} which is valid for 2d Windows.
   * <p>
   * In 3D returns a value proportional to the Camera (z projected) distance to the
   * {@link #anchor()} so that when zooming on the object, the ortho Camera is translated
   * forward and its boundary is narrowed, making the object appear bigger on screen, as
   * intuitively expected.
   * <p>
   * Value is computed as: {@code 2 * distanceToAnchor() / screenHeight()}.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  public float rescalingOrthoFactor() {
    if(is2D())
      return 1.0f;
    float toAnchor = this.distanceToAnchor();
    float epsilon = 0.0001f;
    return (2 * (toAnchor == 0 ? epsilon : toAnchor) * rapK / height());
  }

  // nodes

  /**
   * Returns the top-level nodes (those which reference is null).
   * <p>
   * All leading nodes are also reachable by the {@link #traverse()} algorithm for
   * which they are the seeds.
   *
   * @see #nodes(boolean)
   * @see #isNodeReachable(Node)
   * @see #pruneBranch(Node)
   */
  public List<Node> leadingNodes() {
    return seeds;
  }

  /**
   * Returns {@code true} if the node is top-level.
   */
  protected boolean isLeadingNode(Node node) {
    for (Node _node : leadingNodes())
      if (_node == node)
        return true;
    return false;
  }

  /**
   * Add the node as top-level if its reference node is null and it isn't already added.
   */
  protected boolean addLeadingNode(Node node) {
    if (node == null || node.reference() != null)
      return false;
    if (isLeadingNode(node))
      return false;
    return leadingNodes().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected boolean removeLeadingNode(Node node) {
    boolean result = false;
    Iterator<Node> it = leadingNodes().iterator();
    while (it.hasNext()) {
      if (it.next() == node) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Traverse the node hierarchy, successively applying the local transformation defined
   * by each traversed node, and calling
   * {@link Node#visit()} on it.
   * <p>
   * Note that only reachable nodes are visited by this algorithm.
   * <p>
   * <b>Attention:</b> this method should be called after {@link MatrixHandler#bind()} (i.e.,
   * eye update) and before any other transformation of the modelview takes place.
   *
   * @see #isNodeReachable(Node)
   * @see #pruneBranch(Node)
   */
  public void traverse() {
    for (Node node : leadingNodes())
      visitNode(node);
  }

  /**
   * Used by the traverse node tree algorithm.
   */
  protected void visitNode(Node node) {
    pushModelView();
    applyTransformation(node);
    node.visitCallback();
    for (Node child : node.children())
      visitNode(child);
    popModelView();
  }

  /**
   * Same as {@code for(Node node : leadingNodes()) pruneBranch(node)}.
   *
   * @see #pruneBranch(Node)
   */
  public void clear() {
    for (Node node : leadingNodes())
      pruneBranch(node);
  }

  /**
   * Make all the nodes in the {@code node} branch eligible for garbage collection.
   * <p>
   * A call to {@link #isNodeReachable(Node)} on all {@code node} descendants
   * (including {@code node}) will return false, after issuing this method. It also means
   * that all nodes in the {@code node} branch will become unreachable by the
   * {@link #traverse()} algorithm.
   * <p>
   * nodes in the {@code node} branch will also be removed from all the agents currently
   * registered in the {@link #inputHandler()}.
   * <p>
   * To make all the nodes in the branch reachable again, first cache the nodes
   * belonging to the branch (i.e., {@code branch=pruneBranch(node)}) and then call
   * {@link #appendBranch(List)} on the cached branch. Note that calling
   * {@link Node#setReference(Node)} on a
   * node belonging to the pruned branch will become reachable again by the traversal
   * algorithm. In this case, the node should be manually added to some agents to
   * interactively handle it.
   * <p>
   * Note that if node is not reachable ({@link #isNodeReachable(Node)}) this
   * method returns {@code null}.
   * <p>
   * When collected, pruned nodes behave like {@link Node},
   * otherwise they are eligible for garbage collection.
   *
   * @see #clear()
   * @see #appendBranch(List)
   * @see #isNodeReachable(Node)
   */
  public ArrayList<Node> pruneBranch(Node node) {
    if (!isNodeReachable(node))
      return null;
    ArrayList<Node> list = new ArrayList<Node>();
    collectNodes(list, node, true);
    for (Node _node : list) {
      inputHandler().removeGrabber(_node);
      if (_node.reference() != null)
        _node.reference().removeChild(_node);
      else
        removeLeadingNode(_node);
    }
    return list;
  }

  /**
   * Appends the branch which typically should come from the one pruned (and cached) with
   * {@link #pruneBranch(Node)}.
   * <p>
   * All nodes belonging to the branch are automatically added to all graph agents.
   * <p>
   * {@link #pruneBranch(Node)}
   */
  public void appendBranch(List<Node> branch) {
    if (branch == null)
      return;
    for (Node node : branch) {
      inputHandler().addGrabber(node);
      if (node.reference() != null)
        node.reference().addChild(node);
      else
        addLeadingNode(node);
    }
  }

  /**
   * Returns {@code true} if the node is reachable by the {@link #traverse()}
   * algorithm and {@code false} otherwise.
   * <p>
   * Nodes are make unreachable with {@link #pruneBranch(Node)} and reachable
   * again with
   * {@link Node#setReference(Node)}.
   *
   * @see #traverse()
   * @see #nodes(boolean)
   */
  public boolean isNodeReachable(Node node) {
    if (node == null)
      return false;
    return node.reference() == null ? isLeadingNode(node) : node.reference().hasChild(node);
  }

  /**
   * Returns a list of all the nodes that are reachable by the {@link #traverse()}
   * algorithm, including the eye nodes (when {@code eyeframes} is {@code true}).
   *
   * @see #isNodeReachable(Node)
   * @see Node#isEye()
   */
  //TODO discard boolean param
  public ArrayList<Node> nodes(boolean eyeNodes) {
    ArrayList<Node> list = new ArrayList<Node>();
    for (Node node : leadingNodes())
      collectNodes(list, node, eyeNodes);
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. When {@code eyeframes} is
   * {@code true} eye-nodes will also be collected. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isNodeReachable(Node)
   */
  //TODO decide param
  public ArrayList<Node> branch(Node node, boolean eyeNodes) {
    ArrayList<Node> list = new ArrayList<Node>();
    collectNodes(list, node, eyeNodes);
    return list;
  }

  /**
   * Returns a straight path of nodes between {@code tail} and {@code tip}. When {@code eyeframes} is
   * {@code true} eye-nodes will also be included.
   * <p>
   * If {@code tip} is descendant of {@code tail} the returned list will include both of them. Otherwise it will be empty.
   */
  //TODO decide me
  public ArrayList<Node> branch(Node tail, Node tip, boolean eyeNodes) {
    ArrayList<Node> list = new ArrayList<Node>();
    //1. Check if tip is a tail descendant
    boolean desc = false;
    ArrayList<Node> descList = branch(tail, eyeNodes);
    for(Node node : descList)
      if(node == tip) {
        desc = true;
        break;
      }
    //2. If so, return the path between the two
    if(desc) {
      Node _tip = tip;
      while(_tip != tail) {
        if (!_tip.isEye() || eyeNodes)
          list.add(0, _tip);
          _tip = _tip.reference();
      }
      list.add(0, tail);
    }
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. When {@code eyeframes} is
   * {@code true} eye-nodes will also be collected. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isNodeReachable(Node)
   */
  //TODO check boolean param
  protected void collectNodes(List<Node> list, Node node, boolean eyeNodes) {
    if (node == null)
      return;
    if (!node.isEye() || eyeNodes)
      list.add(node);
    for (Node child : node.children())
      collectNodes(list, child, eyeNodes);
  }

  // Actions

  /**
   * Same as {@code eye().setAnchor(new Vector(0, 0, 0))}.
   */
  public void resetAnchor() {
    setAnchor(new Vector(0, 0, 0));
    // looks horrible, but works ;)
    //TODO restore
    //eye().anchorFlag = true;
    //eye().runResetAnchorHintTimer(1000);
  }

  // AGENTs

  // Keys

  /**
   * Returns the default {@link Agent} key agent.
   *
   * @see #motionAgent()
   */
  public Agent keyAgent() {
    return defKeyboardAgent;
  }

  /**
   * Returns {@code true} if the {@link #keyAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #enableKeyAgent()
   * @see #disableKeyAgent()
   * @see #isMotionAgentEnabled()
   */
  public boolean isKeyAgentEnabled() {
    return inputHandler().isAgentRegistered(keyAgent());
  }

  /**
   * Enables key handling through the {@link #keyAgent()}.
   *
   * @see #isKeyAgentEnabled()
   * @see #disableKeyAgent()
   * @see #enableMotionAgent()
   */
  public void enableKeyAgent() {
    if (!inputHandler().isAgentRegistered(keyAgent())) {
      inputHandler().registerAgent(keyAgent());
    }
  }

  // Motion agent

  /**
   * Returns the default motion agent.
   *
   * @see #keyAgent()
   */
  public Agent motionAgent() {
    return defMotionAgent;
  }

  /**
   * Returns {@code true} if the {@link #motionAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #enableMotionAgent()
   * @see #disableMotionAgent()
   * @see #isKeyAgentEnabled()
   */
  public boolean isMotionAgentEnabled() {
    return inputHandler().isAgentRegistered(motionAgent());
  }

  /**
   * Enables motion handling through the {@link #motionAgent()}.
   *
   * @see #isMotionAgentEnabled()
   * @see #disableMotionAgent()
   * @see #enableKeyAgent()
   */
  public void enableMotionAgent() {
    if (!inputHandler().isAgentRegistered(motionAgent())) {
      inputHandler().registerAgent(motionAgent());
    }
  }

  /**
   * Disables the default {@link Agent} and returns it.
   *
   * @see #isKeyAgentEnabled()
   * @see #enableMotionAgent()
   * @see #disableMotionAgent()
   */
  public boolean disableKeyAgent() {
    return inputHandler().unregisterAgent(keyAgent());
  }

  /**
   * Disables the default motion agent and returns it.
   *
   * @see #isMotionAgentEnabled()
   * @see #enableMotionAgent()
   * @see #enableKeyAgent()
   */
  public boolean disableMotionAgent() {
    return inputHandler().unregisterAgent(motionAgent());
  }

  // FPSTiming STUFF

  /**
   * Returns the number of nodes displayed since the graph was instantiated.
   * <p>
   * Use {@code Graph.frameCount} to retrieve the number of nodes displayed since
   * the first graph was instantiated.
   */
  public long frameCount() {
    return timingHandler().frameCount();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerTask(task)}.
   *
   * @see remixlab.fpstiming.TimingHandler#registerTask(TimingTask)
   */
  public void registerTimingTask(TimingTask task) {
    timingHandler().registerTask(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterTask(task)}.
   */
  public void unregisterTimingTask(TimingTask task) {
    timingHandler().unregisterTask(task);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isTaskRegistered(task)}.
   */
  public boolean isTimingTaskRegistered(TimingTask task) {
    return timingHandler().isTaskRegistered(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerAnimator(object)}.
   */
  public void registerAnimator(Animator object) {
    timingHandler().registerAnimator(object);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterAnimator(object)}.
   *
   * @see remixlab.fpstiming.TimingHandler#unregisterAnimator(Animator)
   */
  public void unregisterAnimator(Animator object) {
    timingHandler().unregisterAnimator(object);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isAnimatorRegistered(object)}.
   *
   * @see remixlab.fpstiming.TimingHandler#isAnimatorRegistered(Animator)
   */
  public boolean isAnimatorRegistered(Animator object) {
    return timingHandler().isAnimatorRegistered(object);
  }

  // E V E N T H A N D L I N G

  /**
   * Returns the graph {@link InputHandler}.
   */
  public InputHandler inputHandler() {
    return iHandler;
  }

  /**
   * Returns the graph {@link TimingHandler}.
   */
  public TimingHandler timingHandler() {
    return tHandler;
  }

  // 1. Scene overloaded

  // MATRIX and TRANSFORMATION STUFF

  /**
   * Wrapper for {@link MatrixHandler#beginScreenDrawing()}. Adds
   * exception when no properly closing the screen drawing with a call to
   * {@link #endScreenDrawing()}.
   *
   * @see MatrixHandler#beginScreenDrawing()
   */
  public void beginScreenDrawing() {
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
              + "endScreenDrawing() and they cannot be nested. Check your implementation!");

    startCoordCalls++;
    matrixHandler.beginScreenDrawing();
  }

  /**
   * Wrapper for {@link MatrixHandler#endScreenDrawing()} . Adds
   * exception if {@link #beginScreenDrawing()} wasn't properly called before
   *
   * @see MatrixHandler#endScreenDrawing()
   */
  public void endScreenDrawing() {
    startCoordCalls--;
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
              + "endScreenDrawing() and they cannot be nested. Check your implementation!");

    matrixHandler.endScreenDrawing();
  }

  /**
   * Computes the projection matrix associated with the eye.
   * <p>
   * If eye is a 3D PERSPECTIVE Camera, defines a projection matrix using the
   * {@link #fieldOfView()}, {@link #aspectRatio()},
   * {@link #zNear()} and
   * {@link #zFar()} parameters. If eye is a 3D ORTHOGRAPHIC
   * Camera, the frustum's width and height are set using
   * {@link #getBoundaryWidthHeight()}. Both types use
   * {@link #zNear()} and
   * {@link #zFar()} to place clipping planes. These values
   * are determined from sceneRadius() and sceneCenter() so that they best fit the graph
   * size.
   * <p>
   * <b>Note:</b> You must call this method if your eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   */
  //TODO pass a Matrix param!
  public Matrix computeProjection() {
    Matrix m = new Matrix();
    float ZNear = zNear();
    float ZFar = zFar();
    switch (type()) {
      case PERSPECTIVE:
        // #CONNECTION# all non null coefficients were set to 0.0 in constructor.
        m.mat[0] = 1 / (eye().magnitude() * this.aspectRatio());
        m.mat[5] = 1 / (isLeftHanded() ? -eye().magnitude() : eye().magnitude());
        m.mat[10] = (ZNear + ZFar) / (ZNear - ZFar);
        m.mat[11] = -1.0f;
        m.mat[14] = 2.0f * ZNear * ZFar / (ZNear - ZFar);
        m.mat[15] = 0.0f;
        // same as gluPerspective( 180.0*fieldOfView()/M_PI, aspectRatio(),
        // zNear(), zFar() );
        break;
      case ORTHOGRAPHIC:
        float[] wh = getBoundaryWidthHeight();
        m.mat[0] = 1.0f / wh[0];
        m.mat[5] = (isLeftHanded() ? -1.0f : 1.0f) / wh[1];
        m.mat[10] = -2.0f / (ZFar - ZNear);
        m.mat[11] = 0.0f;
        m.mat[14] = -(ZFar + ZNear) / (ZFar - ZNear);
        m.mat[15] = 1.0f;
        // same as glOrtho( -w, w, -h, h, zNear(), zFar() );
        break;
    }
    return m;
  }

  /**
   * Computes the View matrix associated with the {@link #eye()}) position and orientation.
   * <p>
   * This matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using the projection matrix (see
   * {@link #computeProjection()}).
   * <p>
   * <b>Note:</b> You must call this method if your eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   */
  //TODO pass a Matrix param!
  public Matrix computeView() {
    Matrix m = new Matrix();

    Quaternion q = eye().orientation();

    float q00 = 2.0f * q.quat[0] * q.quat[0];
    float q11 = 2.0f * q.quat[1] * q.quat[1];
    float q22 = 2.0f * q.quat[2] * q.quat[2];

    float q01 = 2.0f * q.quat[0] * q.quat[1];
    float q02 = 2.0f * q.quat[0] * q.quat[2];
    float q03 = 2.0f * q.quat[0] * q.quat[3];

    float q12 = 2.0f * q.quat[1] * q.quat[2];
    float q13 = 2.0f * q.quat[1] * q.quat[3];
    float q23 = 2.0f * q.quat[2] * q.quat[3];

    m.mat[0] = 1.0f - q11 - q22;
    m.mat[1] = q01 - q23;
    m.mat[2] = q02 + q13;
    m.mat[3] = 0.0f;

    m.mat[4] = q01 + q23;
    m.mat[5] = 1.0f - q22 - q00;
    m.mat[6] = q12 - q03;
    m.mat[7] = 0.0f;

    m.mat[8] = q02 - q13;
    m.mat[9] = q12 + q03;
    m.mat[10] = 1.0f - q11 - q00;
    m.mat[11] = 0.0f;

    Vector t = q.inverseRotate(eye().position());

    m.mat[12] = -t.vec[0];
    m.mat[13] = -t.vec[1];
    m.mat[14] = -t.vec[2];
    m.mat[15] = 1.0f;

    return m;
  }

  /**
   * Sets the {@link MatrixHandler} defining how dandelion matrices
   * are to be handled.
   *
   * @see #matrixHandler()
   */
  public void setMatrixHandler(MatrixHandler r) {
    matrixHandler = r;
  }

  /**
   * Returns the {@link MatrixHandler}.
   *
   * @see #setMatrixHandler(MatrixHandler)
   */
  public MatrixHandler matrixHandler() {
    return matrixHandler;
  }

  /**
   * Wrapper for {@link MatrixHandler#pushModelView()}
   */
  public void pushModelView() {
    matrixHandler.pushModelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#popModelView()}
   */
  public void popModelView() {
    matrixHandler.popModelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#pushProjection()}
   */
  public void pushProjection() {
    matrixHandler.pushProjection();
  }

  /**
   * Wrapper for {@link MatrixHandler#popProjection()}
   */
  public void popProjection() {
    matrixHandler.popProjection();
  }

  /**
   * Wrapper for {@link MatrixHandler#translate(float, float)}
   */
  public void translate(float tx, float ty) {
    matrixHandler.translate(tx, ty);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#translate(float, float, float)}
   */
  public void translate(float tx, float ty, float tz) {
    matrixHandler.translate(tx, ty, tz);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotate(float)}
   */
  public void rotate(float angle) {
    matrixHandler.rotate(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateX(float)}
   */
  public void rotateX(float angle) {
    matrixHandler.rotateX(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateY(float)}
   */
  public void rotateY(float angle) {
    matrixHandler.rotateY(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateZ(float)}
   */
  public void rotateZ(float angle) {
    matrixHandler.rotateZ(angle);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#rotate(float, float, float, float)}
   */
  public void rotate(float angle, float vx, float vy, float vz) {
    matrixHandler.rotate(angle, vx, vy, vz);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float)}
   */
  public void scale(float s) {
    matrixHandler.scale(s);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float, float)}
   */
  public void scale(float sx, float sy) {
    matrixHandler.scale(sx, sy);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float, float, float)}
   */
  public void scale(float x, float y, float z) {
    matrixHandler.scale(x, y, z);
  }

  /**
   * Wrapper for {@link MatrixHandler#modelView()}
   */
  public Matrix modelView() {
    return matrixHandler.modelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#projection()}
   */
  public Matrix projection() {
    return matrixHandler.projection();
  }

  /**
   * Wrapper for {@link MatrixHandler#projection()}
   */
  public Matrix view() {
    return matrixHandler.view();
  }

  /**
   * Wrapper for {@link MatrixHandler#bindModelView(Matrix)}
   */
  public void setModelView(Matrix source) {
    matrixHandler.bindModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#bindProjection(Matrix)}
   */
  public void setProjection(Matrix source) {
    matrixHandler.bindProjection(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#applyModelView(Matrix)}
   */
  public void applyModelView(Matrix source) {
    matrixHandler.applyModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#applyProjection(Matrix)}
   */
  public void applyProjection(Matrix source) {
    matrixHandler.applyProjection(source);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#isProjectionViewInverseCached()} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vector)}.
   *
   * @see #optimizeUnprojectedCoordinatesOf(boolean)
   * @see #unprojectedCoordinatesOf(Vector)
   */
  public boolean isUnprojectedCoordinatesOfOptimized() {
    return matrixHandler.isProjectionViewInverseCached();
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#cacheProjectionViewInverse(boolean)} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vector)}.
   *
   * @see #isUnprojectedCoordinatesOfOptimized()
   * @see #unprojectedCoordinatesOf(Vector)
   */
  public void optimizeUnprojectedCoordinatesOf(boolean optimise) {
    matrixHandler.cacheProjectionViewInverse(optimise);
  }

  // DRAWING STUFF

  /**
   * Returns the visual hints flag.
   */
  public int visualHints() {
    return this.visualHintMask;
  }

  /**
   * Low level setting of visual flags. You'd prefer {@link #setAxesVisualHint(boolean)},
   * {@link #setGridVisualHint(boolean)}, and
   * {@link #setPickingVisualHint(boolean)}, unless you want to set them all at once,
   * e.g., {@code setVisualHints(Scene.AXES | Scene.GRID | Scene.PATHS | Scene.PICKING)}.
   */
  public void setVisualHints(int flag) {
    visualHintMask = flag;
  }

  /**
   * Toggles the state of {@link #gridVisualHint()}.
   *
   * @see #setGridVisualHint(boolean)
   */
  public void toggleGridVisualHint() {
    setGridVisualHint(!gridVisualHint());
  }

  /**
   * Toggles the state of {@link #axesVisualHint()}.
   *
   * @see #axesVisualHint()
   * @see #setAxesVisualHint(boolean)
   */
  public void toggleAxesVisualHint() {
    setAxesVisualHint(!axesVisualHint());
  }

  /**
   * Toggles the state of {@link #pickingVisualHint()}.
   *
   * @see #setPickingVisualHint(boolean)
   */
  public void togglePickingVisualhint() {
    setPickingVisualHint(!pickingVisualHint());
  }

  /**
   * Returns {@code true} if grid is currently being drawn and {@code false} otherwise.
   */
  public boolean gridVisualHint() {
    return ((visualHintMask & GRID) != 0);
  }

  /**
   * Returns {@code true} if axes are currently being drawn and {@code false} otherwise.
   */
  public boolean axesVisualHint() {
    return ((visualHintMask & AXES) != 0);
  }

  /**
   * Returns {@code true} if the picking selection visual hint is currently being drawn
   * and {@code false} otherwise.
   */
  public boolean pickingVisualHint() {
    return ((visualHintMask & PICKING) != 0);
  }

  /**
   * Sets the display of the grid according to {@code draw}
   */
  public void setGridVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= GRID;
    else
      visualHintMask &= ~GRID;
  }

  /**
   * Sets the display of the axes according to {@code draw}
   */
  public void setAxesVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= AXES;
    else
      visualHintMask &= ~AXES;
  }

  /**
   * Sets the display of the interactive nodes' selection hints according to {@code draw}
   */
  public void setPickingVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= PICKING;
    else
      visualHintMask &= ~PICKING;
  }

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link MatrixHandler#bind()}</li>
   * <li>Calls {@link #updateBoundaryEquations()} if
   * {@link #areBoundaryEquationsEnabled()}</li>
   * <li>Calls {@link #proscenium()}</li>
   * <li>Calls {@link #displayVisualHints()}.</li>
   * </ol>
   *
   * @see #postDraw()
   */
  public void preDraw() {
    // 1. Eye, raster graph
    matrixHandler().bind();
    if (areBoundaryEquationsEnabled()) {
      if(eye() instanceof Node) {
        if(( ((Node)eye()).lastUpdate() > lastEqUpdate || lastEqUpdate == 0)) {
          updateBoundaryEquations();
          lastEqUpdate = frameCount;
        }
      }
      else {
        updateBoundaryEquations();
        lastEqUpdate = frameCount;
      }
    }
    //TODO really needs checking. Previously we went like this:
    /*
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > lastEqUpdate || lastEqUpdate == 0)) {
      updateBoundaryEquations();
      lastEqUpdate = frameCount;
    }
    */
    // 3. Alternative use only
    proscenium();
    // 4. Display visual hints
    displayVisualHints(); // abstract
  }

  /**
   * Called after your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link remixlab.fpstiming.TimingHandler#handle()} and increments the the
   * {@link #frameCount()}</li>
   * <li>Increments the {@link #frameCount()}</li>
   * <li>Calls {@link InputHandler#handle()}</li>
   * </ol>
   *
   * @see #preDraw()
   */
  public void postDraw() {
    // 1. timers (include IK Solvers' execution in the order they were registered)
    timingHandler().handle();
    if (frameCount < timingHandler().frameCount())
      frameCount = timingHandler().frameCount();
    if (frameCount < timingHandler().frameCount() + deltaCount)
      frameCount = timingHandler().frameCount() + deltaCount;
    // 2. Agents
    inputHandler().handle();
  }

  /**
   * Internal use. Display various on-screen visual hints to be called from
   * {@link #postDraw()}.
   */
  protected void displayVisualHints() {
    if (gridVisualHint())
      drawGridHint();
    if (axesVisualHint())
      drawAxesHint();
    if (pickingVisualHint())
      drawPickingHint();
    //TODO restore
    /*
    if (pathsVisualHint())
      drawPathsHint();
    if (zoomVisualHint())
      drawZoomWindowHint();
    if (rotateVisualHint())
      drawScreenRotateHint();
    if (eye().anchorFlag)
      drawAnchorHint();
    if (eye().pupFlag)
      drawPointUnderPixelHint();
    */
  }

  /**
   * Internal use.
   */
  protected void drawGridHint() {
    System.out.println("implement me");
  }

  /**
   * Internal use.
   */
  protected void drawAxesHint() {
    System.out.println("implement me");
  }

  /**
   * Internal use.
   */
  protected void drawPickingHint() {
    System.out.println("implement me");
  }

  /**
   * Draws visual hint (a cross on the screen) when the
   * {@link #anchor()} is being set.
   */
  //protected abstract void drawAnchorHint();

  // 0. Optimization stuff

  // public abstract long frameCount();

  // 3. EYE STUFF

  /**
   * Returns the associated eye. Never null.
   *
   * @see #setEye(Frame)
   */
  public Frame eye() {
    return eye;
  }

  /**
   * Replaces the current {@link #eye()} with {@code e}.
   *
   * @see #eye()
   */
  public void setEye(Frame e) {
    if (e == null || eye == e)
      return;
    eye = e;
    //TODO decide me, but I dont think it should go in new minimalistic design
    //if(eye instanceof Node)
      //inputHandler().setDefaultGrabber((Node)eye());
    setRadius(radius());
    setCenter(center());
  }

  /**
   * If {@link #isLeftHanded()} calls {@link #setRightHanded()}, otherwise calls
   * {@link #setLeftHanded()}.
   */
  public void flip() {
    if (isLeftHanded())
      setRightHanded();
    else
      setLeftHanded();
  }

  /**
   * Same as {@code return isPointVisible(new Vector(x, y, z))}.
   *
   * @see #isPointVisible(Vector)
   */
  public boolean isPointVisible(float x, float y, float z) {
    return isPointVisible(new Vector(x, y, z));
  }

  /**
   * Returns {@code true} if {@code point} is visible (i.e, lies within the eye boundary)
   * and {@code false} otherwise.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public boolean isPointVisible(Vector point) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by pointIsVisible) may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    for (int i = 0; i < 6; ++i)
      if (distanceToBoundary(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Returns {@link Visibility#VISIBLE},
   * {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the
   * sphere (of radius {@code radius} and center {@code center}) is visible, invisible, or
   * semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility ballVisibility(Vector center, float radius) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by sphereIsVisible) may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      float d = distanceToBoundary(i, center);
      if (d > radius)
        return Visibility.INVISIBLE;
      if ((d > 0) || (-d < radius))
        allInForAllPlanes = false;
    }
    if (allInForAllPlanes)
      return Visibility.VISIBLE;
    return Visibility.SEMIVISIBLE;
  }

  /**
   * Returns {@link Visibility#VISIBLE},
   * {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the
   * axis aligned box (defined by corners {@code p1} and {@code p2}) is visible,
   * invisible, or semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility boxVisibility(Vector p1, Vector p2) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by aaBoxIsVisible) may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      boolean allOut = true;
      for (int c = 0; c < 8; ++c) {
        Vector pos = new Vector(((c & 4) != 0) ? p1.vec[0] : p2.vec[0], ((c & 2) != 0) ? p1.vec[1] : p2.vec[1],
                ((c & 1) != 0) ? p1.vec[2] : p2.vec[2]);
        if (distanceToBoundary(i, pos) > 0.0)
          allInForAllPlanes = false;
        else
          allOut = false;
      }
      // The eight points are on the outside side of this plane
      if (allOut)
        return Visibility.INVISIBLE;
    }

    if (allInForAllPlanes)
      return Visibility.VISIBLE;

    // Too conservative, but tangent cases are too expensive to detect
    return Visibility.SEMIVISIBLE;
  }

  /**
   * Convenience function that in 2D simply returns
   * {@code computeFrustumPlanesCoefficients(new float [4][3])} and in 3D
   * {@code computeFrustumPlanesCoefficients(new float [6][4])}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link Graph#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations(float[][])
   */
  public float[][] computeBoundaryEquations() {
    return computeBoundaryEquations(new float[6][4]);
  }

  /**
   * Fills {@code coef} with the 6 plane equations of the camera frustum and returns it.
   * <p>
   * In 2D the four 4-component vectors of {@code coef} respectively correspond to the
   * left, right, top and bottom Window boundary lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * <p>
   * In 3D the six 4-component vectors of {@code coef} respectively correspond to the
   * left, right, near, far, top and bottom Camera frustum planes. Each vector holds a
   * plane equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   * <p>
   * This format is compatible with the {@code gl.glClipPlane()} function. One camera
   * frustum plane can hence be applied in an other viewer to visualize the culling
   * results:
   * <p>
   * {@code // Retrieve place equations}<br>
   * {@code float [][] coef =
   * mainViewer.camera().getFrustumPlanesCoefficients();}<br>
   * {@code // These two additional clipping planes (which must have been enabled)} <br>
   * {@code // will reproduce the mainViewer's near and far clipping.}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE0, coef[2]);}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE1, coef[3]);}<br>
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link Graph#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations()
   */
  public float[][] computeBoundaryEquations(float[][] coef) {
    // soft check:
    if (coef == null || (coef.length == 0))
      coef = new float[6][4];
    else if ((coef.length != 6) || (coef[0].length != 4))
      coef = new float[6][4];

    // Computed once and for all
    Vector pos = eye().position();
    Vector viewDir = viewDirection();
    Vector up = upVector();
    Vector right = rightVector();

    float posViewDir = Vector.dot(pos, viewDir);

    switch (type()) {
      case PERSPECTIVE: {
        float hhfov = horizontalFieldOfView() / 2.0f;
        float chhfov = (float) Math.cos(hhfov);
        float shhfov = (float) Math.sin(hhfov);
        normal[0] = Vector.multiply(viewDir, -shhfov);
        normal[1] = Vector.add(normal[0], Vector.multiply(right, chhfov));
        normal[0] = Vector.add(normal[0], Vector.multiply(right, -chhfov));
        normal[2] = Vector.multiply(viewDir, -1);
        normal[3] = viewDir;

        float hfov = fieldOfView() / 2.0f;
        float chfov = (float) Math.cos(hfov);
        float shfov = (float) Math.sin(hfov);
        normal[4] = Vector.multiply(viewDir, -shfov);
        normal[5] = Vector.add(normal[4], Vector.multiply(up, -chfov));
        normal[4] = Vector.add(normal[4], Vector.multiply(up, chfov));

        for (int i = 0; i < 2; ++i)
          dist[i] = Vector.dot(pos, normal[i]);
        for (int j = 4; j < 6; ++j)
          dist[j] = Vector.dot(pos, normal[j]);

        // Natural equations are:
        // dist[0,1,4,5] = pos * normal[0,1,4,5];
        // dist[2] = (pos + zNear() * viewDir) * normal[2];
        // dist[3] = (pos + zFar() * viewDir) * normal[3];

        // 2 times less computations using expanded/merged equations. Dir vectors
        // are normalized.
        float posRightCosHH = chhfov * Vector.dot(pos, right);
        dist[0] = -shhfov * posViewDir;
        dist[1] = dist[0] + posRightCosHH;
        dist[0] = dist[0] - posRightCosHH;
        float posUpCosH = chfov * Vector.dot(pos, up);
        dist[4] = -shfov * posViewDir;
        dist[5] = dist[4] - posUpCosH;
        dist[4] = dist[4] + posUpCosH;
        break;
      }
      case ORTHOGRAPHIC:
        normal[0] = Vector.multiply(right, -1);
        normal[1] = right;
        normal[4] = up;
        normal[5] = Vector.multiply(up, -1);

        float[] wh = getBoundaryWidthHeight();
        dist[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh[0])), normal[0]);
        dist[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh[0])), normal[1]);
        dist[4] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh[1])), normal[4]);
        dist[5] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh[1])), normal[5]);
        break;
    }

    // Front and far planes are identical for both camera types.
    normal[2] = Vector.multiply(viewDir, -1);
    normal[3] = viewDir;
    dist[2] = -posViewDir - zNear();
    dist[3] = posViewDir + zFar();

    for (int i = 0; i < 6; ++i) {
      coef[i][0] = normal[i].vec[0];
      coef[i][1] = normal[i].vec[1];
      coef[i][2] = normal[i].vec[2];
      coef[i][3] = dist[i];
    }

    return coef;
  }

  /**
   * Toggles automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see #updateBoundaryEquations()
   */
  public void toggleBoundaryEquations() {
    if (areBoundaryEquationsEnabled())
      disableBoundaryEquations();
    else
      enableBoundaryEquations();
  }

  /**
   * Disables automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #toggleBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see #updateBoundaryEquations()
   */
  public void disableBoundaryEquations() {
    enableBoundaryEquations(false);
  }

  /**
   * Enables automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #toggleBoundaryEquations()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see #updateBoundaryEquations()
   */
  public void enableBoundaryEquations() {
    enableBoundaryEquations(true);
  }

  /**
   * Enables or disables automatic update of the eye boundary plane equations every frame
   * according to {@code flag}. Computation of the equations is expensive and hence is
   * disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public void enableBoundaryEquations(boolean flag) {
    fpCoefficientsUpdate = flag;
  }

  /**
   * Returns {@code true} if automatic update of the eye boundary plane equations is
   * enabled and {@code false} otherwise. Computation of the equations is expensive and
   * hence is disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public boolean areBoundaryEquationsEnabled() {
    return fpCoefficientsUpdate;
  }

  /**
   * Updates the boundary plane equations according to the current eye setup, by simply
   * calling {@link #computeBoundaryEquations()}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * boundary equations to be updated only occasionally (rare). Use
   * {@link #enableBoundaryEquations()} which
   * automatically update the boundary equations every frame instead.
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public void updateBoundaryEquations() {
    computeBoundaryEquations(fpCoefficients);
  }

  /**
   * Returns the boundary plane equations.
   * <p>
   * The six 4-component vectors returned by this method, respectively correspond to the
   * left, right, near, far, top and bottom eye boundary planes. Each vector holds a plane
   * equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link #enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float[][] getBoundaryEquations() {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    return fpCoefficients;
  }

  /**
   * Returns the signed distance between point {@code pos} and plane {@code index} in
   * Scene units. The distance is negative if the point lies in the planes's boundary
   * halfspace, and positive otherwise.
   * <p>
   * {@code index} is a value between {@code 0} and {@code 5} which respectively
   * correspond to the left, right, near, far, top and bottom eye boundary planes.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link #enableBoundaryEquations()}).
   *
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float distanceToBoundary(int index, Vector pos) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations (needed by distanceToBoundary) may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    Vector myVector = new Vector(fpCoefficients[index][0], fpCoefficients[index][1], fpCoefficients[index][2]);
    return Vector.dot(pos, myVector) - fpCoefficients[index][3];
  }

  /**
   * Returns the pixel to graph (units) ratio at {@code position}.
   * <p>
   * Convenience function that simply returns {@code 1 / sceneToPixelRatio(position)}.
   *
   * @see #sceneToPixelRatio(Vector)
   */
  public float pixelToSceneRatio(Vector position) {
    return 1 / sceneToPixelRatio(position);
  }

  /**
   * Returns the ratio of graph (units) to pixel at {@code position}.
   * <p>
   * A line of {@code n * sceneToPixelRatio()} graph units, located at {@code position} in
   * the world coordinates system, will be projected with a length of {@code n} pixels on
   * screen.
   * <p>
   * Use this method to scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #center()} and
   * always directed along the screen vertical direction:
   * <p>
   * {@code beginShape(LINES);}<br>
   * {@code vertex(sceneCenter().x, sceneCenter().y, sceneCenter().z);}<br>
   * {@code Vector v = Vector.addGrabber(sceneCenter(), Vector.mult(upVector(), 20 * sceneToPixelRatio(sceneCenter())));}
   * <br>
   * {@code vertex(v.x, v.y, v.z);}<br>
   * {@code endShape();}<br>
   */
  public float sceneToPixelRatio(Vector position) {
    switch (type()) {
      case PERSPECTIVE:
        return 2.0f * Math.abs((eye().coordinatesOf(position)).vec[2] * eye().magnitude()) * (float) Math
                .tan(fieldOfView() / 2.0f) / height();
      case ORTHOGRAPHIC:
        float[] wh = getBoundaryWidthHeight();
        return 2.0f * wh[1] / height();
    }
    return 1.0f;
  }

  /**
   * Same as {@code return !isFaceBackFacing(a, b, c)}.
   *
   * @see #isFaceBackFacing(Vector, Vector, Vector)
   */
  public boolean isFaceFrontFacing(Vector a, Vector b, Vector c) {
    return !isFaceBackFacing(a, b, c);
  }

  /**
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   * <p>
   * Vertices must given in clockwise order if
   * {@link Graph#isLeftHanded()} or in counter-clockwise
   * order if {@link Graph#isRightHanded()}.
   *
   * @param a first face vertex
   * @param b second face vertex
   * @param c third face vertex
   * @see #isFaceBackFacing(Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector a, Vector b, Vector c) {
    return isFaceBackFacing(a, isLeftHanded() ?
            Vector.subtract(b, a).cross(Vector.subtract(c, a)) :
            Vector.subtract(c, a).cross(Vector.subtract(b, a)));
  }

  /**
   * Same as {@code return !isFaceBackFacing(vertex, normal)}.
   *
   * @see #isFaceBackFacing(Vector, Vector)
   */
  public boolean isFaceFrontFacing(Vector vertex, Vector normal) {
    return !isFaceBackFacing(vertex, normal);
  }

  /**
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   *
   * @param vertex belonging to the face
   * @param normal face normal
   * @see #isFaceBackFacing(Vector, Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector vertex, Vector normal) {
    return isConeBackFacing(vertex, normal, 0);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vector, ArrayList)
   */
  public boolean isConeFrontFacing(Vector vertex, ArrayList<Vector> normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals ArrayList of normals defining the cone.
   * @see #isConeBackFacing(Vector, Vector[])
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeBackFacing(Vector vertex, ArrayList<Vector> normals) {
    return isConeBackFacing(vertex, normals.toArray(new Vector[normals.size()]));
  }

  /**
   * Same as {@code !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vector, Vector[])
   */
  public boolean isConeFrontFacing(Vector vertex, Vector[] normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals Array of normals defining the cone.
   * @see #isConeBackFacing(Vector, ArrayList)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeBackFacing(Vector vertex, Vector[] normals) {
    float angle;
    Vector axis = new Vector(0, 0, 0);

    if (normals.length == 0)
      throw new RuntimeException("Normal array provided is empty");

    Vector[] n = new Vector[normals.length];
    for (int i = 0; i < normals.length; i++) {
      n[i] = new Vector();
      n[i].set(normals[i]);
      n[i].normalize();
      axis = Vector.add(axis, n[i]);
    }

    if (axis.magnitude() != 0)
      axis.normalize();
    else
      axis.set(0, 0, 1);

    angle = 0;
    for (int i = 0; i < normals.length; i++)
      angle = Math.max(angle, (float) Math.acos(Vector.dot(n[i], axis)));

    return isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, axis, angle)}.
   *
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeFrontFacing(Vector vertex, Vector axis, float angle) {
    return !isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
   *
   * @param vertex Cone vertex
   * @param axis   Cone axis
   * @param angle  Cone angle
   */
  public boolean isConeBackFacing(Vector vertex, Vector axis, float angle) {
    // more or less inspired by this:
    // http://en.wikipedia.org/wiki/Back-face_culling (perspective case :P)
    Vector camAxis;
    if (type() == Type.ORTHOGRAPHIC)
      camAxis = viewDirection();
    else {
      camAxis = Vector.subtract(vertex, eye().position());
      if (angle != 0)
        camAxis.normalize();
    }
    if (angle == 0)
      return Vector.dot(camAxis, axis) >= 0;
    float absAngle = Math.abs(angle);
    if (absAngle >= Math.PI / 2)
      return true;
    Vector faceNormal = axis.get();
    faceNormal.normalize();
    return Math.acos(Vector.dot(camAxis, faceNormal)) + absAngle < Math.PI / 2;
  }

  /**
   * Convenience function that simply returns {@code projectedCoordinatesOf(src, null)}.
   *
   * @see #projectedCoordinatesOf(Vector, Frame)
   */
  public final Vector projectedCoordinatesOf(Vector src) {
    return projectedCoordinatesOf(src, null);
  }

  /**
   * Returns the screen projected coordinates of a point {@code src} defined in the
   * {@code frame} coordinate system.
   * <p>
   * When {@code frame} is {@code null}, {@code src} is expressed in the world coordinate
   * system. See {@link #projectedCoordinatesOf(Vector)}.
   * <p>
   * The x and y coordinates of the returned Vector are expressed in pixel, (0,0) being the
   * upper left corner of the window. The z coordinate ranges between 0.0 (near plane) and
   * 1.0 (excluded, far plane). See the {@code gluProject} man page for details.
   *
   * @see #unprojectedCoordinatesOf(Vector, Frame)
   */
  public final Vector projectedCoordinatesOf(Vector src, Frame frame) {
    float xyz[] = new float[3];

    if (frame != null) {
      Vector tmp = frame.inverseCoordinatesOf(src);
      project(tmp.vec[0], tmp.vec[1], tmp.vec[2], xyz);
    } else
      project(src.vec[0], src.vec[1], src.vec[2], xyz);

    return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  // cached version
  protected boolean project(float objx, float objy, float objz, float[] windowCoordinate) {
    Matrix projectionViewMatrix = matrixHandler().cacheProjectionView();

    float in[] = new float[4];
    float out[] = new float[4];

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    out[0] = projectionViewMatrix.mat[0] * in[0] + projectionViewMatrix.mat[4] * in[1] + projectionViewMatrix.mat[8] * in[2]
            + projectionViewMatrix.mat[12] * in[3];
    out[1] = projectionViewMatrix.mat[1] * in[0] + projectionViewMatrix.mat[5] * in[1] + projectionViewMatrix.mat[9] * in[2]
            + projectionViewMatrix.mat[13] * in[3];
    out[2] = projectionViewMatrix.mat[2] * in[0] + projectionViewMatrix.mat[6] * in[1] + projectionViewMatrix.mat[10] * in[2]
            + projectionViewMatrix.mat[14] * in[3];
    out[3] = projectionViewMatrix.mat[3] * in[0] + projectionViewMatrix.mat[7] * in[1] + projectionViewMatrix.mat[11] * in[2]
            + projectionViewMatrix.mat[15] * in[3];

    if (out[3] == 0.0)
      return false;

    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height();
    viewport[2] = width();
    viewport[3] = -height();

    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];

    // Map x, y and z to range 0-1
    out[0] = out[0] * 0.5f + 0.5f;
    out[1] = out[1] * 0.5f + 0.5f;
    out[2] = out[2] * 0.5f + 0.5f;

    // Map x,y to viewport
    out[0] = out[0] * viewport[2] + viewport[0];
    out[1] = out[1] * viewport[3] + viewport[1];

    windowCoordinate[0] = out[0];
    windowCoordinate[1] = out[1];
    windowCoordinate[2] = out[2];

    return true;
  }

  /**
   * Convenience function that simply returns {@code unprojectedCoordinatesOf(src, null)}.
   * <p>
   * #see {@link #unprojectedCoordinatesOf(Vector, Frame)}
   */
  public final Vector unprojectedCoordinatesOf(Vector src) {
    return this.unprojectedCoordinatesOf(src, null);
  }

  /**
   * Returns the world unprojected coordinates of a point {@code src} defined in the
   * screen coordinate system.
   * <p>
   * The {@code src.x} and {@code src.y} inputGrabber values are expressed in pixels, (0,0) being
   * the upper left corner of the window. The {@code src.z} is a depth value ranging in
   * [0..1] (near and far plane respectively). In 3D Note that {@code src.z} is not a
   * linear interpolation between {@link #zNear()} and
   * {@link #zFar()};
   * {@code src.z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z);} where {@code z}
   * is the distance from the point you project to the camera, along the
   * {@link #viewDirection()} . See the {@code gluUnProject} man page for details.
   * <p>
   * The result is expressed in the {@code frame} coordinate system. When {@code frame} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code frame} hierarchy (i.e., when
   * {@link Frame#reference()} is non-null) is taken into
   * account.
   * <p>
   * {@link #projectedCoordinatesOf(Vector, Frame)} performs the inverse transformation.
   * <p>
   * This method only uses the intrinsic eye parameters (see {@link #view()},
   * {@link #projection()}, {@link #width()} and {@link #height()}) and is completely independent of
   * the Processing matrices. You can hence define a virtual eye and use this method to
   * compute un-projections out of a classical rendering context.
   * <p>
   * This method is not computationally optimized by default. If you call it several times with no
   * change in the matrices, you should buffer the entire inverse projection matrix (view,
   * projection) to speed-up the queries. See {@link #optimizeUnprojectedCoordinatesOf(boolean)}.
   *
   * @see #projectedCoordinatesOf(Vector, Frame)
   * @see #setWidth(int)
   * @see #setHeight(int)
   */
  public final Vector unprojectedCoordinatesOf(Vector src, Frame frame) {
    float xyz[] = new float[3];
    // unproject(src.vec[0], src.vec[1], src.vec[2], this.getViewMatrix(true),
    // this.getProjectionMatrix(true),
    // getViewport(), xyz);
    unproject(src.vec[0], src.vec[1], src.vec[2], xyz);
    if (frame != null)
      return frame.coordinatesOf(new Vector(xyz[0], xyz[1], xyz[2]));
    else
      return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  /**
   * Similar to {@code gluUnProject}: map window coordinates to object coordinates.
   *
   * @param winx                     Specify the window x coordinate.
   * @param winy                     Specify the window y coordinate.
   * @param winz                     Specify the window z coordinate.
   * @param objCoordinate            Return the computed object coordinates.
   */
  public boolean unproject(float winx, float winy, float winz, float[] objCoordinate) {
    Matrix projectionViewInverseMatrix;
    if(matrixHandler().isProjectionViewInverseCached())
      projectionViewInverseMatrix = matrixHandler().cacheProjectionViewInverse();
    else {
      projectionViewInverseMatrix = Matrix.multiply(matrixHandler().cacheProjection(), matrixHandler().cacheView());
      projectionViewInverseMatrix.invert();
    }

    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height();
    viewport[2] = width();
    viewport[3] = -height();

    float in[] = new float[4];
    float out[] = new float[4];

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
    in[3] = 1.0f;

    /* Map x and y from window coordinates */
    in[0] = (in[0] - viewport[0]) / viewport[2];
    in[1] = (in[1] - viewport[1]) / viewport[3];

    /* Map to range -1 to 1 */
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;

    projectionViewInverseMatrix.multiply(in, out);
    if (out[3] == 0)
      return false;

    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];

    objCoordinate[0] = out[0];
    objCoordinate[1] = out[1];
    objCoordinate[2] = out[2];

    return true;
  }

  /**
   * Returns the radius of the graph observed by the eye in graph (world) units.
   * <p>
   * In the case of a 3D eye you need to provide such an approximation of the
   * graph dimensions so that the it can adapt its
   * {@link #zNear()} and
   * {@link #zFar()} values. See the {@link #center()}
   * documentation.
   * <p>
   * Note that {@link Graph#radius()} (resp.
   * {@link Graph#setRadius(float)} simply call this
   * method on its associated eye.
   *
   * @see #setBoundingBox(Vector, Vector)
   */
  public float radius() {
    return scnRadius;
  }

  /**
   * Returns the position of the graph center, defined in the world coordinate system.
   * <p>
   * The graph observed by the eye should be roughly centered on this position, and
   * included in a {@link #radius()} ball.
   * <p>
   * Default value is the world origin. Use {@link #setCenter(Vector)} to change it.
   *
   * @see #setBoundingBox(Vector, Vector)
   * @see #zNear()
   * @see #zFar()
   */
  public Vector center() {
    return scnCenter;
  }

  /**
   * The point the eye revolves around with the ROTATE action binding. Defined in world
   * coordinate system.
   * <p>
   * Default value is the {@link #center()}.
   * <p>
   * <b>Attention:</b> {@link #setCenter(Vector)} changes this value.
   */
  public Vector anchor() {
    return anchorPnt;
  }

  /**
   * Sets the {@link #anchor()}, defined in the world coordinate system.
   */
  public void setAnchor(Vector rap) {
    if(is2D()) {
      anchorPnt = rap;
      anchorPnt.setZ(0);
    }
    else {
      float prevDist = distanceToAnchor();
      this.anchorPnt = rap;
      float newDist = distanceToAnchor();
      if (prevDist != 0 && newDist != 0)
        rapK *= prevDist / newDist;
    }
  }

  /**
   * Returns the eye position to {@link #anchor()} distance in Scene units.
   * <p>
   * 3D Cameras return the projected eye position() to {@link #anchor()} distance
   * along the Camera Z axis and use it in {@link #getBoundaryWidthHeight(float[])} so
   * that when the Camera is translated forward then its frustum is narrowed, making the
   * object appear bigger on screen, as intuitively expected.
   */
  public float distanceToAnchor() {
    Vector zCam = eye().zAxis();
    Vector cam2anchor = Vector.subtract(eye().position(), anchor());
    return Math.abs(Vector.dot(cam2anchor, zCam));
  }

  /**
   * Sets the {@link #radius()} value in graph (world) units. Negative values are
   * ignored. It also sets {@link Node#flySpeed()} to 1% of {@link #radius()}.
   */
  public void setRadius(float radius) {
    if (radius <= 0.0f) {
      System.out.println("Warning: Scene radius must be positive - Ignoring value");
      return;
    }
    scnRadius = radius;
    if(eye() instanceof Node)
      ((Node)eye()).setFlySpeed(0.01f * radius());
    if(motionAgent() != null)
      for (Grabber mg : motionAgent().grabbers()) {
        if (mg instanceof Node)
          ((Node) mg).setFlySpeed(0.01f * radius());
      }
    // TODO previous was:
    //if(is3D())
    //setFocusDistance(sceneRadius() / (float) Math.tan(fieldOfView() / 2.0f));
  }

  /**
   * Sets the {@link #center()} of the Scene.
   * <p>
   * Convenience wrapper function that simply calls {@code }
   *
   * @see #setRadius(float)
   */
  public void setCenter(Vector center) {
    scnCenter = center;
    setAnchor(center());
  }

  /**
   * Similar to {@link #setRadius(float)} and {@link #setCenter(Vector)}, but the
   * graph limits are defined by a (world axis aligned) bounding box.
   */
  public void setBoundingBox(Vector min, Vector max) {
    //TODO check 2d case
    setCenter(Vector.multiply(Vector.add(min, max), 1 / 2.0f));
    setRadius(0.5f * (Vector.subtract(max, min)).magnitude());
  }

  /**
   * Moves the eye so that the entire graph is visible.
   * <p>
   * Simply calls {@link #fitBall(Vector, float)} on a sphere defined by
   * {@link #center()} and {@link #radius()}.
   * <p>
   * You will typically use this method at init time after you defined a new
   * {@link #radius()}.
   */
  public void showAll() {
    fitBall(center(), radius());
  }

  /**
   * Returns the current {@link #eye()} type.
   */
  public final Type eyeType() {
    return tp;
  }

  /**
   * Defines the Camera {@link #type()}.
   * <p>
   * Changing the Camera Type alters the viewport and the objects' size can be changed.
   * This method guarantees that the two frustum match in a plane normal to
   * {@link #viewDirection()}, passing through the arcball reference point.
   */
  public final void setEyeType(Type type) {
    if (type != type()) {
      modified();
      this.tp = type;
    }
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #eye()} (
   * {@code frame().inverseTransformOf(new Vector(0.0f, 0.0f, -1.0f))} ) whih in 2D always is
   * (0,0,-1)
   * <p>
   * In 3D change this value using
   * {@link #setViewDirection(Vector)}, {@link #lookAt(Vector)} or
   * {@link Node#setOrientation(Quaternion)} . It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    //TODO test me
    //before it was:
    //if(graph.is2D())
    //return new Vector(0, 0, (frame().zAxis().z() > 0) ? -1 : 1);
    //bu now I think we should simply go something like this:
    return eye().zAxis(false);
  }

  /**
   * Rotates the Camera so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The Camera {@link Node#position()} is not modified. The Camera is rotated so that the
   * horizon (defined by its {@link #upVector()}) is preserved.
   *
   * @see #lookAt(Vector)
   * @see #setUpVector(Vector)
   */
  public void setViewDirection(Vector direction) {
    if (direction.squaredNorm() == 0)
      return;

    Vector xAxis = direction.cross(upVector());
    if (xAxis.squaredNorm() == 0) {
      // target is aligned with upVector, this means a rotation around X axis
      // X axis is then unchanged, let's keep it !
      xAxis = eye().xAxis();
    }

    Quaternion q = new Quaternion();
    q.fromRotatedBasis(xAxis, xAxis.cross(direction), Vector.multiply(direction, -1));
    eye().setOrientationWithConstraint(q);
  }

  /**
   * Convenience function that simply calls {@code setUpVector(up, true)}.
   *
   * @see #setUpVector(Vector, boolean)
   */
  public void setUpVector(Vector up) {
    setUpVector(up, true);
  }

  /**
   * Rotates the Eye so that its {@link #upVector()} becomes {@code up} (defined in the
   * world coordinate system).
   * <p>
   * The Eye is rotated around an axis orthogonal to {@code up} and to the current
   * {@link #upVector()} direction.
   * <p>
   * Use this method in order to define the Eye horizontal plane.
   * <p>
   * When {@code noMove} is set to {@code false}, the orientation modification is
   * compensated by a translation, so that the {@link #anchor()} stays projected at the
   * same position on screen. This is especially useful when the Eye is an observer of the
   * graph (default action binding).
   * <p>
   * When {@code noMove} is true, the Eye {@link Node#position()} is left unchanged, which is
   * an intuitive behavior when the Eye is in first person mode.
   *
   * @see #lookAt(Vector)
   */
  public void setUpVector(Vector up, boolean noMove) {
    Quaternion q = new Quaternion(new Vector(0.0f, 1.0f, 0.0f), eye().transformOf(up));

    if (!noMove && is3D())
      eye().setPosition(Vector.subtract(anchor(),
              (Quaternion.multiply(eye().orientation(), q)).rotate(eye().coordinatesOf(anchor()))));

    eye().rotate(q);

    // Useful in fly mode to keep the horizontal direction.
    updateSceneUpVector();
  }

  /**
   * Returns the normalized up vector of the eye, defined in the world coordinate system.
   * <p>
   * Set using {@link #setUpVector(Vector)} or {@link Node#setOrientation(Quaternion)}. It is
   * orthogonal to {@link #viewDirection()} and to {@link #rightVector()}.
   * <p>
   * It corresponds to the Y axis of the associated {@link #eye()} (actually returns
   * {@code frame().yAxis()}
   */
  public Vector upVector() {
    return eye().yAxis();
  }

  /**
   * 2D Windows simply call {@code frame().setPosition(target.x(), target.y())}. 3D
   * Cameras set {@link Node#orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The Camera {@link Node#position()} is not modified.
   * Simply {@link #setViewDirection(Vector)}).
   *
   * @see #at()
   * @see #setUpVector(Vector)
   * @see #showAll()
   * @see #fitBall(Vector, float)
   * @see #fitBoundingBox(Vector, Vector)
   */
  public void lookAt(Vector target) {
    setViewDirection(Vector.subtract(target, eye().position()));
  }

  /**
   * Returns the normalized right vector of the eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}. Set using
   * {@link #setUpVector(Vector)}, {@link #lookAt(Vector)} or {@link Node#setOrientation(Quaternion)}.
   * <p>
   * Simply returns {@code frame().xAxis()}.
   */
  public Vector rightVector() {
    return eye().xAxis();
  }

  /**
   * 2D Windows return the postion. 3D Cameras return a point defined in the world
   * coordinate system where the camera is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   *
   * @see #lookAt(Vector)
   */
  public Vector at() {
    return Vector.add(eye().position(), viewDirection());
  }

  /**
   * Moves the eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   * <p>
   * In 3D the Camera is simply translated along its {@link #viewDirection()} so that the
   * sphere fits the screen. Its {@link Node#orientation()} and its
   * {@link #fieldOfView()} are unchanged. You should
   * therefore orientate the Camera before you call this method.
   *
   * @see #lookAt(Vector)
   * @see #setUpVector(Vector, boolean)
   */
  public void fitBall(Vector center, float radius) {
    float distance = 0.0f;
    switch (type()) {
      case PERSPECTIVE: {
        float yview = radius / (float) Math.sin(fieldOfView() / 2.0f);
        float xview = radius / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distance = Math.max(xview, yview);
        break;
      }
      case ORTHOGRAPHIC: {
        distance = Vector.dot(Vector.subtract(center, anchor()), viewDirection()) + (radius / eye().magnitude());
        break;
      }
    }

    Vector newPos = Vector.subtract(center, Vector.multiply(viewDirection(), distance));
    eye().setPositionWithConstraint(newPos);
  }

  /**
   * Moves the eye so that the (world axis aligned) bounding box ({@code min} ,
   * {@code max}) is entirely visible, using {@link #fitBall(Vector, float)}.
   */
  public void fitBoundingBox(Vector min, Vector max) {
    float diameter = Math.max(Math.abs(max.vec[1] - min.vec[1]), Math.abs(max.vec[0] - min.vec[0]));
    diameter = Math.max(Math.abs(max.vec[2] - min.vec[2]), diameter);
    fitBall(Vector.multiply(Vector.add(min, max), 0.5f), 0.5f * diameter);
  }

  /**
   * Moves the eye so that the rectangular screen region defined by {@code rectangle}
   * (pixel units, with origin in the upper left corner) fits the screen.
   * <p>
   * in 3D the Camera is translated (its {@link Node#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #center()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   */
  public void fitScreenRegion(Rectangle rectangle) {
    Vector vd = viewDirection();
    float distToPlane = distanceToSceneCenter();

    Point center = new Point((int) rectangle.centerX(), (int) rectangle.centerY());

    Vector orig = new Vector();
    Vector dir = new Vector();
    convertClickToLine(center, orig, dir);
    Vector newCenter = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));

    convertClickToLine(new Point(rectangle.x(), center.y()), orig, dir);
    final Vector pointX = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));

    convertClickToLine(new Point(center.x(), rectangle.y()), orig, dir);
    final Vector pointY = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));

    float distance = 0.0f;
    float distX, distY;
    switch (type()) {
      case PERSPECTIVE:
        distX = Vector.distance(pointX, newCenter) / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distY = Vector.distance(pointY, newCenter) / (float) Math.sin(fieldOfView() / 2.0f);
        distance = Math.max(distX, distY);
        break;
      case ORTHOGRAPHIC:
        float dist = Vector.dot(Vector.subtract(newCenter, anchor()), vd);
        distX = Vector.distance(pointX, newCenter) / eye().magnitude() / aspectRatio();
        distY = Vector.distance(pointY, newCenter) / eye().magnitude() / 1.0f;
        distance = dist + Math.max(distX, distY);
        break;
    }

    eye().setPositionWithConstraint(Vector.subtract(newCenter, Vector.multiply(vd, distance)));
  }

  /**
   * Gives the coefficients of a 3D half-line passing through the Camera eye and pixel
   * (x,y). Origin in the upper left corner. Use {@link #height()} - y to locate the
   * origin at the lower left corner.
   * <p>
   * The origin of the half line (eye position) is stored in {@code orig}, while
   * {@code dir} contains the properly oriented and normalized direction of the half line.
   * <p>
   * This method is useful for analytical intersection in a selection method.
   */
  public void convertClickToLine(final Point pixelInput, Vector orig, Vector dir) {
    Point pixel = new Point(pixelInput.x(), pixelInput.y());

    // lef-handed coordinate system correction
    if (isLeftHanded())
      pixel.setY(height() - pixelInput.y());

    switch (type()) {
      case PERSPECTIVE:
        orig.set(eye().position());
        dir.set(new Vector(((2.0f * pixel.x() / width()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f) * aspectRatio(),
                ((2.0f * (height() - pixel.y()) / height()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f),
                -1.0f));
        dir.set(Vector.subtract(eye().inverseCoordinatesOf(dir), orig));
        dir.normalize();
        break;

      case ORTHOGRAPHIC: {
        float[] wh = getBoundaryWidthHeight();
        orig.set(
                new Vector((2.0f * pixel.x() / width() - 1.0f) * wh[0], -(2.0f * pixel.y() / height() - 1.0f) * wh[1],
                        0.0f));
        orig.set(eye().inverseCoordinatesOf(orig));
        dir.set(viewDirection());
        break;
      }
    }
  }

  // WARNINGS and EXCEPTIONS STUFF

  static protected HashMap<String, Object> warnings;

  /**
   * Show warning, and keep track of it so that it's only shown once.
   *
   * @param msg the error message (which will be stored for later comparison)
   */
  static public void showWarning(String msg) { // ignore
    if (warnings == null) {
      warnings = new HashMap<String, Object>();
    }
    if (!warnings.containsKey(msg)) {
      System.err.println(msg);
      warnings.put(msg, new Object());
    }
  }

  /**
   * Display a warning that the specified method is only available in 3D.
   *
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarning(String method) {
    showWarning(method + "() is not available in 2d");
  }

  /**
   * Display a warning that the specified method lacks implementation.
   */
  static public void showMissingImplementationWarning(String method, String theclass) {
    showWarning(method + "(), should be implemented by your " + theclass + " derived class.");
  }

  /**
   * Display a warning that the specified method can only be implemented from a relative
   * bogus event.
   */
  static public void showEventVariationWarning(String method) {
    showWarning(method + " can only be performed using a relative event.");
  }

  /**
   * Same as {@code showOnlyEyeWarning(method, true)}.
   *
   * @see #showOnlyEyeWarning(String, boolean)
   */
  static public void showOnlyEyeWarning(String method) {
    showOnlyEyeWarning(method, true);
  }

  /**
   * Display a warning that the specified method is only available for an eye-frame if
   * {@code eye} is {@code true} or a frame, different than an eye-frame, if {@code eye}
   * is {@code false}.
   */
  static public void showOnlyEyeWarning(String method, boolean eye) {
    if (eye)
      showWarning(method + "() is meaningful only when frame is attached to an eye.");
    else
      showWarning(method + "() is meaningful only when frame is detached from an eye.");
  }

  static public void showMinDOFsWarning(String themethod, int dofs) {
    showWarning(themethod + "() requires at least a " + dofs + " dofs.");
  }

  // NICE STUFF

  /**
   * Apply the local transformation defined by {@code frame}, i.e., respect to the frame
   * {@link Frame#reference()}. The Frame is first translated
   * and then rotated around the new translated origin.
   * <p>
   * This method may be used to modify the modelview matrix from a Frame hierarchy. For
   * example, with this Frame hierarchy:
   * <p>
   * {@code Frame body = new Frame();} <br>
   * {@code Frame leftArm = new Frame();} <br>
   * {@code Frame rightArm = new Frame();} <br>
   * {@code leftArm.setReference(body);} <br>
   * {@code rightArm.setReference(body);} <br>
   * <p>
   * The associated drawing code should look like:
   * <p>
   * {@code pushModelView();} <br>
   * {@code applyTransformation(body);} <br>
   * {@code drawBody();} <br>
   * {@code pushModelView();} <br>
   * {@code applyTransformation(leftArm);} <br>
   * {@code drawArm();} <br>
   * {@code popMatrix();} <br>
   * {@code pushMatrix();} <br>
   * {@code applyTransformation(rightArm);} <br>
   * {@code drawArm();} <br>
   * {@code popModelView();} <br>
   * {@code popModelView();} <br>
   * <p>
   * Note the use of nested {@link #pushModelView()} and {@link #popModelView()} blocks to
   * represent the frame hierarchy: {@code leftArm} and {@code rightArm} are both
   * correctly drawn with respect to the {@code body} coordinate system.
   * <p>
   * <b>Attention:</b> When drawing a frame hierarchy as above, this method should be used
   * whenever possible.
   *
   * @see #applyWorldTransformation(Frame)
   */
  public void applyTransformation(Frame frame) {
    if (is2D()) {
      translate(frame.translation().x(), frame.translation().y());
      rotate(frame.rotation().angle());
      scale(frame.scaling(), frame.scaling());
    } else {
      translate(frame.translation().vec[0], frame.translation().vec[1], frame.translation().vec[2]);
      rotate(frame.rotation().angle(), ((Quaternion) frame.rotation()).axis().vec[0], ((Quaternion) frame.rotation()).axis().vec[1],
          ((Quaternion) frame.rotation()).axis().vec[2]);
      scale(frame.scaling(), frame.scaling(), frame.scaling());
    }
  }

  /**
   * Same as {@link #applyTransformation(Frame)} but applies the global transformation
   * defined by the frame.
   */
  public void applyWorldTransformation(Frame frame) {
    Frame refFrame = frame.reference();
    if (refFrame != null) {
      applyWorldTransformation(refFrame);
      applyTransformation(frame);
    } else {
      applyTransformation(frame);
    }
  }

  /**
   * This method is called before the first drawing happen and should be overloaded to
   * initialize stuff. The default implementation is empty.
   */
  public void init() {
  }

  /**
   * The method that actually defines the graph.
   * <p>
   * If you build a class that inherits from Scene, this is the method you should
   * overload, but no if you instantiate your own Scene object (for instance, in
   * Processing you should just overload {@code PApplet.draw()} to define your graph).
   * <p>
   * The eye matrices set in {@link MatrixHandler#bind()} converts from the world to the camera
   * coordinate systems. Thus vertices given here can then be considered as being given in
   * the world coordinate system. The eye is moved in this world using the mouse. This
   * representation is much more intuitive than a camera-centric system (which for
   * instance is the standard in OpenGL).
   */
  public void proscenium() {
  }

  // GENERAL STUFF

  /**
   * Returns true if graph is left handed. Note that the graph is right handed by default.
   * However in proscene we set it as right handed (same as with P5).
   *
   * @see #setLeftHanded()
   */
  public boolean isLeftHanded() {
    return !rightHanded;
  }

  /**
   * Returns true if graph is right handed. Note that the graph is right handed by
   * default. However in proscene we set it as right handed (same as with P5).
   *
   * @see #setRightHanded()
   */
  public boolean isRightHanded() {
    return rightHanded;
  }

  /**
   * Set the graph as right handed.
   *
   * @see #isRightHanded()
   */
  public void setRightHanded() {
    rightHanded = true;
  }

  /**
   * Set the graph as left handed.
   *
   * @see #isLeftHanded()
   */
  public void setLeftHanded() {
    rightHanded = false;
  }

  /**
   * @return true if the graph is 2D.
   */
  public boolean is2D() {
    return twod;
  }

  /**
   * @return true if the graph is 3D.
   */
  public boolean is3D() {
    return !is2D();
  }
}