package org.n52.sos.ioos.data.subsensor;

public abstract class SubSensor implements Comparable<SubSensor>{
    @Override
    public int compareTo(SubSensor o) {
        if (o == null) {
            throw new NullPointerException();
        }

        //this should never happen (different types of subsensors shouldn't be in the same collection)
        //but fall back on class name to group like subsensors
        return this.getClass().getName().compareTo(o.getClass().getName());
    }
}
