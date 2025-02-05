package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;

/**
 * A {@link ResiduePairIdentifier} defined by data from the inverted index. Just a wrapper for the raw data array.
 */
public class InvertedIndexResiduePairIdentifier implements ResiduePairIdentifier {
    private final Object[] data;
    private final boolean flipped;

    /**
     * Construct an identifier based on raw data.
     * @param data an Object[] from the inverted index
     * @param flipped true iff positions are flipped by the contract of {@link ResiduePairDescriptor}
     */
    public InvertedIndexResiduePairIdentifier(Object[] data, boolean flipped) {
        // length 2: { index1, index2 };
        // length 4: { index1, index2, structOperId1, structOperId2 };
        this.data = data;
        this.flipped = flipped;
    }

    @Override
    public String getStructOperId1() {
        if (data.length < 4) {
            return "1";
        }
        return (String) data[!flipped ? 2 : 3];
    }

    @Override
    public int getIndex1() {
        return (int) data[!flipped ? 0 : 1];
    }

    @Override
    public String getStructOperId2() {
        if (data.length < 4) {
            return "1";
        }
        return (String) data[!flipped ? 3 : 2];
    }

    @Override
    public int getIndex2() {
        return (int) data[!flipped ? 1 : 0];
    }

    @Override
    public IndexSelection getIndexSelection1() {
        return new IndexSelection(getStructOperId1(), getIndex1());
    }

    @Override
    public IndexSelection getIndexSelection2() {
        return new IndexSelection(getStructOperId2(), getIndex2());
    }
}
