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
		if( o1.getIdentifierCodeWithAuthority() == null && o2.getIdentifierCodeWithAuthority() == null ){
			return 0;
		}
		if( o1.getIdentifierCodeWithAuthority() != null && o2.getIdentifierCodeWithAuthority() == null ){
			return -1;
		}
		if( o1.getIdentifierCodeWithAuthority() == null && o2.getIdentifierCodeWithAuthority() != null ){
			return 1;
		}
		if( o1.getIdentifierCodeWithAuthority().getCodeSpace() != null && o2.getIdentifierCodeWithAuthority() == null ){
			return -1;
		}
		if( o1.getIdentifierCodeWithAuthority().getCodeSpace() == null && o2.getIdentifierCodeWithAuthority() != null ){
			return 1;
		}
		if( o1.getIdentifierCodeWithAuthority().getCodeSpace() != null
				&& o2.getIdentifierCodeWithAuthority().getCodeSpace() != null
				&& !o1.getIdentifierCodeWithAuthority().getCodeSpace().equals(
						o2.getIdentifierCodeWithAuthority().getCodeSpace() ) ){
			return o1.getIdentifierCodeWithAuthority().getCodeSpace().compareTo(
					o2.getIdentifierCodeWithAuthority().getCodeSpace() );
		}
		if( o1.getIdentifierCodeWithAuthority().getValue() == null
				&& o2.getIdentifierCodeWithAuthority().getValue() == null ){
			return 0;
		}
		if( o1.getIdentifierCodeWithAuthority().getValue() != null
				&& o2.getIdentifierCodeWithAuthority().getValue() == null ){
			return -1;
		}
		if( o1.getIdentifierCodeWithAuthority().getValue() == null
				&& o2.getIdentifierCodeWithAuthority().getValue() != null ){
			return 1;
		}
		return o1.getIdentifierCodeWithAuthority().getValue().compareTo(
				o2.getIdentifierCodeWithAuthority().getValue() );
	}
}
