package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import ucar.nc2.constants.CF;

public class TimeSeriesProfileSensorDataset extends AbstractSensorDataset implements IStaticLocationDataset {
    private Double lng;
    private Double lat;
    
    public TimeSeriesProfileSensorDataset( SensorAsset sensor, Double lng, Double lat, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CF.FeatureType.timeSeriesProfile, sensor, dataValues);
        this.lng = lng;
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public Double getLat() {
        return lat;
    }
}