package org.n52.sos.ioos.data.subsensor;

public class BinProfileSubSensor extends ProfileSubSensor {
    private double topHeight;
    private double bottomHeight;

    public BinProfileSubSensor(double topHeight, double bottomHeight) {
        this.topHeight = topHeight;
        this.bottomHeight = bottomHeight;
    }

    public double getTopHeight() {
        return topHeight;
    }

    public double getBottomHeight() {
        return bottomHeight;
    }

    @Override
    public double getHeight() {
        return (topHeight + bottomHeight) / 2.0;
    }

    public double getBinHeight() {
        return topHeight - bottomHeight;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(bottomHeight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(topHeight);
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
        BinProfileSubSensor other = (BinProfileSubSensor) obj;
        if (Double.doubleToLongBits(bottomHeight) != Double
                .doubleToLongBits(other.bottomHeight))
            return false;
        if (Double.doubleToLongBits(topHeight) != Double
                .doubleToLongBits(other.topHeight))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BinProfileSubSensor [topHeight=" + topHeight
                + ", bottomHeight=" + bottomHeight + "]";
    }
}