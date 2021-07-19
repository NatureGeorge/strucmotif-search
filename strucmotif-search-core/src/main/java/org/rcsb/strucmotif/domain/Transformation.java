package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.math.Algebra;

import java.util.Arrays;

/**
 * A transformation described by a 4x4 matrix.
 */
public class Transformation {
    /**
     * Neutral/identity transformation.
     */
    public static final float[][] IDENTITY_MATRIX_4D = new float[][] {
            { 1, 0, 0, 0 },
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 0, 0, 0, 1 }
    };
    public static final Transformation IDENTITY_TRANSFORMATION = new Transformation(IDENTITY_MATRIX_4D);
    private final float[][] transformation;

    /**
     * Construct a transformation from translation and rotation.
     * @param transformation 4x4 matrix
     */
    public Transformation(float[][] transformation) {
        this.transformation = transformation;
    }

    /**
     * The actual transformation matrix.
     * @return a 4x4 transformation matrix
     */
    public float[][] getTransformationMatrix() {
        return transformation;
    }

    /**
     * The flattened transformation operation (row-major indexing).
     * @return a vector of 16 values
     */
    public float[] getFlattenedTransformation() {
        // flatten transformation into 1d array
        float[] out = new float[16];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(transformation[i], 0, out, i * 4, 4);
        }
        return out;
    }

    /**
     * Transform a vector.
     * @param out the output
     * @param v the vector to transform
     */
    public void transform(float[] out, float[] v) {
        Algebra.multiply4d(out, transformation, v);
    }

    /**
     * <code>true</code> if this is the identity matrix
     * @return a boolean
     */
    public boolean isNeutral() {
        return Arrays.deepEquals(IDENTITY_MATRIX_4D, transformation);
    }
}
