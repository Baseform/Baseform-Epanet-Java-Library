package org.addition.epanetold.Types;


public class Label {
    String text;
    Point position;

    public Label() {
        position = new Point();
        text = "";
    }

    public String getText() {

        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }
}
