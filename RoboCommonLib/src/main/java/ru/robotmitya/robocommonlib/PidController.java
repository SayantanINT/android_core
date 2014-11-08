package ru.robotmitya.robocommonlib;

/**
 *
 * Created by dmitrydzz on 05.11.14.
 */
@SuppressWarnings("UnusedDeclaration")
public class PidController {
    private double mKp;			// factor for "proportional" control
    private double mKi;			// factor for "integral" control
    private double mKd;			// factor for "derivative" control
    private double mInput;             // sensor input for pid controller
    private double mMaximumOutput = 1.0;	// |maximum output|
    private double mMinimumOutput = -1.0;	// |minimum output|
    private double mMaximumInput = 0.0;		// maximum input - limit target to this
    private double mMinimumInput = 0.0;		// minimum input - limit target to this
    private boolean mContinuous = false;	// do the endpoints wrap around? eg. Absolute encoder
    private boolean mEnabled = false; 			//is the pid controller enabled
    private double mPrevError = 0.0;	// the prior sensor input (used to compute velocity)
    private double mTotalError = 0.0; //the sum of the errors for use in the integral calc
    private double mTolerance = 0.05;	//the percentage error that is considered on target
    private double mTarget = 0.0;
    private double mError = 0.0;
    private double mResult = 0.0;

    /**
     * Allocate a PID object with the given constants for P, I, D
     * @param kp the proportional coefficient
     * @param ki the integral coefficient
     * @param kd the derivative coefficient
     */
    public PidController(double kp, double ki, double kd) {
        mKp = kp;
        mKi = ki;
        mKd = kd;
    }

    public PidController() {
    }

    /**
     * Read the input, calculate the output accordingly, and write to the output.
     * This should only be called by the PIDTask
     * and is created during initialization.
     */
    private void calculate() {
        // If enabled then proceed into controller calculations
        if (mEnabled) {

            // Calculate the error signal
            mError = mTarget - mInput;

            // !!!!DEBUG!!!
//            System.out.println(mTarget);

            // If continuous is set to true allow wrap around
            if (mContinuous) {
                if (Math.abs(mError) >
                        (mMaximumInput - mMinimumInput) / 2) {
                    if (mError > 0) {
                        mError = mError - mMaximumInput + mMinimumInput;
                    } else {
                        mError = mError +
                                mMaximumInput - mMinimumInput;
                    }
                }
            }

            /* Integrate the errors as long as the upcoming integrator does
               not exceed the minimum and maximum output thresholds */
            if (((mTotalError + mError) * mKi < mMaximumOutput) &&
                    ((mTotalError + mError) * mKi > mMinimumOutput)) {
                mTotalError += mError;
            }

            // Perform the primary PID calculation
            mResult = (mKp * mError + mKi * mTotalError + mKd * (mError - mPrevError));

            // Set the current error to the previous error for the next cycle
            mPrevError = mError;

            // Make sure the final result is within bounds
            if (mResult > mMaximumOutput) {
                mResult = mMaximumOutput;
            } else if (mResult < mMinimumOutput) {
                mResult = mMinimumOutput;
            }
        }
    }

    /**
     * Set the PID Controller gain parameters.
     * Set the proportional, integral, and differential coefficients.
     * @param kp Proportional coefficient
     * @param ki Integral coefficient
     * @param kd Differential coefficient
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setPid(double kp, double ki, double kd) {
        mKp = kp;
        mKi = ki;
        mKd = kd;
    }

    /**
     * Get the Proportional coefficient
     * @return proportional coefficient
     */
    public double getKp() {
        return mKp;
    }

    /**
     * Get the Integral coefficient
     * @return integral coefficient
     */
    public double getKi() {
        return mKi;
    }

    /**
     * Get the Differential coefficient
     * @return differential coefficient
     */
    public double getKd() {
        return mKd;
    }

