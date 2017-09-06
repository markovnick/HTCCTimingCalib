/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.modules;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import org.clas.view.DetectorShape2D;
import org.clas.viewer.CalibrationModule;
import org.clas.viewer.CCDetector;
import org.jlab.clas.pdg.PhysicsConstants;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.groot.base.ColorPalette;
import org.jlab.groot.data.H1F;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.fitter.DataFitter;

/**
 *
 * @author devita
 */
public class TimeCalibration extends CalibrationModule {

    public TimeCalibration(CCDetector d, String name) {
        super(d, name, "offset:offset_error:resolution");

    }

    @Override
    public void resetEventListener() {
        H1F htsum = new H1F("htsum", 300, -30.0, 30.0);
        htsum.setTitleX("Time Offset (ns)");
        htsum.setTitleY("Counts");
        htsum.setTitle("Global Time Offset");
        htsum.setFillColor(3);
        H1F htsum_calib = new H1F("htsum_calib", 300, -30.0, 30.0);
        htsum_calib.setTitleX("Time Offset (ns)");
        htsum_calib.setTitleY("counts");
        htsum_calib.setTitle("Global Time Offset");
        htsum_calib.setFillColor(44);
        GraphErrors gtoffsets = new GraphErrors("gtoff");
        gtoffsets.addPoint(0., -1., 0., 0.);
        gtoffsets.addPoint(1., 0., 0., 0.);
        gtoffsets.setTitle("Timing Offsets"); //  title
        gtoffsets.setTitleX("Crystal ID"); // X axis title
        gtoffsets.setTitleY("Timing (ns)");   // Y axis title
        gtoffsets.setMarkerColor(5); // color from 0-9 for given palette
        gtoffsets.setMarkerSize(5);  // size in points on the screen
//        gtoffsets.setMarkerStyle(1); // Style can be 1 or 2

        for (int iSect : this.getDetector().getSectors()) {
            for (int iLay = 1; iLay <= this.getLayers(); iLay++) {
                for (int iComp = 1; iComp <= this.getSegments(); iComp++) {
                    this.getCalibrationTable().addEntry(iSect, iLay, iComp);
                    getCalibrationTable().setDoubleValue(0., "offset", iSect, iLay, iComp);
                    getCalibrationTable().setDoubleValue(0., "offset_error", iSect, iLay, iComp);
                    getCalibrationTable().setDoubleValue(1., "resolution", iSect, iLay, iComp);

                    // initialize data group
                    H1F htime_wide = new H1F("htime_wide_" + iSect + "_" + iLay + "_" + iComp, 600, -50.0, 50.0);
                    htime_wide.setTitleX("Time (ns)");
                    htime_wide.setTitleY("Counts");
                    htime_wide.setTitle("Channel (" + iSect + "," + iLay + "," + iComp + ")");
                    H1F htime = new H1F("htime_" + iSect + "_" + iLay + "_" + iComp, 300, -15.0, 15.0);
                    htime.setTitleX("Time (ns)");
                    htime.setTitleY("Counts");
                    htime.setTitle("Channel (" + iSect + "," + iLay + "," + iComp + ")");
                    H1F htime_calib = new H1F("htime_calib_" + iSect + "_" + iLay + "_" + iComp, 300, -3.0, 3.0);
                    htime_calib.setTitleX("Time (ns)");
                    htime_calib.setTitleY("Counts");
                    htime_calib.setTitle("Channel (" + iSect + "," + iLay + "," + iComp + ")");
                    htime_calib.setFillColor(44);
                    htime_calib.setLineColor(24);
                    F1D ftime = new F1D("ftime_" + iSect + "_" + iLay + "_" + iComp, "[amp]*gaus(x,[mean],[sigma])", -1., 1.);
                    ftime.setParameter(0, 0.0);
                    ftime.setParameter(1, 0.0);
                    ftime.setParameter(2, 2.0);
                    ftime.setLineColor(24);
                    ftime.setLineWidth(2);
                    //            ftime.setLineColor(2);
                    //            ftime.setLineStyle(1);
                    DataGroup dg = new DataGroup(2, 2);
                    dg.addDataSet(htsum, 0);
                    dg.addDataSet(htsum_calib, 0);
                    dg.addDataSet(gtoffsets, 1);
                    dg.addDataSet(htime_wide, 2);
                    dg.addDataSet(htime, 3);
                    dg.addDataSet(htime_calib, 3);
                    dg.addDataSet(ftime, 3);
                    this.getDataGroup().add(dg, iSect, iLay, iComp);
                }
            }
        }
        getCalibrationTable().fireTableDataChanged();
    }

