/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * This example implements the attached-frames version of the detached-frames
 * version example having the same name.
 *
 * The sphere and the boxes are interactive. Pick and drag them with the
 * right mouse button. Use also the arrow keys to select and move the sphere.
 * See how the boxes will always remain oriented towards the sphere.
 *
 * Both the sphere and the boxes are implemented as attached-frames
 * instances having their visit() method overridden which get automatically
 * call by the scene traversal algorithm.
 *
 * Contrast this example with the detached-frame version with the same name.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Box[] cajas;
Sphere esfera;

void setup() {
  size(800, 800, P3D);
  scene = new Scene(this);
  scene.setRadius(200);
  scene.fitBall();
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  esfera = new Sphere();
  esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
  esfera.setColor(color(0, 0, 255));

  cajas = new Box[30];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box();

  scene.fitBallInterpolation();
  scene.setTrackedFrame("keyboard", esfera.iFrame);
}

void draw() {
  background(0);
  // calls visit() on all scene attached frames
  // automatically applying all the frame transformations
  scene.traverse();
}

void mouseMoved() {
  scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == 'e')
    scene.setType(Graph.Type.ORTHOGRAPHIC);
  if (key == 'E')
    scene.setType(Graph.Type.PERSPECTIVE);
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'S')
    scene.fitBall();
  if (key == 'u')
    if (scene.trackedFrame("keyboard") == null)
      scene.setTrackedFrame("keyboard", esfera.iFrame);
    else
      scene.resetTrackedFrame("keyboard");
  if (key == CODED)
    if (keyCode == UP)
      scene.translate("keyboard", 0, -10);
    else if (keyCode == DOWN)
      scene.translate("keyboard", 0, 10);
    else if (keyCode == LEFT)
      scene.translate("keyboard", -10, 0);
    else if (keyCode == RIGHT)
      scene.translate("keyboard", 10, 0);
}