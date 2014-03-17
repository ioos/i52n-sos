package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import com.axiomalaska.cf4j.CFFeatureTypes;

@Deprecated
public class PointSensorDataset extends AbstractSensorDataset implements IStaticLocationDataset, 
    IStaticTimeDataset, IStaticAltitudeDataset {
    private Double lng;
    private Double lat;
    private Double alt;
    private Time time;
    
    public PointSensorDataset( SensorAsset sensor, Double lng, Double lat, Double alt, Time time, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {
        super( CFFeatureTypes.POINT, sensor, dataValues);
        this.lng = lng;
        this.lat = lat;
        this.alt = alt;
        this.time = time;
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

    public Time getTime() {
        return time;
    }
}
