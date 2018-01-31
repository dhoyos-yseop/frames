/**
 * Basic Use.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates a direct approach to use proscene by Scene proper
 * instantiation.
 */

import proscene.input.*;
import proscene.input.event.*;
import proscene.core.*;
import proscene.processing.*;

Scene scene;
//Choose P2D or P3D
String renderer = P2D;

Node node;
float length = 100;
PGraphics pg;

void setup() {
  size(800, 800, renderer);
  pg = this.g;
  rectMode(CENTER);
  scene = new Scene(this);
  scene.setRadius(200);

  node = new Node(scene) {
    @Override
    public void visit() {
      graphics(pg);
    }

    @Override
    public void interact(MotionEvent2 event) {
      Shortcut left = new Shortcut(PApplet.LEFT);
      Shortcut right = new Shortcut(PApplet.RIGHT);
      if (left.matches(event.shortcut()))
        rotate(event);
      if (right.matches(event.shortcut()))
        screenTranslate(event);
    }

    @Override
    public void interact(MotionEvent1 event) {
      if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
        scale((MotionEvent1) event);
    }

    @Override
    public void interact(proscene.input.event.KeyEvent event) {
      KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
      KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
      KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
      KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);
      if (event.shortcut().matches(upArrow))
        translateYPos();
      else if (event.shortcut().matches(downArrow))
        translateYNeg();
      else if (event.shortcut().matches(leftArrow))
        translateXNeg();
      else if (event.shortcut().matches(rightArrow))
        translateXPos();
    }
  };

  node.setPrecision(Node.Precision.ADAPTIVE);
  node.setPrecisionThreshold(length);
  node.translate(50, 50);

  scene.setDefaultNode(node);
  scene.fitBallInterpolation();
}

void graphics(PGraphics pg) {
  pg.fill(255, 0, 255);
  pg.rect(0, 0, length, length);
}

void draw() {
  background(0);
  scene.traverse();
}