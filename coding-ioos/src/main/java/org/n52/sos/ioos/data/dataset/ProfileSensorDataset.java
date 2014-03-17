package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import com.axiomalaska.cf4j.CFFeatureTypes;

@Deprecated
public class ProfileSensorDataset extends AbstractSensorDataset implements IStaticLocationDataset, IStaticTimeDataset{
    private Double lng;
    private Double lat;
    private Time time;
    
    public ProfileSensorDataset( SensorAsset sensor, Double lng, Double lat, Time time, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CFFeatureTypes.PROFILE, sensor, dataValues);
        this.lng = lng;
        this.lat = lat;
        this.time = time;
    }

    public Double getLng() {
        return lng;
    }

    public Double getLat() {
        return lat;
    }

    public Time getTime() {
        return time;
    }
}
