package org.n52.sos.encode;

public class EncodedValuesResult {
    private String encodedValuesString;
    private int count;
    
    public EncodedValuesResult(String encodedValuesString, int count) {
        super();
        this.encodedValuesString = encodedValuesString;
        this.count = count;
    }

    public String getEncodedValuesString() {
        return encodedValuesString;
    }

    public int getCount() {
        return count;
    }
}
