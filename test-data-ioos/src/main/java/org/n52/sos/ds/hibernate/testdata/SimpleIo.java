package org.n52.sos.ds.hibernate.testdata;

public class SimpleIo{
    private String name;
    private String definition;
    private String unit;

    public SimpleIo(String name, String definition, String unit) {
        super();
        this.name = name;
        this.definition = definition;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}