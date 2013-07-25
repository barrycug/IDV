/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.control;


import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.imagery.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.chooser.adde.AddeImageChooser;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.data.AreaImageFlatField;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.data.mcidas.AREACoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


import javax.swing.*;


/**
 * Class for controlling the display of images.  Designed for brightness
 * images with range of 0 to 255.
 *
 * @author IDV Development Group
 */
public class ImagePlanViewControl extends PlanViewControl {

    /**
     * _more_
     */
    Gridded2DSet last2DSet = null;

    //  NB: For now, we don't subclass ColorPlanViewControl because we get
    //  the DataRange widget from getControlWidgets.  Might want this in
    //  the future.  It would be simpler if we wanted to include that.

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ImagePlanViewControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SKIPFACTOR | FLAG_TEXTUREQUALITY);

    }

    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     * @return Contour2DDisplayable for this instance.
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable gridDisplay =
            new Grid2DDisplayable("ImagePlanViewControl_"
                                  + ((datachoice != null)
                                     ? datachoice.toString()
                                     : ""), true);
        gridDisplay.setTextureEnable(true);
        gridDisplay.setCurvedSize(getTextureQuality());
        /* TODO: Find out why this causes redisplays
        if (BaseImageControl.EMPTY_IMAGE != null) {
            gridDisplay.loadData(BaseImageControl.EMPTY_IMAGE);
        }
        */
        //gridDisplay.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(gridDisplay);
        return gridDisplay;
    }

    /**
     *  Use the value of the texture quality to set the value on the display
     *
     * @throws RemoteException  problem with Java RMI
     * @throws VisADException   problem setting attribute on Displayable
     */
    protected void applyTextureQuality()
            throws VisADException, RemoteException {
        if (getGridDisplay() != null) {
            getGridDisplay().setCurvedSize(getTextureQuality());
        }
    }

    /**
     * Called to initialize this control from the given dataChoice;
     * sets levels controls to match data; make data slice at first level;
     * set display's color table and display units.
     *
     * @param dataChoice  choice that describes the data to be loaded.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        List dsList = new ArrayList();
        dataChoice.getDataSources(dsList);
        DataSourceImpl dsImpl = (DataSourceImpl) dsList.get(0);
        if (dsImpl instanceof AddeImageDataSource) {
            AddeImageDataSource aImageDS = (AddeImageDataSource) dsImpl;
            AddeImageDataSource.ImagePreviewSelection regionSelection =
                aImageDS.previewSelection;
            AddeImageSelectionPanel advanceSelection =
                aImageDS.advancedSelection;

            ProjectionRect rect =
                regionSelection.display.getNavigatedPanel()
                    .getSelectedRegion();

            boolean isProgressive =
                advanceSelection.getIsProgressiveResolution();
            String regionOption =  regionSelection.getRegionOption();

            if (rect != null) {
                ProjectionImpl projectionImpl =
                    regionSelection.display.getProjectionImpl();
                LatLonRect latLonRect =
                    projectionImpl.getLatLonBoundingBox(rect);
                GeoLocationInfo gInfo = new GeoLocationInfo(latLonRect);
                GeoSelection    gs    = new GeoSelection(gInfo);
                NavigatedDisplay navDisplay =
                    (NavigatedDisplay) getViewManager().getMaster();
                Rectangle screenBoundRect = navDisplay.getScreenBounds();
                gs.setScreenBound(screenBoundRect);
                gs.setScreenLatLonRect(navDisplay.getLatLonRect());
                if ( !isProgressive) {
                    gs.setXStride(advanceSelection.getElementMag());
                    gs.setYStride(advanceSelection.getLineMag());
                }
                dataSelection.setGeoSelection(gs);

            } else {
                GeoSelection gs = new GeoSelection();
                NavigatedDisplay navDisplay =
                    (NavigatedDisplay) getViewManager().getMaster();
                Rectangle screenBoundRect = navDisplay.getScreenBounds();
                gs.setScreenBound(screenBoundRect);
                gs.setScreenLatLonRect(navDisplay.getLatLonRect());
                if ( !isProgressive) {
                    gs.setXStride(advanceSelection.getElementMag());
                    gs.setYStride(advanceSelection.getLineMag());
                }
                dataSelection.setGeoSelection(gs);
            }
            dataSelection.putProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION, isProgressive);
            dataSelection.putProperty(
                    DataSelection.PROP_REGIONOPTION, regionOption);
        }

        boolean result = super.setData(dataChoice);
        if ( !result) {
            userMessage("Selected image(s) not available");
        }
        return result;
    }

    /**
     * Get the initial color table for the data
     *
     * @return  intitial color table
     */
    protected ColorTable getInitialColorTable() {
        ColorTable colorTable = super.getInitialColorTable();
        if (colorTable.getName().equalsIgnoreCase("default")) {
            colorTable = getDisplayConventions().getParamColorTable("image");
        }
        return colorTable;
    }

    /**
     * Return the color display used by this object.  A wrapper
     * around {@link #getPlanDisplay()}.
     * @return this instance's Grid2Ddisplayable.
     * @see #createPlanDisplay()
     */
    Grid2DDisplayable getGridDisplay() {
        return (Grid2DDisplayable) getPlanDisplay();
    }


    /**
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return false;
    }


    /**
     * Get the initial range for the data and color table.
     * Optimized for brightness images with range of 0 to 255.
     *
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {

        // WARNING:  Twisty-turny logic below
        // try for the parameter.
        Range range = getDisplayConventions().getParamRange(paramName,
                          getDisplayUnit());

        // see if one is defined for the color table.
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        // look for the default for "image" - hopefully it never changes
        boolean usingImage = false;
        Range imageRange = getDisplayConventions().getParamRange("image",
                               getDisplayUnit());
        /*
        if (range == null) {
            range = imageRange;
        }
        */
        if ((range != null) && Misc.equals(range, imageRange)) {
            usingImage = true;
        }

        // check to see if the range of the data is outside the range
        // of the default. This will be wrong if someone redefined what image
        // is supposed to be (0-255).
        if ((range != null) && usingImage
                && (getGridDataInstance() != null)) {
            Range dataRange = getDataRangeInColorUnits();
            if (dataRange != null) {
                if ((range.getMin() > dataRange.getMin())
                        || (range.getMax() < dataRange.getMax())) {
                    range = dataRange;
                }
            }
        }
        if (range == null) {
            range = super.getInitialRange();
        }
        return range;
    }

    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        checkImageSize(slice);
        return super.getSliceForDisplay(slice);
    }

    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Pixel Sampling";
    }

    /**
     * What label to use for the data projection
     *
     * @return label
     */
    protected String getDataProjectionLabel() {
        return "Use Native Image Projection";
    }

    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return true;
    }



    /**
     * _more_
     */
    public void viewpointChanged() {
        try {
            // check if this is rubber band event, if not do nothing
            GeoSelection geoSelection = dataSelection.getGeoSelection(true);
            geoSelection.setScreenLatLonRect(
                    getNavigatedDisplay().getLatLonRect());
            if (isRubberBandBoxChanged()) {
                reloadDataSource();
                setProjectionInView(true);
            }
        } catch (Exception e) {}
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isRubberBandBoxChanged() {

        MapProjectionDisplay mpd =
            (MapProjectionDisplay) getNavigatedDisplay();
        RubberBandBox rubberBandBox = mpd.getRubberBandBox();
        float[]       boundHi       = rubberBandBox.getBounds().getHi();

        if ((boundHi[0] == 0) && (boundHi[1] == 0)) {
            return false;
        }
        // get the displayCS here:

        Gridded2DSet new2DSet = rubberBandBox.getBounds();
        if ((rubberBandBox != null) && !new2DSet.equals(last2DSet)) {
            last2DSet = new2DSet;
            GeoSelection geoSelection = dataSelection.getGeoSelection(true);
            try {
                LatLonPoint[] llp0 =
                    getLatLonPoints(rubberBandBox.getBounds().getDoubles());
                geoSelection.setRubberBandBoxPoints(llp0);
                geoSelection.setScreenBound(
                    getNavigatedDisplay().getScreenBounds());
            } catch (Exception e) {}
            dataSelection.setGeoSelection(geoSelection);
            List          dataSources = getDataSources();
            DataSelection ds          = null;
            for (int i = 0; i < dataSources.size(); i++) {
                DataSourceImpl d = (DataSourceImpl) dataSources.get(i);
                ds = d.getDataSelection();
                ds.setGeoSelection(geoSelection);
            }

            return true;
        }
        return false;
    }

    /**
     * _more_
     *
     * @param xyPoints _more_
     *
     * @return _more_
     */
    public LatLonPoint[] getLatLonPoints(double[][] xyPoints) {
        LatLonPoint[]    latlonPoints = new LatLonPoint[xyPoints[0].length];
        NavigatedDisplay navDisplay   = getMapDisplay();
        for (int i = 0; i < xyPoints.length; i++) {
            EarthLocation llpoint =
                navDisplay.getEarthLocation(xyPoints[0][i], xyPoints[1][i],
                                            0);
            latlonPoints[i] =
                new LatLonPointImpl(llpoint.getLatitude().getValue(),
                                    llpoint.getLongitude().getValue());

        }

        return latlonPoints;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }

    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }


}
