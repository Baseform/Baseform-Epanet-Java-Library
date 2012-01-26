package org.addition.epanetold.ui;

import org.addition.epanetold.Network;
import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.Label;

import java.awt.*;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.*;

/***
 *
 * todo: draw other elements (pumps, valves and labels)
 */
public class NetworkImage {
    static private Color TANKS_FILL_COLOR = new Color(0xcccccc);
    static private Color TANKS_STROKE_COLOR = new Color(0x666666);
    static private int TANK_DIAM = 10;
    
    static private Color RESERVOIRS_FILL_COLOR = new Color(0x666666);
    static private Color RESERVOIRS_STROKE_COLOR = new Color(0xcccccc);
    static private int RESERVOIR_DIAM = 10;
    
    private static final Color PIPES_FILL_COLOR = new Color(0x666666);

    private static final Color NODE_STROKE_COLOR = new Color(0, 0, 0, .5f);
    private static final Color NODE_FILL_COLOR = new Color(0xcc / 256f, 0xcc / 256f, 0xcc / 256f, .4f);
    private static final int NODE_DIAM = 2;

    static private Color LABEL_COLOR = new Color(0,0,0);

    public static void drawNetwork(Graphics g, int w, int h, Network net, boolean drawPipes, boolean drawTanks, boolean drawNodes) {
        int maxNodes = net.getMaxNodes();
        int maxLinks = net.getMaxLinks();

        Rectangle2D.Double bounds = null;
        for (int i = 1; i <= maxNodes; i++) {
            Node node = net.getNode(i);
            org.addition.epanetold.Types.Point position = node.getPosition();
            if (position != null) {
                if (bounds == null)
                    bounds = new Rectangle2D.Double((int) position.getX(), (int) -position.getY(), 0, 0);
                else
                    bounds.add(new Point((int) position.getX(), (int) -position.getY()));
            }
        }
        for (int i = 1; i <= maxLinks; i++) {
            Link link = net.getLink(i);
            java.util.List<org.addition.epanetold.Types.Point> vertices = link.getVertices();
            for (org.addition.epanetold.Types.Point position : vertices) {
                if (position != null) {
                    if (bounds == null)
                        bounds = new Rectangle2D.Double((int) position.getX(), (int) -position.getY(), 0, 0);
                    else
                        bounds.add(new Point((int) position.getX(), (int) -position.getY()));
                }
            }
        }

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g.setColor(new Color(0x99, 0x99, 0x99));
        //g.drawRect(0, 0, w - 1, h - 1);

        double factor = (bounds.width / bounds.height) < (((double) w) / h) ? h / bounds.height : w / bounds.width;

        double dx = bounds.getMinX();
        double dy = bounds.getMinY();
        double dwidth = Math.abs(bounds.getMaxX() - bounds.getMinX());
        double dheight = Math.abs(bounds.getMaxY() - bounds.getMinY());

        factor *= .9d;

        dx += dwidth * .5d - w * 0.5 / factor;
        dy += dheight * .5d - h * 0.5 / factor;

        int maxPumps = net.getSections(EnumVariables.SectType._PUMPS);

        //tanks
        if (drawTanks) {
            for (Tank tank : net.getTanksArray()) {
                Node node = net.getNode(tank.getNode());
                org.addition.epanetold.Types.Point position = node.getPosition();
                if (position != null) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    g.setColor(TANKS_FILL_COLOR);
                    g.fillRect(point.x - TANK_DIAM / 2, point.y - TANK_DIAM / 2, TANK_DIAM, TANK_DIAM);
                    g.setColor(TANKS_STROKE_COLOR);
                    g.drawRect(point.x - TANK_DIAM / 2, point.y - TANK_DIAM / 2, TANK_DIAM, TANK_DIAM);
                }
            }
            for (Tank reservoir : net.getReservoirsArray()) {
                Node node = net.getNode(reservoir.getNode());
                org.addition.epanetold.Types.Point position = node.getPosition();
                if (position != null) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    g.setColor(RESERVOIRS_FILL_COLOR);
                    g.fillRect(point.x - RESERVOIR_DIAM / 2, point.y - RESERVOIR_DIAM / 2, RESERVOIR_DIAM, RESERVOIR_DIAM);
                    g.setColor(RESERVOIRS_STROKE_COLOR);
                    g.drawRect(point.x - RESERVOIR_DIAM / 2, point.y - RESERVOIR_DIAM / 2, RESERVOIR_DIAM, RESERVOIR_DIAM);
                }
            }
        }

        if (drawPipes) {
            //links
            g.setColor(PIPES_FILL_COLOR);
            for (int i = 1; i <= maxLinks; i++) {
                Link link = net.getLink(i);
                java.util.List<org.addition.epanetold.Types.Point> vertices = new ArrayList<org.addition.epanetold.Types.Point>(link.getVertices());
                Node node1 = net.getNode(link.getN1());
                Node node2 = net.getNode(link.getN2());
                vertices.add(0, node1.getPosition());
                vertices.add(node2.getPosition());
                Point prev = null;
                for (org.addition.epanetold.Types.Point position : vertices) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    if (prev != null) {
                        g.drawLine(prev.x, prev.y, point.x, point.y);
                    }
                    prev = point;

                }
            }
        }

        if (drawNodes) {
            //nodes
            Color nodefillColor = NODE_FILL_COLOR;
            Color nodeStrokeColor = NODE_STROKE_COLOR;
            g.setColor(nodefillColor);
            for (int i = 1; i <= maxNodes; i++) {
                Node node = net.getNode(i);
                org.addition.epanetold.Types.Point position = node.getPosition();
                if (position != null) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    g.setColor(nodefillColor);
                    g.fillOval(point.x - NODE_DIAM / 2, point.y - NODE_DIAM / 2, NODE_DIAM, NODE_DIAM);
                    g.setColor(nodeStrokeColor);
                    g.drawOval(point.x - NODE_DIAM / 2, point.y - NODE_DIAM / 2, NODE_DIAM, NODE_DIAM);
                }
            }
        }

        for (int i = 1; i <= net.getSections(EnumVariables.SectType._LABELS); i++) {
            Label l = net.getLabel(i);

            Point point = new Point(
                    (int) ((l.getPosition().getX() - dx) * factor),
                    (int) ((-l.getPosition().getY() - dy) * factor));
            g.setColor(LABEL_COLOR);

            g.drawString(l.getText(),(int)point.getX(),(int)point.getY());
        }


    }

}
