package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import ucar.nc2.constants.CF;

public class TrajectorySensorDataset extends AbstractSensorDataset implements IStaticAltitudeDataset{
    private Double alt;
    
    public TrajectorySensorDataset( SensorAsset sensor, Double alt, 
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CF.FeatureType.trajectory, sensor, dataValues);
        this.alt = alt;
    }

    public Double getAlt() {
        return alt;
    }
}