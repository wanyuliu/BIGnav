package com.company;

import fr.inria.zvtm.engine.*;
import fr.inria.zvtm.glyphs.VCircle;
import fr.inria.zvtm.event.ViewAdapter;
import fr.inria.zvtm.event.ViewListener;
import fr.inria.zvtm.glyphs.*;
import fr.inria.zvtm.engine.View;
import fr.inria.zvtm.event.CameraListener;
import fr.inria.zvtm.animation.Animation;
import fr.inria.zvtm.animation.AnimationManager;
import fr.inria.zvtm.animation.interpolation.ConstantAccInterpolator;


import java.awt.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.Collections;
import java.lang.*;

/***
 * the following import is used for reading and recording experimental data
 */

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class Main {

    public Main(){
        this.init();
    }

    static ArrayList<Point> objects = new ArrayList<Point>();
    static double[][] goals = new double[100][100];
    static int targ;
    static int target_x, target_y;

    static VirtualSpaceManager vsm;
    static AnimationManager am;

    /***
    * In init(): set up the environment: the objects and how many; the spatial layout; the initial distribution; 
    which one is the target, the Index of Difficulty level. 
    */

    public void init(){


        // define virtual space manager
        vsm = VirtualSpaceManager.INSTANCE;

        // define virtual space
        VirtualSpace vs = vsm.addVirtualSpace(VirtualSpace.ANONYMOUS);

        // add camera
        Camera cam = vs.addCamera();
        Vector<Camera> cameras = new Vector<Camera>();
        cameras.add(cam);

        // define animation manager
        am = new AnimationManager(vsm);

        // define view
        View v = vsm.addFrameView(cameras, "fooview",View.STD_VIEW, 800, 600, true);
        v.setBackgroundColor(Color.darkGray);

        // define objects depending on the spatial layout. This is just an instance
        VCircle ci = new VCircle(kx, ky, 0, 30, Color.BLUE);

        // add them to the virtual space
        vs.addGlyph(ci);

        // add them to the objects array
        objects.add(new Point(kx, ky));

        // for each one of the objects, define initial probability distribution. Here an instance of uniform distribution
        // n is the total number of objects
        for (int i = 0; i < n; i++){
            goals[i][0] = objects.get(i).x;
            goals[i][1] = objects.get(i).y;
            goals[i][2] = 1/n;
        }
        
        // randomly draw a target from the set of objects
        targ = random.nextInt(n);
        target_x = objects.get(targ).x;
        target_y = objects.get(targ).y;

        // and add this target to virtual space
        tar = new VCircle(target_x, target_y, 0, 30, Color.red);
        vs.addGlyph(tar);

        // define two listeners and use only one at each time
        // if conventional pan and zoom
        ConListener cl = new ConListener();
        v.setListener(cl,cam);

        // if BIG navigation
        MainListener eh = new MainListener();
        v.setListener(eh,cam);

        // add a listener to camera
        Foo cc = new Foo();
        cam.addListener(cc);

    }

    public static void main(String[] args) {

        new Main();
    }
}


/***
This is BIGnav class
*/
class MainListener extends ViewAdapter implements ViewListener {

    int lastJPX, lastJPY;
    int endJPX, endJPY;
    static final float RBSSF = 50;
    static final float WSF = 5f;
    boolean click = false;
    int rightcx, rightcy;

    /***
    The following is already defined in ZVTM for enabling panning and zooming
    */

    // panning - mouse down
    public void press1(ViewPanel v, int mod, int jpx, int jpy, MouseEvent e){
        lastJPX = jpx;
        lastJPY = jpy;
    }
    // panning - mouse up 
    public void release1(ViewPanel v, int mod, int jpx, int jpy, MouseEvent e){

        endJPX = jpx;
        endJPY = jpy;
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double zlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        c.setXspeed(0);
        c.setYspeed(0);
        c.setZspeed(0);
        v.setDrawDrag(false);
        // newupdate is to update the computer's knowledge about the user's goal
        newupdate();
        // BestWP is to locate the view that maximizes the expected information gain from the user's subsequent input
        BestWP();
    }

    public void mouseDragged(ViewPanel v,int mod,int buttonNumber,int jpx,int jpy,
                             MouseEvent e){

        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double a = (c.focal + Math.abs(c.altitude)) / c.focal;
        if (buttonNumber == 1){
            c.setXspeed(
                    (c.altitude>0) ? (lastJPX-jpx)*(a/RBSSF)
                            : (lastJPX-jpx)/(a*RBSSF));
            c.setYspeed(
                    (c.altitude>0) ? (jpy-lastJPY)*(a/RBSSF)
                            : (jpy-lastJPY)/(a*RBSSF));
            c.setZspeed(0);
        }
    }

