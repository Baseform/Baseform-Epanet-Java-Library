/*
 * Copyright (C) 2012  Addition, Lda. (addition at addition dot pt)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package org.addition.epanet.ui;

import org.addition.epanet.network.Network;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.network.structures.Label;
import org.addition.epanet.network.structures.Point;


import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Network 2D rendering class for the EpanetUI class.
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

    /**
     * Get the nearest node to the mouse cursor position.
     * @param w Canvas width.
     * @param h Canvas height.
     * @param x Mouse x position.
     * @param y Mouse y position.
     * @param net Epanet network.
     * @return Reference to the nearest node.
     */
    public static Node peekNearest(int w, int h,int x,int y, Network net){
        if(net==null)
            return null;

        Rectangle2D.Double bounds = null;
        for(Node node : net.getNodes()){
            if (node.getPosition() != null) {
                if (bounds == null)
                    bounds = new Rectangle2D.Double((int) node.getPosition().getX(), (int) -node.getPosition().getY(), 0, 0);
                else
                    bounds.add(new java.awt.Point((int) node.getPosition().getX(), (int) -node.getPosition().getY()));
            }
        }
        for(Link link : net.getLinks()){
            //java.util.List<org.addition.epanetold.Types.Point> vertices = link.getVertices();
            for (Point position : link.getVertices()) {
                if (position != null) {
                    if (bounds == null)
                        bounds = new Rectangle2D.Double((int) position.getX(), (int) -position.getY(), 0, 0);
                    else
                        bounds.add(new java.awt.Point((int) position.getX(), (int) -position.getY()));
                }
            }
        }

        double factor = (bounds.width / bounds.height) < (((double) w) / h) ? h / bounds.height : w / bounds.width;

        double dx = bounds.getMinX();
        double dy = bounds.getMinY();
        double dwidth = Math.abs(bounds.getMaxX() - bounds.getMinX());
        double dheight = Math.abs(bounds.getMaxY() - bounds.getMinY());

        factor *= .9d;

        dx += dwidth * .5d - w * 0.5 / factor;
        dy += dheight * .5d - h * 0.5 / factor;

        Node nearest = null;
        double distance = 0;
        for(Node n : net.getNodes()){
            Point point = new Point(
                    (int) ((n.getPosition().getX() - dx) * factor),
                    (int) ((-n.getPosition().getY() - dy) * factor));
            double dist = Math.sqrt( Math.pow(point.getX() - x,2) +  Math.pow(point.getY() - y,2));
            if(nearest == null || dist< distance){

                nearest = n;
                distance = dist;
            }
        }

        return nearest;
    }

    /**
     * Render the network in the canvas.
     * @param g Reference to the canvas graphics.
     * @param w Canvas width.
     * @param h Canvas height.
     * @param net Epanet network.
     * @param drawPipes Draw pipes flag.
     * @param drawTanks Draw tanks flag.
     * @param drawNodes Draw nodes flag.
     * @param selNode Reference to the selected node.
     */
    public static void drawNetwork(Graphics g, int w, int h, Network net, boolean drawPipes, boolean drawTanks, boolean drawNodes, Node selNode) {
        //int maxNodes = net.getMaxNodes();
        //int maxLinks = net.getMaxLinks();

        Rectangle2D.Double bounds = null;
        for(Node node : net.getNodes()){
            if (node.getPosition() != null) {
                if (bounds == null)
                    bounds = new Rectangle2D.Double((int) node.getPosition().getX(), (int) -node.getPosition().getY(), 0, 0);
                else
                    bounds.add(new java.awt.Point((int) node.getPosition().getX(), (int) -node.getPosition().getY()));
            }
        }
        for(Link link : net.getLinks()){
            //java.util.List<org.addition.epanetold.Types.Point> vertices = link.getVertices();
            for (Point position : link.getVertices()) {
                if (position != null) {
                    if (bounds == null)
                        bounds = new Rectangle2D.Double((int) position.getX(), (int) -position.getY(), 0, 0);
                    else
                        bounds.add(new java.awt.Point((int) position.getX(), (int) -position.getY()));
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


        //tanks
        if (drawTanks) {
            for(Tank tank : net.getTanks()){
                if(tank.getArea()==0){
                    // Reservoir
                    Point position = tank.getPosition();
                    if (position != null) {
                        Point point = new Point(
                                (int) ((position.getX() - dx) * factor),
                                (int) ((-position.getY() - dy) * factor));
                        g.setColor(RESERVOIRS_FILL_COLOR);
                        g.fillRect((int)(point.getX() - RESERVOIR_DIAM / 2),(int)(point.getY()-RESERVOIR_DIAM/2), RESERVOIR_DIAM, RESERVOIR_DIAM);
                        g.setColor(RESERVOIRS_STROKE_COLOR);
                        g.drawRect((int)(point.getX() - RESERVOIR_DIAM / 2),(int)(point.getY()-RESERVOIR_DIAM/2), RESERVOIR_DIAM, RESERVOIR_DIAM);
                    }
                }
                else {
                    // Tank
                    Point position = tank.getPosition();
                    if (position != null) {
                        Point point = new Point(
                                (int) ((position.getX() - dx) * factor),
                                (int) ((-position.getY() - dy) * factor));
                        g.setColor(TANKS_FILL_COLOR);
                        g.fillRect((int)(point.getX() - RESERVOIR_DIAM / 2),(int)(point.getY()-RESERVOIR_DIAM/2), TANK_DIAM, TANK_DIAM);
                        g.setColor(TANKS_STROKE_COLOR);
                        g.drawRect((int)(point.getX() - RESERVOIR_DIAM / 2),(int)(point.getY()-RESERVOIR_DIAM/2), TANK_DIAM, TANK_DIAM);
                    }
                }
            }
        }

        if (drawPipes) {
            //links
            g.setColor(PIPES_FILL_COLOR);
            for(Link link : net.getLinks()){
                List<Point> vertices = new ArrayList<Point>(link.getVertices());
                Node node1 = link.getFirst();
                Node node2 = link.getSecond();
                vertices.add(0, node1.getPosition());
                vertices.add(node2.getPosition());
                Point prev = null;
                for (Point position : vertices) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    if (prev != null) {
                        g.drawLine((int)prev.getX(),(int) prev.getY(),(int) point.getX(),(int) point.getY());
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
            for(Node node : net.getNodes()){
                Point position = node.getPosition();
                if (position != null) {
                    Point point = new Point(
                            (int) ((position.getX() - dx) * factor),
                            (int) ((-position.getY() - dy) * factor));
                    g.setColor(nodefillColor);
                    g.fillOval((int)(point.getX() - NODE_DIAM / 2), (int)(point.getY() - NODE_DIAM / 2), NODE_DIAM, NODE_DIAM);
                    g.setColor(nodeStrokeColor);
                    g.drawOval((int)(point.getX() - NODE_DIAM / 2), (int)(point.getY() - NODE_DIAM / 2), NODE_DIAM, NODE_DIAM);
                }
            }
        }

        for(Label l : net.getLabels()){
            Point point = new Point(
                    (int) ((l.getPosition().getX() - dx) * factor),
                    (int) ((-l.getPosition().getY() - dy) * factor));
            g.setColor(LABEL_COLOR);
            g.drawString(l.getText(),(int)point.getX(),(int)point.getY());
        }

        if(selNode!=null){
            Point point = new Point(
                    (int) ((selNode.getPosition().getX() - dx) * factor),
                    (int) ((-selNode.getPosition().getY() - dy) * factor));

            g.setColor(new Color(0xFF0000));
            g.drawOval((int)(point.getX() - 20 / 2), (int)(point.getY() - 20 / 2), 20, 20);

            g.setColor(LABEL_COLOR);
            g.drawString(selNode.getId(),(int)point.getX()+20,(int)point.getY()+20);
        }


    }

}
