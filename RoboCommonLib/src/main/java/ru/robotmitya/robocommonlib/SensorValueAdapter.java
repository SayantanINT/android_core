package ru.robotmitya.robocommonlib;

/**
 * Created by dmitrydzz on 4/17/14.
 *
 */
public final class SensorValueAdapter {
    private final double mMaxDelta;
    private final double mMinWeight;
    private Double mMean = null; // Current weighted arithmetic mean

    public SensorValueAdapter(final double maxDelta, final double minWeight) {
        mMaxDelta = maxDelta;
        mMinWeight = minWeight;
    }

    public void put(final double value) {
        if (mMean == null) {
            mMean = value;
        } else {
            double delta = Math.abs(mMean - value);
            double weight = 1 - delta / mMaxDelta;
            weight = weight < mMinWeight ? mMinWeight : weight;
            mMean = (mMean + weight * value) / (1 + weight);
        }
    }

    public Double get() {
        return mMean;
    }

    public double get(final double defaultValue) {
        Double result = get();
        if (result == null) {
            return defaultValue;
        }
        return result;
    }
}