    public void click3(ViewPanel v, int mod, int jpx, int jpy, int clickNumber, MouseEvent e) {
        rightcx = jpx;
        rightcy = jpy;
        click = true;
    }

    // zooming in and out
    public void mouseWheelMoved(ViewPanel v, short wheelDirection, int jpx, int jpy, MouseWheelEvent e) {

        lastJPX = jpx;
        lastJPY = jpy;
        endJPX = jpx;
        endJPY = jpy;
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        //System.out.println(c.altitude);
        double zlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        double a = (c.focal + Math.abs(c.altitude)) / c.focal;
        //current window size
        double wx = 800 * zlevel;
        double wy = 600 * zlevel;

        if (wheelDirection == WHEEL_DOWN) {  //zoom in
            c.altitudeOffset(-a * WSF);
            VirtualSpaceManager.INSTANCE.repaint();
            newupdate();
            BestWP();
        } else {
            //wheelDirection == WHEEL_IN  //zoom out
            c.altitudeOffset(a * WSF);
            VirtualSpaceManager.INSTANCE.repaint();
        }
    }

    /***
    Starting from here - BIGnav core functions
    */

    // get angle between mouse down and mouse up for panning
    public double getAngle(int sx, int sy, int ex, int ey) {
        double theta = Math.atan2(ex - sx, ey - sy);
        theta += Math.PI/2.0;
        double angle = Math.toDegrees(theta);
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    // based on the angle, the panning direction is determined
    public int direction() {
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double zlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        int direction = 1;
        double theta = getAngle(endJPX, endJPY, lastJPX, lastJPY);
        if (!Main.conti) {
            if (endJPX == lastJPX && endJPY == lastJPY) {
                direction = 5;
            } else if (theta >= 22.5 && theta < 67.5) {
                direction = 3;
            } else if (theta > 67.5 && theta < 112.5) {
                direction = 2;
            } else if (theta >= 112.5 && theta < 157.5) {
                direction = 1;
            } else if (theta >= 157.5 && theta < 202.5) {
                direction = 4;
            } else if (theta >= 202.5 && theta < 247.5) {
                direction = 7;
            } else if (theta >= 247.5 && theta < 292.5) {
                direction = 8;
            } else if (theta >= 292.5 && theta < 337.5) {
                direction = 9;
            } else if ((theta >= 337.5 && theta < 360) || (theta >= 0 && theta < 22.5)) {
                direction = 6;
            }
        } else direction = Main.targ;

        return direction;
    }

    // update the computer's knowledge about the user's goal
    // update the probability of the objects
    // and calculate information gain after each operation

    double [][] updates = Main.goals;

    // this is the expected information gain calculation function
    // it calculates the expected information gain on objects given a particular view and user input on average
    public double New_IG(int vc_x, int vc_y, double zlevel){
        double EXP = 0;
        double BU = 0;
        double bu_temp = 0;
        double ubu_temp = 0;
        double UBU = 0;

        // Button uncertainty
        // input should consider panning, zooming and clicking on each object
        // so the input is from 0 to n (the number of objects) plus the panning and zooming inputs 
        // here user_input is a variable corresponding to the panning and zooming inputs in the specific applications
        // e.g. if you have 10 objects and 10 panning & zooming inputs, user_input = 10 + 10 = 20
        for (int input = 0; input < n + user_input; input++){
            if (P_BI(input, vc_x, vc_y, zlevel) != 0){
                bu_temp = - P_BI(input, vc_x, vc_y, zlevel) * Math.log(P_BI(input, vc_x, vc_y, zlevel));
            } else bu_temp = 0;
            BU = BU + bu_temp;
        }

        // User behaviour uncertainty
        for (int k = 0; k < n; k++){
            for (int i = 0; i < n + user_input; i++){
                if (Pr_BGI(i,k,(int)updates[k][0],(int)updates[k][1],vc_x,vc_y,zlevel) != 0){
                    ubu_temp = - Pr_BGI(i,k,(int)updates[k][0],(int)updates[k][1],vc_x,vc_y,zlevel) *
                            Math.log(Pr_BGI(i,k,(int)updates[k][0],(int)updates[k][1],vc_x,vc_y,zlevel)) * updates[k][2];
                } else ubu_temp = 0;
                UBU = UBU + ubu_temp;
            }
        }

        EXP = BU - UBU;

        // return the expected information gain
        return EXP;
    }

    // this is the function to see:
    // given a particular view, for each object, what is the correct direction
    public int output_region(int cam_x, int cam_y, int x, int y, double zlevel) {
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        // compare each one of the points to current camera location
        int button = 0;
        //double zlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        //current window size
        double winx = 800 * zlevel;
        double winy = 600 * zlevel;
        //double ang = getAngle((int) c.vx, (int) c.vy, x, y);
        double ang = getAngle(cam_x, cam_y, x, y);
        if ((x > (cam_x - winx / 2) && x < (cam_x + winx / 2) && y > (cam_y - winy / 2)
                && y < (cam_y + winy / 2)) && c.altitude == 0.0) {
            button = 10;
        } else if ((x > (cam_x - winx / 4) && x < (cam_x + winx / 4) && y > (cam_y - winy / 4)
                && y < (cam_y + winy / 4)) && c.altitude != 0.0){
            button = 5;
        } else if (ang >= 22.5 && ang < 67.5) {  // 1 -> 9
            button = 1;
        } else if (ang >= 67.5 && ang < 112.5) { //2 -> 8
            button = 2;
        } else if (ang >= 112.5 && ang < 157.5) { //3 -> 7
            button = 3;

        } else if (ang >= 157.5 && ang < 202.5) {  //6 -> 4
            button = 6;

        } else if (ang >= 202.5 && ang < 247.5) { //9 -> 1
            button = 9;

        } else if (ang >= 247.5 && ang < 292.5) { // 8 ->2
            button = 8;

        } else if (ang >= 292.5 && ang < 337.5) { // 7 -> 3
            button = 7;

        } else if ((ang >= 337.5 && ang < 360) || (ang >= 0 && ang < 22.5)) { // 4 -> 6
            button = 4;
        }
        return button;
    }

    // this is the user behavior function
    // for each target, given a particular view
    // what is the probability of user giving which possible user input
    public double Pr_BGI(int button, int xy_index, int xx, int yy, int view_x, int view_y, double zlevel) {
        double P_BGI = 0;
        int reg = output_region(view_x, view_y, xx, yy, zlevel);

        if (button == 1) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.9;
                    break;
                case 8:
                    P_BGI = 0.04;
                    break;
                case 6:
                    P_BGI = 0.04;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 2) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.04;
                    break;
                case 8:
                    P_BGI = 0.9;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.04;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 3) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.04;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.9;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.04;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 4) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.04;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.9;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.04;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 5) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.95;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 6) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.04;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.9;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.02/6;
                    break;
                case 1:
                    P_BGI = 0.04;
                    break;
            }
        }
        if (button == 7) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.04;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.9;
                    break;
                case 2:
                    P_BGI = 0.04;
                    break;
                case 1:
                    P_BGI = 0.02/6;
                    break;
            }
        }
        if (button == 8) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.02/6;
                    break;
                case 3:
                    P_BGI = 0.04;
                    break;
                case 2:
                    P_BGI = 0.9;
                    break;
                case 1:
                    P_BGI = 0.04;
                    break;
            }
        }
        if (button == 9) {
            switch (reg) {
                case 10:
                    P_BGI = 0;
                    break;
                case 9:
                    P_BGI = 0.02/6;
                    break;
                case 8:
                    P_BGI = 0.02/6;
                    break;
                case 6:
                    P_BGI = 0.02/6;
                    break;
                case 7:
                    P_BGI = 0.02/6;
                    break;
                case 5:
                    P_BGI = 0.05/8;
                    break;
                case 4:
                    P_BGI = 0.04;
                    break;
                case 3:
                    P_BGI = 0.02/6;
                    break;
                case 2:
                    P_BGI = 0.04;
                    break;
                case 1:
                    P_BGI = 0.9;
                    break;
            }
        }
        if (button > 9) {
            switch (reg){
                case 10:
                    if (button == xy_index + 10) {
                        P_BGI = 1;
                    }
                    break;
                case 9:
                    P_BGI = 0;
                    break;
                case 8:
                    P_BGI = 0;
                    break;
                case 6:
                    P_BGI = 0;
                    break;
                case 7:
                    P_BGI = 0;
                    break;
                case 5:
                    P_BGI = 0;
                    break;
                case 4:
                    P_BGI = 0;
                    break;
                case 3:
                    P_BGI = 0;
                    break;
                case 2:
                    P_BGI = 0;
                    break;
                case 1:
                    P_BGI = 0;
                    break;
            }
        }
        return P_BGI;
    }

    // this is the function to calculate the sum of the above function
    public double P_BI(int button, int view_x, int view_y, double zlevel){
        double P_BI = 0;
        for (int k = 0; k < n; k++){
            P_BI = P_BI + Pr_BGI(button, k, (int)updates[k][0], (int)updates[k][1], view_x,view_y,zlevel) * updates[k][2];
        }
        return P_BI;
    }

    // after receiving the user's input
    // this is the function to update the probability distribution of the objects in the virtual world
    // and now we can compute the actual information gain
    public void newupdate() {
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double zlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        double wx = 800 * zlevel;
        double wy = 600 * zlevel;

        if (Main.target_x > c.vx - 2/wx && Main.target_x < c.vx + 2/wx && Main.target_y > c.vy - 2/wy
                && Main.target_y < c.vy + 2/wy && zlevel == 1.0) Main.tar.setColor(Color.green);

        // calculate H(goal)
        double H_goal = 0;
        double ent;
        for (int i = 0; i < n; i++) {
            if (updates[i][2] != 0){
                ent = -(updates[i][2] * Math.log(updates[i][2]));
            } else ent = 0;
            H_goal = H_goal + ent;
        }

        // update the probability of the objects in virtual space
        int direc = direction();

        for (int l = 0; l < n; l++){
            double PBGI = Pr_BGI(direc, l, (int)updates[l][0], (int)updates[l][1], (int)c.vx, (int)c.vy, zlevel);

            updates[l][2] = PBGI * updates[l][2] / P_BI(direc, (int)c.vx, (int)c.vy, zlevel);
        }

        //calculate the actual information gain since we now have X=x and Y=y
        double H_goalBI = 0;
        double entr = 0;
        for (int j = 0; j < n; j++) {
            if (updates[j][2] != 0){
                entr = - updates[j][2]*Math.log(updates[j][2]);
            } else entr = 0;
            H_goalBI = H_goalBI + entr;
        }

        // this is the actual information gain 
        double IG = H_goal - H_goalBI;

    }

    // enable smooth transition between two views - animate zooming
    public void animate_zoom(Camera c, float alt){
        Animation a = Main.vsm.getAnimationManager().getAnimationFactory().createCameraAltAnim(500,
                c, alt, false, ConstantAccInterpolator.getInstance(), null);
        Main.vsm.getAnimationManager().startAnimation(a, false);

    }

    // animate panning
    public void animate_panning(Camera c, int x, int y){
        Point2D.Double loc = new Point2D.Double((double)x, (double)y);
        Animation b = Main.vsm.getAnimationManager().getAnimationFactory().createCameraTranslation(500,c,
                loc,false, ConstantAccInterpolator.getInstance(), null);
        Main.vsm.getAnimationManager().startAnimation(b, false);
    }

    // go over all possible views to locate the view that maximizes the expected information gain
    public void BestWP() {
        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double max_expIG = 0;
        int max_x = (int)c.vx;
        int max_y = (int)c.vy;
        double cam_alt = c.altitude;
        double max_cam = 0;

        for ( // go over all possible views here 
            ){
            double expIG = New_IG(vc_x, vc_y, zlevel);
            if (expIG > max_expIG) {
                // this is the maximum expected information gain
                max_expIG = expIG;
                // and the view (x,y,z)
                max_x = vc_x;
                max_y = vc_y;
                max_cam = cam_alt;
            }
        }
        // use animation to transit the view
        animate_zoom(c,(float) max_cam);
        animate_panning(c,max_x,max_y);
    }
}


/***
This is conventional pan and zoom class
*/
class ConListener extends ViewAdapter implements ViewListener {

    // if you just want to use pan and zoom
    // you can just copy & paste the codes about ZVTM part from BIG class

    // if you want to calculate the actual information gain from the user's input in standard navigation
    // you can copy & paste the codes from BIG class except the animation and the BestWP() function
}


/***
This is the camera listener class 
*/
class Foo implements CameraListener {

    public void cameraMoved(Camera cam, Point2D.Double coord, double alt) {

        Camera c = VirtualSpaceManager.INSTANCE.getActiveCamera();
        double zoomlevel = (c.focal + Math.abs(c.altitude)) / c.focal;
        double coordx = coord.getX();
        double coordy = coord.getY();
        double wx = 800 * zoomlevel;
        double wy = 600 * zoomlevel;
    }
}

