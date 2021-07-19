package org.rcsb.strucmotif2.domain;

import org.rcsb.strucmotif2.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif2.domain.structure.Residue;

import java.util.List;

/**
 * The result of an alignment.
 */
public interface AlignmentResult {
    /**
     * The original coordinates of the reference.
     * @return an ordered collection of residues
     */
    List<Residue> getOriginalReference();

    /**
     * The original coordinates of the candidate.
     * @return an ordered collection of residues
     */
    List<Residue> getOriginalCandidate();

    /**
     * The coordinates of the candidate when moved onto the reference.
     * @return an ordered collection of residues
     */
    List<Residue> getAlignedCandidate();

    /**
     * The transformation which recreates this alignment when applied to the 2nd argument.
     * @return a {@link Transformation} which can be used to recreate this alignment
     */
    Transformation getTransformation();

    /**
     * The score of this superposition.
     * @return an {@link RootMeanSquareDeviation}
     */
    RootMeanSquareDeviation getRootMeanSquareDeviation();
}
