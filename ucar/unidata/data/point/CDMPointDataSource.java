package ucar.unidata.data.point;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Formatter;
import java.util.Locale;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.geoloc.LatLonRect;
import visad.FieldImpl;
import visad.VisADException;

/**
 * The Class CDMPointDataSource.
 */
public class CDMPointDataSource extends PointDataSource {
    
    /**
     * Instantiates a new cDM point data source.
     *
     * @throws VisADException the vis ad exception
     */
    public CDMPointDataSource() throws VisADException {
        super();
    }

    /* (non-Javadoc)
     * @see ucar.unidata.data.point.PointDataSource#makeObs(ucar.unidata.data.DataChoice, ucar.unidata.data.DataSelection, ucar.unidata.geoloc.LatLonRect)
     */
    @Override
    protected FieldImpl makeObs(final DataChoice dataChoice, final DataSelection subset, final LatLonRect bbox) throws Exception {
    	final Formatter formatter = new Formatter(new StringBuffer(), Locale.US);
    	//Obviously will have to be parameterized
    	final FeatureDatasetPoint dataset = (FeatureDatasetPoint)FeatureDatasetFactoryManager.open(FeatureType.STATION_PROFILE, "/tmp/Upperair_20110526_0000.nc", null, formatter);
    	dataset.getPointFeatureCollectionList();
    	for (FeatureCollection c : dataset.getPointFeatureCollectionList()) {
			//Eventually, Logic to pull apart feature collection and put into a FieldImpl will be found here.
		}
    	
        return null;
    }
}
