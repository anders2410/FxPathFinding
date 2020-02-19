package swingui;

import old_model.*;
import paths.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Random;

public class SimpleUI extends JFrame {

    private Graph graph;
    private int coordToPos = 1000;
    private int zoom = 10000000;
    private int xoffset = -3900;
    private int yoffset = 7900;

    public SimpleUI(Graph graph) {
        this.graph = graph;
    }

    private JTextField xoffsetInfo, yoffsetInfo;

    public void generateUI() {
        JPanel panel = new JPanel();
        getContentPane().add(panel);
        setSize(640, 640);

        //Info text
        xoffsetInfo = new JTextField("" + xoffset);
        xoffsetInfo.addActionListener(e -> {
            String offsetString = xoffsetInfo.getText();
            xoffset = Integer.parseInt(offsetString);
            repaint();
        });
        panel.add(xoffsetInfo);

        yoffsetInfo = new JTextField("" + yoffset);
        xoffsetInfo.addActionListener(e -> {
            String offsetString = yoffsetInfo.getText();
            yoffset = Integer.parseInt(offsetString);
            repaint();
        });
        panel.add(yoffsetInfo);

        //Buttons
        JButton right = new JButton("left");
        right.addActionListener(e -> {
            xoffset += 100;
            repaint();
        });
        panel.add(right);

        JButton left = new JButton("right");
        left.addActionListener(e -> {
            xoffset -= 100;
            repaint();
        });
        panel.add(left);

        JButton up = new JButton("down");
        up.addActionListener(e -> {
            yoffset -= 100;
            repaint();
        });
        panel.add(up);

        JButton down = new JButton("up");
        down.addActionListener(e -> {
            yoffset += 100;
            repaint();
        });
        panel.add(down);

        JButton zoom = new JButton("up");
        down.addActionListener(e -> {
            yoffset += 100;
            repaint();
        });
        panel.add(down);

        JButton dPath = new JButton("d path");
        dPath.addActionListener(e -> {
            DijkOld.randomPath(graph, AlgorithmMode.DIJKSTRA);
            repaint();
        });
        panel.add(dPath);

        JButton aPath = new JButton("a path");
        aPath.addActionListener(e -> {
            DijkOld.randomPath(graph, AlgorithmMode.A_STAR_DIST);
            repaint();
        });
        panel.add(aPath);

        JButton newSeed = new JButton("new seed");
        newSeed.addActionListener(e -> {
            Dijkstra.seed = new Random().nextInt();
        });
        panel.add(newSeed);
    }

    @Override
    public void repaint() {
        super.repaint();
        xoffsetInfo.setText("" + xoffset);
        yoffsetInfo.setText("" + yoffset);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        drawEdges(g2d);
        //drawNodes(g2d);
    }

    private void drawNodes(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.BLACK);
        List<Node> nodeList = graph.getNodeList();
        for (Node nx : nodeList) {
            double x = projectCord(nx.latitude, xoffset);
            double y = projectCord(-nx.longitude, yoffset);
            //System.out.println("Node(" + x + ", " + y + ")");
            double xm = projectXCordMercator(nx.latitude, 2, -15336945000f) / 20000;
            double ym = projectYCordMercator(-nx.longitude, 2, 0);
            //System.out.println("NodeMercator(" + xm + ", " + ym + ")");
            g2d.setColor(Color.BLACK);
            Line2D line = new Line2D.Double(x, y, x, y);
            g2d.draw(line);
            /*g2d.setColor(Color.MAGENTA);
            Line2D line2 = new Line2D.Float(xm, ym, xm, ym);
            g2d.draw(line2);*/
        }
    }

    private void drawEdges(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.RED);
        List<Node> nodeList = graph.getNodeList();
        List<List<Edge>> adjList = graph.getAdjList();
        resetIsDrawn(adjList);
        for (int i = 0; i < adjList.size(); i++) {
            Node nx = nodeList.get(i);
            for (Edge edge : adjList.get(i)) {
                Node ny = nodeList.get(edge.to);
                Edge oppositeEdge = findOppositeEdge(adjList, i, edge);
                if (oppositeEdge == null || edge.isBetter(oppositeEdge)) {
                    drawEdge(g2d, nx, ny, edge);
                }
            }
        }
    }

    private Edge findOppositeEdge(List<List<Edge>> adjList, int i, Edge edge) {
        Edge oppositeEdge = null;
        for (Edge edgeTo : adjList.get(edge.to)) {
            if (edgeTo.to == i) {
                oppositeEdge = edgeTo;
            }
        }
        return oppositeEdge;
    }

    private void resetIsDrawn(List<List<Edge>> adjList) {
        for (List<Edge> edgeList : adjList) {
            for (Edge edge : edgeList) {
                edge.isDrawn = false;
            }
        }
    }

    private void drawEdge(Graphics2D g2d, Node nx, Node ny, Edge edge) {
        double x1 = projectCord(nx.latitude, xoffset);
        double y1 = projectCord(-nx.longitude, yoffset);
        double x2 = projectCord(ny.latitude, xoffset);
        double y2 = projectCord(-ny.longitude, yoffset);
        //System.out.println("(" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ")");
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(Color.BLACK);
        if (edge.visited) {
            g2d.setColor(Color.BLUE);
        }
        if (edge.inPath) {
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(Color.RED);
        }
        g2d.draw(line);
        edge.isDrawn = true;
        //g2d.drawString("" + Math.round(edge.d), (x1 + x2) * 0.5f, (y1 + y2) * 0.5f);
    }

    private float floatPi = (float) Math.PI;

    private float radConversionFactor = floatPi / 180;

    private float floatPiDivFour = floatPi / 4;

    private float pixels = 1;

    private float degreeToRadian(int degree) {
        return degree / 10000000f * radConversionFactor;
    }

    private float zoomFactor(float zoom) {
        return (float) (256 / (2 * floatPi) * Math.pow(2, zoom));
    }

    private double projectXCordMercator(double cord, float zoom, float shift) {
        return zoomFactor(zoom) * (cord + floatPi) * pixels + shift;
    }

    private float projectYCordMercator(double cord, float zoom, float shift) {
        return zoomFactor(zoom) * (floatPi - (float) Math.log(Math.tan(floatPiDivFour + cord / 2))) * pixels + shift;
    }

    private double projectCord(double cord, int shift) {
        return (cord % zoom) / coordToPos + shift;
    }

}
