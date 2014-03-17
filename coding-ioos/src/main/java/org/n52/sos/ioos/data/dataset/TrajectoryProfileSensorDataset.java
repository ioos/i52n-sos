package org.n52.sos.ioos.data.dataset;

import java.util.Map;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import com.axiomalaska.cf4j.CFFeatureTypes;

public class TrajectoryProfileSensorDataset extends AbstractSensorDataset {
    public TrajectoryProfileSensorDataset( SensorAsset sensor,  
            Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> dataValues) {        
        super( CFFeatureTypes.TRAJECTORY_PROFILE, sensor, dataValues);
    }
}