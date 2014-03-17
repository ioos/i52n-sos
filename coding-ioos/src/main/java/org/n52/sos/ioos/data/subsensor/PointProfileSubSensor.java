package org.n52.sos.ioos.data.subsensor;

public class PointProfileSubSensor extends ProfileSubSensor {
    private double height;
            
    public PointProfileSubSensor(double height) {
        this.height = height;
    }
    
    @Override
    public double getHeight() {        
        return height;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(height);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PointProfileSubSensor other = (PointProfileSubSensor) obj;
        if (Double.doubleToLongBits(height) != Double
                .doubleToLongBits(other.height))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PointProfileSubSensor [height=" + height + "]";
    }
}