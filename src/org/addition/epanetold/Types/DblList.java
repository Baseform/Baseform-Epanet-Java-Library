package org.addition.epanetold.Types;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Serialization Helper
 */
public class DblList extends ArrayList<Double>{
    public DblList(int initialCapacity) {
        super(initialCapacity);
    }

    public DblList() {
    }

    public DblList(Collection<? extends Double> c) {
        super(c);
    }
}
