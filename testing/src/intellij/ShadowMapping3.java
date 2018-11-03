package intellij;

import frames.core.Graph;
import frames.primitives.Matrix;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class ShadowMapping3 extends PApplet {
  Matrix matrix;
  Scene scene;
  Shape[] shapes;
  Shape light;
  boolean show = true;
  PGraphics shadowMap;
  float zNear = 50;
  float zFar = 500;
  boolean ortho = true;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    shapes = new Shape[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Shape(scene);
      shapes[i].setGraphics(caja());
      shapes[i].randomize();
    }
    light = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pg) {
        pg.pushStyle();
        Scene.drawAxes(pg, 150);
        pg.fill(isTracked() ? 255 : 25, isTracked() ? 0 : 255, 255);
        if (ortho)
          Scene.drawOrthographicVolume(pg, this, zNear, zFar, shadowMap);
        else
          Scene.drawPerspectiveVolume(pg, this, zNear, zFar, shadowMap);
        pg.popStyle();
      }
    };
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();
    shadowMap = createGraphics(w / 2, h / 2, P3D);
    setVolume();
  }

  public void draw() {
    background(90, 80, 125);
    // 1. Fill in and display front-buffer
    scene.traverse();
    // 2. Display shadow map
    shadowMap.beginDraw();
    shadowMap.background(120);
    scene.traverse(shadowMap, light, matrix);
    shadowMap.endDraw();
    // 3. display shadow map
    if (show) {
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
  }

  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
    setVolume();
  }

  public void keyPressed() {
    if (key == ' ')
      show = !show;
    if (key == 'o') {
      ortho = !ortho;
      setVolume();
    }
    if (key == 't')
      if (scene.type() == Graph.Type.PERSPECTIVE)
        scene.setAperture(Graph.Type.ORTHOGRAPHIC);
      else
        scene.setAperture(Graph.Type.PERSPECTIVE);
    if (key == 'f')
      scene.flip();
  }

  public void setVolume() {
    matrix = ortho ? light.orthographic(shadowMap.width, shadowMap.height, zNear, zFar) :
        light.perspective(shadowMap.width / shadowMap.height, zNear, zFar);
    println(light.magnitude());
    matrix.print();
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMapping3"});
  }
}
