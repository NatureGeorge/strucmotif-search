package org.rcsb.strucmotif.domain.query;

/**
 * How to score hits?
 */
public enum ScoringStrategy {
    /**
     * Score hits by an alignment.
     */
    ALIGNMENT,
    /**
     * Score hits based on properties of the geometric descriptors.
     */
    DESCRIPTOR
}
