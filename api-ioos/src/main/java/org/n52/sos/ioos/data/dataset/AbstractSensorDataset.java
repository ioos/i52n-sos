package org.n52.sos.ioos.data.dataset;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.n52.sos.ioos.asset.SensorAsset;
import org.n52.sos.ioos.data.subsensor.SubSensor;
import org.n52.sos.ogc.gml.time.Time;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.values.Value;

import ucar.nc2.constants.CF;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class AbstractSensorDataset implements Comparable<AbstractSensorDataset>{
    private CF.FeatureType featureType;    
    private SensorAsset sensor;

    private List<OmObservableProperty> obsProps;
    private List<Time> times;
    private List<SubSensor> subSensors;
    
    private Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>> dataValues;

    public AbstractSensorDataset( CF.FeatureType featureType, SensorAsset sensor,
            Map<Time,Map<OmObservableProperty,Map<SubSensor,Value<?>>>> dataValues){
        this.featureType = featureType;
        this.sensor = sensor;
        //make the sensorDataValues unmodifiable, since some data summaries will be made below and we don't want the data changing
        this.dataValues = Collections.unmodifiableMap(dataValues);

        //set times, phenomena, and subsensors
        Set<Time> timeSet = Sets.newHashSet();
        Set<OmObservableProperty> obsPropSet = Sets.newHashSet();
        Set<SubSensor> subSensorSet = Sets.newHashSet();
        for( Entry<Time,Map<OmObservableProperty, Map<SubSensor,Value<?>>>> dataValuesEntry : dataValues.entrySet() ){
            Time time = dataValuesEntry.getKey();
            timeSet.add(time);
            for( Map.Entry<OmObservableProperty, Map<SubSensor,Value<?>>> phenObsEntry : dataValuesEntry.getValue().entrySet() ){
                OmObservableProperty phen = phenObsEntry.getKey();                    
                Set<SubSensor> phenSubSensors = phenObsEntry.getValue().keySet();                
                obsPropSet.add(phen);                
                for (SubSensor subSensor : phenSubSensors) {
                    if (subSensor != null) {
                        subSensorSet.add(subSensor);
                    }
                }                    
            }
        }

        List<Time> timeList = Lists.newArrayList(timeSet);
        Collections.sort(timeList);
        times = Collections.unmodifiableList(timeList);

        List<OmObservableProperty> obsPropList = Lists.newArrayList(obsPropSet);
        Collections.sort(obsPropList);
        obsProps = Collections.unmodifiableList(obsPropList);

        List<SubSensor> subSensorList = Lists.newArrayList(subSensorSet);
        Collections.sort(subSensorList);
        subSensors = Collections.unmodifiableList(subSensorList);
    }


    public SensorAsset getSensor() {
        return sensor;
    }

    public CF.FeatureType getFeatureType() {
        return featureType;
    }

    public List<OmObservableProperty> getPhenomena() {
        return obsProps;
    }
    
    public List<SubSensor> getSubSensors() {
        return subSensors;
    }
    
    public List<Time> getTimes(){
        return times;
    }

    public Map<Time, Map<OmObservableProperty, Map<SubSensor, Value<?>>>> getDataValues() {
        return dataValues;
    }
    
    public static Set<AbstractSensorDataset> getAbstractAssetDatasets( Set<? extends AbstractSensorDataset> stationDatasets ){
        Set<AbstractSensorDataset> abstractStationDatasets = new HashSet<AbstractSensorDataset>();
        abstractStationDatasets.addAll( stationDatasets );
        return abstractStationDatasets;
    }
    
    @Override
    public int compareTo(AbstractSensorDataset o) {
        if( sensor == null && o.getSensor() == null ){
            return 0;            
        }
        if( sensor == null ){
            return -1;
        }
        if( o.getSensor() == null ){
            return 1;
        }
        return sensor.compareTo(o.getSensor());
    }    
}