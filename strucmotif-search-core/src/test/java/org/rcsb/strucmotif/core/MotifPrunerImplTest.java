package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class MotifPrunerImplTest {
    private StructureReader structureReader;
    private MotifPruner motifPruner;

    @BeforeEach
    public void init() {
        MotifSearchConfig config = new MotifSearchConfig();
        structureReader = new StructureReaderImpl();
        motifPruner = new KruskalMotifPruner(config);
    }

    @Test
    public void whenHds_thenPerformNoOperation() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4cha"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("B", "1", 42), // H
                new LabelSelection("B", "1", 87), // D
                new LabelSelection("C", "1", 47)); // S
        List<Map<LabelAtomId, float[]>> residues = labelSelections.stream()
                .map(structure::manifestResidue)
                .collect(Collectors.toList());

        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(structure, labelSelections, residues);

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                .anyMatch(i -> i == 10));
    }

    @Test
    public void whenEqir_thenPruneOnePairing() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1ec6"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("D", "1", 11), // E, D14
                        new LabelSelection("D", "1", 37), // Q, D40
                        new LabelSelection("D", "1", 38), // I, D41
                        new LabelSelection("D", "1", 51)); // R, D54
        List<Map<LabelAtomId, float[]>> residues = labelSelections.stream()
                .map(structure::manifestResidue)
                .collect(Collectors.toList());

        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(structure, labelSelections, residues);

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                // .peek(System.out::println)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                // maximum alpha carbon distance is 7
                .allMatch(i -> i < 8));
    }
}