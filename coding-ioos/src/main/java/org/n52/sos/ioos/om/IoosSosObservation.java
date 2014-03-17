package org.n52.sos.ioos.om;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.OmObservableProperty;

import com.axiomalaska.cf4j.CFFeatureType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * An IOOS compatible observation block containing all observations for a feature type  
 */
public class IoosSosObservation {
    //for metadata block
    private CFFeatureType featureType;
    private String description;
    private TimePeriod samplingTime = new TimePeriod();
    private Set<OmObservableProperty> phenomena = new HashSet<OmObservableProperty>();
    private Envelope envelope = new Envelope();   
    private SetMultimap<StationAsset,Point> stationPoints;
    private SetMultimap<StationAsset, SensorAsset> stationSensors = HashMultimap.create();

    //for data block
    private Map<SensorAsset,? extends AbstractSensorDataset> sensorDatasetMap;
    
    //constructor
    public IoosSosObservation(CFFeatureType featureType, String description,
            TimePeriod samplingTime, Map<SensorAsset, ? extends AbstractSensorDataset> sensorDatasetMap, Set<OmObservableProperty> phenomena,
            Envelope envelope, SetMultimap<StationAsset,Point> stationPoints ) {
        super();
        this.featureType = featureType;
        this.description = description;
        this.samplingTime = samplingTime;
        this.sensorDatasetMap = sensorDatasetMap;
        this.phenomena = phenomena;
        this.envelope = envelope;
        this.stationPoints = stationPoints;

        //process datasets into lookup maps by station
        for (SensorAsset sensor : sensorDatasetMap.keySet()) {
            stationSensors.put(sensor.getStationAsset(), sensor);            
        }
    }

    public CFFeatureType getFeatureType() {
        return featureType;
    }
        
    public String getDescription() {
        return description;
    }
        
    public TimePeriod getSamplingTime() {
        return samplingTime;
    }
        
    public Map<SensorAsset, ? extends AbstractSensorDataset> getSensorDatasetMap() {
        return sensorDatasetMap;
    }

    public Set<OmObservableProperty> getPhenomena() {
        return phenomena;
    }
        
    public Envelope getEnvelope() {
        return envelope;
    }

    public List<StationAsset> getStations(){
        List<StationAsset> stations = new ArrayList<StationAsset>( stationSensors.keySet() );
        Collections.sort( stations );
        return stations;
    }    

    public List<SensorAsset> getSensors(StationAsset station){
        List<SensorAsset> sensors = new ArrayList<SensorAsset>( stationSensors.get(station) );
        Collections.sort( sensors);
        return sensors;
    }    
    
    public List<StationAsset> getSortedStationsWithPoints(){
    	List<StationAsset> stations = new ArrayList<StationAsset>( stationPoints.keySet() );
    	Collections.sort( stations );
    	return stations;
    }
    
    public Point getSingularStationPoint( StationAsset station ) {
    	if( stationPoints.get( station ).size() != 1 ){
    		return null;
    	}
		return stationPoints.get( station ).iterator().next();
	}
    
    public Collection<? extends AbstractSensorDataset> getSensorDatasets(){
        return sensorDatasetMap.values();
    }
    
    public <T extends AbstractSensorDataset> AbstractSensorDataset getSensorDataset( SensorAsset sensor ){
        return sensorDatasetMap.get( sensor );
    }    
}