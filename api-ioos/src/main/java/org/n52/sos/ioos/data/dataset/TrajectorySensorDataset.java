package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import com.axiomalaska.cf4j.CFFeatureTypes;

public class TrajectorySensorDataset extends AbstractSensorDataset implements IStaticAltitudeDataset{
    private Double alt;
    
    public TrajectorySensorDataset( SensorAsset sensor, Double alt, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CFFeatureTypes.TRAJECTORY, sensor, dataValues);
        this.alt = alt;
    }

    public Double getAlt() {
        return alt;
    }
}