    /**
     * Set the Proportional coefficient
     * @param kp proportional coefficient
     */
    public void setKp(double kp) {
        mKp = kp;
    }

    /**
     * Set the Integral coefficient
     * @param ki integral coefficient
     */
    public void setKi(double ki) {
        mKi = ki;
    }

    /**
     * Set the Differential coefficient
     * @param kd differential coefficient
     */
    public void setKd(double kd) {
        mKd = kd;
    }

    /**
     * Change the Proportional coefficient
     * @param deltaKp proportional coefficient delta
     */
    public void addKp(double deltaKp) {
        mKp += deltaKp;
    }

    /**
     * Change the Integral coefficient
     * @param deltaKi integral coefficient delta
     */
    public void addKi(double deltaKi) {
        mKi += deltaKi;
    }

    /**
     * Change the Differential coefficient
     * @param deltaKd differential coefficient delta
     */
    public void addKd(double deltaKd) {
        mKd += deltaKd;
    }

    /**
     * Return the current PID result
     * This is always centered on zero and constrained the the max and min outs
     * @return the latest calculated output
     */
    public double performPid() {
        calculate();
        return mResult;
    }

    /**
     *  Set the PID controller to consider the input to be continuous,
     *  Rather then using the max and min in as constraints, it considers them to
     *  be the same point and automatically calculates the shortest route to
     *  the setpoint.
     * @param continuous Set to true turns on continuous, false turns off continuous
     */
    public void setContinuous(boolean continuous) {
        mContinuous = continuous;
    }

    /**
     * Sets the maximum and minimum values expected from the input.
     *
     * @param minimumInput the minimum value expected from the input
     * @param maximumInput the maximum value expected from the output
     */
    public void setInputRange(double minimumInput, double maximumInput) {
        mMinimumInput = minimumInput;
        mMaximumInput = maximumInput;
        setTarget(mTarget);
    }

    /**
     * Sets the minimum and maximum values to write.
     *
     * @param minimumOutput the minimum value to write to the output
     * @param maximumOutput the maximum value to write to the output
     */
    public void setOutputRange(double minimumOutput, double maximumOutput) {
        mMinimumOutput = minimumOutput;
        mMaximumOutput = maximumOutput;
    }

    /**
     * Set the target for the PIDController
     * @param target the desired target
     */
    public void setTarget(double target) {
        if (mMaximumInput > mMinimumInput) {
            if (target > mMaximumInput) {
                mTarget = mMaximumInput;
            } else if (target < mMinimumInput) {
                mTarget = mMinimumInput;
            } else {
                mTarget = target;
            }
        } else {
            mTarget = target;
        }
    }

    /**
     * Returns the current target of the PIDController
     * @return the current target
     */
    public double getTarget() {
        return mTarget;
    }

    /**
     * Returns the current difference of the input from the target
     * @return the current error
     */
    public synchronized double getError() {
        return mError;
    }

    /**
     * Set the percentage error which is considered tolerable for use with
     * OnTarget. (Input of 15.0 = 15 percent)
     * @param percent error which is tolerable
     */
    public void setTolerance(double percent) {
        mTolerance = percent;
    }

    /**
     * Return true if the error is within the percentage of the total input range,
     * determined by setTolerance. This assumes that the maximum and minimum input
     * were set using setInput.
     * @return true if the error is less than the tolerance
     */
    public boolean onTarget() {
        return (Math.abs(mError) < mTolerance / 100 *
                (mMaximumInput - mMinimumInput));
    }

    /**
     * Begin running the PIDController
     */
    public void enable() {
        mEnabled = true;
    }

    /**
     * Stop running the PIDController, this sets the output to zero before stopping.

     */
    public void disable() {
        mEnabled = false;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Reset the previous error,, the integral term, and disable the controller.
     */
    public void reset() {
        disable();
        mPrevError = 0;
        mTotalError = 0;
        mResult = 0;
    }

    public void setInput(double input){
        mInput = input;
    }

}