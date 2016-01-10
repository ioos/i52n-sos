package org.n52.sos.ioos.om;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.asset.StationAsset;
import org.n52.sos.ioos.data.dataset.AbstractSensorDataset;
import org.n52.sos.ogc.gml.time.TimePeriod;
import org.n52.sos.ogc.om.OmObservableProperty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

import ucar.nc2.constants.CF;

/**
 * An IOOS compatible observation block containing all observations for a feature type  
 */
public class IoosSosObservation {
    //for metadata block
    private CF.FeatureType featureType;
    private TimePeriod samplingTime = new TimePeriod();
    private Set<OmObservableProperty> phenomena = new HashSet<OmObservableProperty>();
    private Envelope envelope = new Envelope();   
    private HashMap<StationAsset,Point> stationPoints;
    private SetMultimap<SensorAsset,Double> sensorHeights;
    private SetMultimap<StationAsset, SensorAsset> stationSensors = HashMultimap.create();

    //for data block
    private Map<SensorAsset,? extends AbstractSensorDataset> sensorDatasetMap;

    //constructor
    public IoosSosObservation(CF.FeatureType featureType, TimePeriod samplingTime,
            Map<SensorAsset, ? extends AbstractSensorDataset> sensorDatasetMap,
            Set<OmObservableProperty> phenomena, Envelope envelope,
            HashMap<StationAsset,Point> stationPoints, SetMultimap<SensorAsset,Double> sensorHeights ) {
        super();
        this.featureType = featureType;
        this.samplingTime = samplingTime;
        this.sensorDatasetMap = sensorDatasetMap;
        this.phenomena = phenomena;
        this.envelope = envelope;
        this.stationPoints = stationPoints;
        this.sensorHeights = sensorHeights;

        //process datasets into lookup maps by station
        for (SensorAsset sensor : sensorDatasetMap.keySet()) {
            stationSensors.put(sensor.getStationAsset(), sensor);
        }
    }

    public CF.FeatureType getFeatureType() {
        return featureType;
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

    public List<SensorAsset> getSortedSensorsWithHeights(){
        List<SensorAsset> sensors = new ArrayList<SensorAsset>( sensorHeights.keySet() );
        Collections.sort( sensors );
        return sensors;
    }

    public Point getStationPoint( StationAsset station ) {
        return stationPoints.get(station);
    }

    public Double getSingularSensorHeight( SensorAsset sensor ) {
        if( sensorHeights.get( sensor ).size() != 1 ){
            return null;
        }
        return sensorHeights.get( sensor ).iterator().next();
    }

    public List<? extends AbstractSensorDataset> getSensorDatasets(){
        return Collections.unmodifiableList(Lists.newArrayList(sensorDatasetMap.values()));
    }

    public <T extends AbstractSensorDataset> AbstractSensorDataset getSensorDataset( SensorAsset sensor ){
        return sensorDatasetMap.get( sensor );
    }
}