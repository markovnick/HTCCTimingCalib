/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.viewer;

/**
 *
 * @author devita
 */
//import org.clas.fcmon.tools.FCDetector;
import java.util.ArrayList;
import java.util.List;
import org.clas.view.DetectorPane2D;
import org.clas.view.DetectorShape2D;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Path3D;

public class CCDetector extends DetectorPane2D {
    
    private String                viewName = null;
    public CCPixels                  ccPix = null;
    List<Integer>                  sectors = new ArrayList();

    public CCDetector(String name) {
        this.viewName = name;
        this.ccPix = new CCPixels();   
        this.init(1,6);
    } 
    
    public void init(int is1, int is2) {
        initDetector(is1,is2);
   }
    
    public void initButtons() {
        
        System.out.println("CCDetector.initButtons()");
        
//        initMapButtons(0, 0);
//        initMapButtons(1, 0);
//        initViewButtons(0, 0); 
//        
    }   
    
    public void initDetector(int is1, int is2) {
        
//        app.currentView = "LR";
        
        for(int is=is1; is<=is2; is++) {
            sectors.add(is);
            for(int ip=0; ip<ccPix.cc_nstr[0] ; ip++) this.getView().addShape("L0",getMirror(is,1,ip));
            for(int ip=0; ip<ccPix.cc_nstr[1] ; ip++) this.getView().addShape("R0",getMirror(is,2,ip));
        }   
                
        for(String layer : this.getView().getLayerNames()){
            System.out.println(this.getView().getLayerNames());
         }
        
//        addButtons("LAY","View","LR.0.L.1.R.2");
//        addButtons("PMT","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
//        addButtons("PIX","Map","EVT.0.ADC.1.TDC.2.STATUS.3");
//        
//        app.getDetectorView().addMapButtons();
//        app.getDetectorView().addViewButtons(); 
        
    }    

    public List<Integer> getSectors() {
        return sectors;
    }

    public DetectorShape2D getMirror(int sector, int layer, int mirror) {
        
        DetectorShape2D shape = new DetectorShape2D(DetectorType.LTCC,sector,layer,mirror+1);     
        Path3D shapePath = shape.getShapePath();
        
        int off = (layer-1)*ccPix.cc_nstr[0];
        
        for(int j = 0; j < 4; j++){
            shapePath.addPoint(ccPix.cc_xpix[j][mirror+off][sector-1],ccPix.cc_ypix[j][mirror+off][sector-1],0.0);
        }
        return shape;       
    }

    
}