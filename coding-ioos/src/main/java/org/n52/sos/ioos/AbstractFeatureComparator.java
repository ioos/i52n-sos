package org.n52.sos.ioos;

import java.util.Comparator;

import org.n52.sos.ogc.gml.AbstractFeature;

public class AbstractFeatureComparator implements Comparator<AbstractFeature> {
	@Override
	public int compare(AbstractFeature o1, AbstractFeature o2) {
		if( o1 == null && o2 == null ){
			return 0;
		}
		if( o1 != null && o2 == null ){
			return -1;
		}
		if( o1 == null && o2 != null ){
			return 1;
		}
		if( o1.getIdentifier() == null && o2.getIdentifier() == null ){
			return 0;
		}
		if( o1.getIdentifier() != null && o2.getIdentifier() == null ){
			return -1;
		}
		if( o1.getIdentifier() == null && o2.getIdentifier() != null ){
			return 1;
		}
		if( o1.getIdentifier().getCodeSpace() != null && o2.getIdentifier() == null ){
			return -1;
		}
		if( o1.getIdentifier().getCodeSpace() == null && o2.getIdentifier() != null ){
			return 1;
		}
		if( o1.getIdentifier().getCodeSpace() != null && o2.getIdentifier().getCodeSpace() != null
				&& !o1.getIdentifier().getCodeSpace().equals( o2.getIdentifier().getCodeSpace() ) ){
			return o1.getIdentifier().getCodeSpace().compareTo( o2.getIdentifier().getCodeSpace() );
		}
		if( o1.getIdentifier().getValue() == null && o2.getIdentifier().getValue() == null ){
			return 0;
		}
		if( o1.getIdentifier().getValue() != null && o2.getIdentifier().getValue() == null ){
			return -1;
		}
		if( o1.getIdentifier().getValue() == null && o2.getIdentifier().getValue() != null ){
			return 1;
		}
		return o1.getIdentifier().getValue().compareTo( o2.getIdentifier().getValue() );
	}
}
