package org.n52.sos.ioos.data.subsensor;

public abstract class ProfileSubSensor extends SubSensor implements IndexedSubSensor {
    public abstract double getHeight();
    
    @Override
    public int compareTo(SubSensor o) {        
        if (o == null) {
            throw new NullPointerException();
        }

        if (o instanceof ProfileSubSensor) {
            ProfileSubSensor p = (ProfileSubSensor) o;
            return Double.compare(p.getHeight(), getHeight());
        } else {
            return super.compareTo(o);
        }
    }

}
