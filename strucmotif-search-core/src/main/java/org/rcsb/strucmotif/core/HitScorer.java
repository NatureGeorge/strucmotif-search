package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SimpleHit;
import org.rcsb.strucmotif.domain.result.TransformedHit;

/**
 * Quantifies how well a {@link SimpleHit} resembles the query motif.
 */
public interface HitScorer {
    /**
     * Scores this collection of residues (a.k.a. a path through the structure).
     * @param simpleHit the original hit that should be scored
     * @return a {@link Hit} instance - <code>null</code> if filtered for high RMSD
     */
    TransformedHit score(SimpleHit simpleHit);
}
