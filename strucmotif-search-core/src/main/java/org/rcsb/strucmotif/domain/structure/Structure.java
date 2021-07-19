package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.Transformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Structure {
    private final Map<LabelSelection, Integer> residueMapping;
    private final int[] residueOffsets;
    private final int residueCount;
    private final int atomCount;
    private final String[] labelAtomId;
    private final float[] x;
    private final float[] y;
    private final float[] z;
    private final ResidueType[] residueTypes;
    private final Map<String, List<String>> assemblies;
    private final Map<String, Transformation> transformations;

    public Structure(Map<LabelSelection, Integer> residueMapping,
                     int[] residueOffsets,
                     ResidueType[] residueTypes,
                     String[] labelAtomId,
                     float[] x,
                     float[] y,
                     float[] z,
                     Map<String, List<String>> assemblies,
                     Map<String, Transformation> transformations) {
        this.residueMapping = residueMapping;
        this.residueOffsets = residueOffsets;
        this.residueTypes = residueTypes;
        this.residueCount = residueMapping.size();
        this.atomCount = labelAtomId.length;
        this.labelAtomId = labelAtomId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.assemblies = assemblies;
        this.transformations = transformations;
    }

    public List<LabelSelection> getResidueIdentifiers() {
        return new ArrayList<>(residueMapping.keySet());
    }

    public int getResidueIndex(String labelAsymId, int labelSeqId) {
        return residueMapping.get(new LabelSelection(labelAsymId, "1", labelSeqId));
    }

    public int getResidueCount() {
        return residueCount;
    }

    public int getAtomCount() {
        return atomCount;
    }

    public ResidueType getResidueType(int residueIndex) {
        return residueTypes[residueIndex];
    }

    public Map<String, List<String>> getAssemblies() {
        return assemblies;
    }

    public Map<String, Transformation> getTransformations() {
        return transformations;
    }

    public Transformation getTransformation(String structOperIdentifier) {
        return transformations.get(structOperIdentifier);
    }

    public Map<String, float[]> manifestResidue(String labelAsymId, int labelSeqId) {
        return manifestResidue(getResidueIndex(labelAsymId, labelSeqId));
    }

    public Map<String, float[]> manifestResidue(String labelAsymId, String structOperIdentifier, int labelSeqId) {
        return manifestResidue(getResidueIndex(labelAsymId, labelSeqId), structOperIdentifier);
    }

    public Map<String, float[]> manifestResidue(LabelSelection labelSelection) {
        return manifestResidue(labelSelection.getLabelAsymId(), labelSelection.getStructOperId(), labelSelection.getLabelSeqId());
    }

    public Map<String, float[]> manifestResidue(int residueIndex) {
        return manifestResidue(residueIndex, "1");
    }

    public Map<String, float[]> manifestResidue(int residueIndex, String structOperIdentifier) {
        Map<String, float[]> out = new LinkedHashMap<>();
        int offsetStart = residueOffsets[residueIndex];
        int offsetEnd = residueIndex + 1 == residueOffsets.length ? labelAtomId.length : residueOffsets[residueIndex + 1];
        Transformation transformation = transformations.get(structOperIdentifier);

        for (int i = offsetStart; i < offsetEnd; i++) {
            String labelAtomId = this.labelAtomId[i];
            float[] v = new float[]{
                    x[i],
                    y[i],
                    z[i]
            };

            // TODO need to check for identity oper here?
            if (!"1".equals(structOperIdentifier)) {
                transformation.transform(v, v);
            }

            out.put(labelAtomId, v);
        }
        return out;
    }
}
