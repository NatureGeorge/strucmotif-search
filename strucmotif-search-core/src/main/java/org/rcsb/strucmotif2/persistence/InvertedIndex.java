package org.rcsb.strucmotif2.persistence;

import org.rcsb.strucmotif2.domain.Pair;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif2.domain.motif.ResiduePairIdentifier;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The specification on how to insert and select residue pair occurrences. Update operate is not directly supported
 * (rather invalid/obsolete identifiers have to be removed manually and subsequently the new data can be inserted).
 */
public interface InvertedIndex {
    /**
     * Insert operation for new data.
     * @param residuePairDescriptor the bin for which new data should be written
     * @param residuePairOccurrences the data to append to this bin - keys are pdbIds, values are all words of this descriptor
     */
    void insert(ResiduePairDescriptor residuePairDescriptor, Map<StructureIdentifier, Collection<ResiduePairIdentifier>> residuePairOccurrences);

    /**
     * Perform lookup for a particular bin.
     * @param residuePairDescriptor the bin for which occurrences should the lookup be performed
     * @return a {@link Stream} of all occurrences, grouped by {@link StructureIdentifier}
     */
    Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor);

    /**
     * Removes all information on a set of structures from the index.
     * @param structureIdentifiers what to remove
     */
    void delete(Collection<StructureIdentifier> structureIdentifiers);
}