    @Override
    public List<CalibrationConstants> getCalibrationConstants() {
        return Arrays.asList(getCalibrationTable());
    }

    public int getNEvents(int isec, int ilay, int icomp) {
        return this.getDataGroup().getItem(isec, ilay, icomp).getH1F("htime_" + isec + "_" + ilay + "_" + icomp).getEntries();
    }

    public void processEvent(DataEvent event) {
        // loop over FTCAL reconstructed cluster
        DataBank recBankEB = null;
        DataBank recEvenEB = null;
        DataBank recDeteCC = null;
        DataBank recTBTTrack = null;
        DataBank recDeteSC = null;
        double startTime = 0;

        float tofTime = 0;
        float htccTime = 0;

        float tofPath = 0;
        int sector = 0;
        int layer = 0;
        int segment = 0;
        double deltaTime;
        if (event.hasBank("REC::Particle")) {
            recBankEB = event.getBank("REC::Particle");
        }
        if (event.hasBank("REC::Event")) {
            recEvenEB = event.getBank("REC::Event");
        }
        if (event.hasBank("REC::Cherenkov")) {
            recDeteCC = event.getBank("REC::Cherenkov");
        }

        if (event.hasBank("REC::Scintillator")) {
            recDeteSC = event.getBank("REC::Scintillator");
        }

        if (event.hasBank("TimeBasedTrkg::TBTracks")) {
            recTBTTrack = event.getBank("TimeBasedTrkg::TBTracks");
        }

        if (recBankEB != null && recEvenEB != null) {
            startTime = recEvenEB.getFloat("STTime", 0);
        }

        for (int eventLoop = 0; eventLoop < recEvenEB.rows(); eventLoop++) {
            if (recBankEB.getByte("charge", eventLoop) == -1 && recBankEB.getInt("status", eventLoop) == 1) {
                for (int scLoop = 0; scLoop < recDeteSC.rows(); scLoop++) {
                    if (recDeteSC.getInt("pindex", scLoop) == eventLoop) {
                        tofTime = recDeteSC.getFloat("time", scLoop);
                        tofPath = recDeteSC.getFloat("path", scLoop);
                        for (int htccLoop = 0; htccLoop < recDeteCC.rows(); htccLoop++) {
                            if (recDeteCC.getInt("pindex", htccLoop) == eventLoop) {
                                htccTime = recDeteCC.getFloat("time", htccLoop);
                                float theta = recDeteCC.getFloat("theta", htccLoop);
                                float phi = recDeteCC.getFloat("phi", htccLoop);
                                int nphe = recDeteCC.getInt("nphe", htccLoop);
                                int counterPhi = 0;
                                int counter = 0;
                                int counterTheta = (int) Math.toDegrees(theta) / 10;
                                counterPhi = (int) ((Math.toDegrees(phi) + 166.0) / 30);

                                if (counterPhi > 4) {
                                    counterPhi = counterPhi - 5;
                                } else {
                                    counterPhi = counterPhi + 7;
                                }
                                counter = counterTheta * 12 + counterPhi;
                                segment = counter / 12 + 1;
                                sector = (counter - (segment - 1) * 12) / 2 + 1;
                                layer = (counter % 2) + 1;
                                deltaTime = (htccTime) - (tofTime - tofPath / (float) 30.0);
                                if (nphe > 2) {
                                    this.getDataGroup().getItem(sector, layer, segment).getH1F("htsum").fill(deltaTime);
                                    this.getDataGroup().getItem(sector, layer, segment).getH1F("htime_wide_" + sector + "_" + layer + "_" + segment).fill(deltaTime);
                                    this.getDataGroup().getItem(sector, layer, segment).getH1F("htime_" + sector + "_" + layer + "_" + segment).fill(deltaTime);
                                    if (this.getPreviousCalibrationTable().hasEntry(sector, layer, segment)) {
                                        double offset = this.getPreviousCalibrationTable().getDoubleValue("offset", sector, layer, segment);
                                        this.getDataGroup().getItem(sector, layer, segment).getH1F("htsum_calib").fill(deltaTime - offset);
                                        this.getDataGroup().getItem(sector, layer, segment).getH1F("htime_calib_" + sector + "_" + layer + "_" + segment).fill(deltaTime - offset);

                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public void analyze() {
//        System.out.println("Analyzing");
        for (int iSect : this.getDetector().getSectors()) {
            for (int iLay = 1; iLay <= this.getLayers(); iLay++) {
                for (int iComp = 1; iComp <= this.getSegments(); iComp++) {
                    this.getDataGroup().getItem(iSect, iLay, iComp).getGraph("gtoff").reset();
                }
            }
        }
        for (int iSect : this.getDetector().getSectors()) {
            for (int iLay = 1; iLay <= this.getLayers(); iLay++) {
                for (int iComp = 1; iComp <= this.getSegments(); iComp++) {
                    H1F htime = this.getDataGroup().getItem(iSect, iLay, iComp).getH1F("htime_" + iSect + "_" + iLay + "_" + iComp);
                    F1D ftime = this.getDataGroup().getItem(iSect, iLay, iComp).getF1D("ftime_" + iSect + "_" + iLay + "_" + iComp);
                    this.initTimeGaussFitPar(ftime, htime);
                    DataFitter.fit(ftime, htime, "LQ");

                    int key = (iSect - 1) * this.getLayers() * this.getSegments() + (iLay - 1) * this.getSegments() + (iComp);
                    this.getDataGroup().getItem(iSect, iLay, iComp).getGraph("gtoff").addPoint(key, ftime.getParameter(1), 0, ftime.parameter(1).error());

                    getCalibrationTable().setDoubleValue(this.getDataGroup().getItem(iSect, iLay, iComp).getF1D("ftime_" + iSect + "_" + iLay + "_" + iComp).getParameter(1), "offset", iSect, iLay, iComp);
                    getCalibrationTable().setDoubleValue(this.getDataGroup().getItem(iSect, iLay, iComp).getF1D("ftime_" + iSect + "_" + iLay + "_" + iComp).parameter(1).error(), "offset_error", iSect, iLay, iComp);
                    getCalibrationTable().setDoubleValue(this.getDataGroup().getItem(iSect, iLay, iComp).getF1D("ftime_" + iSect + "_" + iLay + "_" + iComp).getParameter(2), "resolution", iSect, iLay, iComp);
                }
            }
        }
        getCalibrationTable().fireTableDataChanged();
    }

    private void initTimeGaussFitPar(F1D ftime, H1F htime) {
        double hAmp = htime.getBinContent(htime.getMaximumBin());
        double hMean = htime.getAxis().getBinCenter(htime.getMaximumBin());
        double hRMS = 2; //ns
        double rangeMin = (hMean - (0.8 * hRMS));
        double rangeMax = (hMean + (0.2 * hRMS));
        double pm = (hMean * 3.) / 100.0;
        ftime.setRange(rangeMin, rangeMax);
        ftime.setParameter(0, hAmp);
        ftime.setParLimits(0, hAmp * 0.8, hAmp * 1.2);
        ftime.setParameter(1, hMean);
        ftime.setParLimits(1, hMean - pm, hMean + (pm));
        ftime.setParameter(2, 0.05);
        ftime.setParLimits(2, 0.001 * hRMS, 0.8 * hRMS);
    }

    @Override
    public Color getColor(DetectorShape2D dsd) {
        // show summary
        int sector = dsd.getDescriptor().getSector();
        int layer = dsd.getDescriptor().getLayer();
        int key = dsd.getDescriptor().getComponent();
        ColorPalette palette = new ColorPalette();
        Color col = new Color(100, 100, 100);
        int nent = this.getNEvents(sector, layer, key);
        if (nent > 0) {
            col = palette.getColor3D(nent, this.getnProcessed(), true);
        }

//        col = new Color(100, 0, 0);
        return col;
    }

    @Override
    public void timerUpdate() {
        this.analyze();
    }
}
