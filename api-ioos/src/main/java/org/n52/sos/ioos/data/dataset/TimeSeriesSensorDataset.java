package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import ucar.nc2.constants.CF;

public class TimeSeriesSensorDataset extends AbstractSensorDataset implements IStaticLocationDataset, IStaticAltitudeDataset{
    private Double lng;
    private Double lat;
    private Double alt;
    
    public TimeSeriesSensorDataset( SensorAsset sensor, Double lng, Double lat, Double alt, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CF.FeatureType.timeSeries, sensor, dataValues);
        this.lng = lng;
        this.lat = lat;
        this.alt = alt;
    }

    public Double getLng() {
        return lng;
    }

    public Double getLat() {
        return lat;
    }

    public Double getAlt() {
        return alt;
    }
}