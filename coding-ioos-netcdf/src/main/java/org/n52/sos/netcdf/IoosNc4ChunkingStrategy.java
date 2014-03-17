package org.n52.sos.netcdf;

import com.axiomalaska.cf4j.CFStandardNames;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.jni.netcdf.Nc4ChunkingStrategyImpl;

public class IoosNc4ChunkingStrategy extends Nc4ChunkingStrategyImpl {
    private static final int TIME_CHUNK_MAX = 1000;

    public IoosNc4ChunkingStrategy() {
        super(5, true);
    }
    
    @Override
    public long[] computeChunking(Variable v) {
        long[] chunks = super.computeChunking(v);
        
        int numDim = v.getDimensions().size();
        for (int i = 0; i < numDim; i++) {
            Dimension dim = v.getDimension(i);
            if (dim.getFullName().equals(CFStandardNames.TIME.getName())) {
                //force time chunking to TIME_CHUNK_MAX
                chunks[i] = TIME_CHUNK_MAX;
            }
        }
        return chunks;
    }
}
