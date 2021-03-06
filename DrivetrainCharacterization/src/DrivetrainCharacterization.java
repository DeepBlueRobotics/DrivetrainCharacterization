import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class DrivetrainCharacterization {
    public static void main(String[] args) {
        String file1 = "drive_char_linear.csv";
        String file2 = "drive_char_stepwise.csv";
        System.out.println("Simple Regression: ");
        DrivetrainCharacterization.simpleRegression(file1, file2);
        System.out.println("Ordinary Least Squares: ");
        DrivetrainCharacterization.ordinaryLeastSquares(file1, file2);
    }

    // Ordinary Least Squares approach (multi-variable approach)
    public static void ordinaryLeastSquares(String file1, String file2) {
        double applied_voltage = 6.0;
        double kv = 0.0;
        double ka = 0.0;
        double voltage_intercept = 0.0;
        int spread = 30;

        ArrayList<Double> voltages = new ArrayList<Double>();
        ArrayList<Double> velocities = new ArrayList<Double>();
        ArrayList<Double> accelerations = new ArrayList<Double>();
        accelerations.add(0.0);

        ArrayList<Double> left_velocities = new ArrayList<Double>();
        ArrayList<Double> right_velocities = new ArrayList<Double>();
        try {
            Scanner filereader1 = new Scanner(new File(file1));
            while (filereader1.hasNext()) {
                String line1 = filereader1.nextLine();
                // If the line is no the first line
                if (!(line1.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    double v1 = Double.valueOf(line1.split(",")[2]);
                    double v2 = Double.valueOf(line1.split(",")[3]);
                    double v3 = 0.5 * (v1 - v2);
                    voltages.add(Double.valueOf(line1.split(",")[1])); // Append voltage
                    velocities.add(v3); // Append average of the left and right motor velocities

                    if (left_velocities.size() >= spread) {
                    	//System.out.println(left_velocities.size());
                    	double a1 = (v1 - left_velocities.get(left_velocities.size() - spread)) / (spread * 0.02);
                        double a2 = (v2 - right_velocities.get(right_velocities.size() - spread)) / (spread * 0.02);                                                                                            // right velocity
                        // System.out.println(a1 + "," + a2);
                        accelerations.add((Math.abs(a1) + Math.abs(a2)));
                    }

                    left_velocities.add(v1);
                    right_velocities.add(v2);
                }
            }
            filereader1.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }
        
        int amt_to_remove = velocities.size() - accelerations.size();
        for (int i = 0; i < amt_to_remove; i++) {
            velocities.remove(velocities.size() - 1);
            voltages.remove(voltages.size() - 1);
        }

        // Do the same thing for filereader2 as filereader1
        int previous_vel_size = left_velocities.size();
        try {
            Scanner filereader2 = new Scanner(new File(file2));
            while (filereader2.hasNext()) {
                String line2 = filereader2.nextLine();
                if (!(line2.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    double v1 = Double.valueOf(line2.split(",")[2]);
                    double v2 = Double.valueOf(line2.split(",")[3]);
                    double v3 = 0.5 * (v1 - v2);
                    velocities.add(v3);

                    if ((left_velocities.size() - previous_vel_size) >= spread) {
                    	//System.out.println(left_velocities.size());
                    	double a1 = (v1 - left_velocities.get(left_velocities.size() - spread)) / (spread * 0.02);
                        double a2 = (v2 - right_velocities.get(right_velocities.size() - spread)) / (spread * 0.02);
                        // System.out.println(a1 + "," + a2);
                        accelerations.add((Math.abs(a1) + Math.abs(a2)));
                    }

                    left_velocities.add(v1);
                    right_velocities.add(v2);
                    voltages.add(applied_voltage);
                }
            }
            filereader2.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }
        
        /*System.out.println(accelerations.size());
        System.out.println(velocities.size());
        System.out.println(voltages.size());*/
        
        amt_to_remove = velocities.size() - accelerations.size();
        for (int i = 0; i < amt_to_remove; i++) {
            velocities.remove(velocities.size() - 1);
            voltages.remove(voltages.size() - 1);
        }

        // Merge all the data into 2d-array data.
        double[][] xs = new double[voltages.size()][2];
        double[] ys = new double[voltages.size()];
        for (int i = 0; i < voltages.size(); i++) {
            xs[i][0] = velocities.get(i); 
            xs[i][1] = accelerations.get(i);
            ys[i] = voltages.get(i);
        }

        // Remove unnecessary arraylists.
        voltages = null;
        velocities = null;
        accelerations = null;
        left_velocities = null;
        right_velocities = null;
        System.gc(); // Force the garbage collector to run.

        OLSMultipleLinearRegression algorithm = new OLSMultipleLinearRegression();
        algorithm.newSampleData(ys, xs);
        double[] params = algorithm.estimateRegressionParameters();
        //System.out.println(params.length);
        kv = params[1];
        ka = params[2];
        voltage_intercept = params[0];

        System.out.print("Velocity Constant is " + Double.toString(12 * kv) /* Change inches to feet in printout */);
        System.out.print(" and the Acceleration Constant is " + Double.toString(12 * ka));
        System.out.print(" with a voltage intercept of " + Double.toString(voltage_intercept) + "\n");
    }

    // Gradient Descent approach. Do not use unless you want a slow method.
    public static void gradientDescentMethod(String file1, String file2) {
        double applied_voltage = 6.0; // The voltage expected to be applied during the Stepwise Testing.

        Random rand = new Random();
        double kv = 2 * rand.nextDouble() - 1; // The parameter for velocity in the drivetrain characterization formula
                                               // (Initially between -1.0 and 1.0)
        double ka = 2 * rand.nextDouble() - 1; // The parameter for acceleration in the drivetrain characterization
                                               // formula (initially between -1.0 and 1.0)
        double voltage_intercept = 2 * rand.nextDouble() - 1; // The parameter for the intercept voltage in the
                                                              // drivetrain characterization formula (initially between
                                                              // -1.0 and 1.0)

        ArrayList<Double> velocities = new ArrayList<Double>(); // The total list of velocities
        ArrayList<Double> accelerations = new ArrayList<Double>(); // The total list of accelerations
        ArrayList<Double> voltages = new ArrayList<Double>(); // The total list of voltages
        ArrayList<double[]> data = new ArrayList<double[]>(); // The arraylist of velocities, accelerations, and
                                                              // voltages

        // Hyperparameters
        int batch_size = 5;
        double learning_rate = 0.001; // Controls how much the gradient affects the parameters
        int training_steps = 10000; // Number of iterations to train for.
        double[] losses = new double[training_steps];
        int print_every = 100; // How often should the program print its losses?
        int update_every = 5; // How often should the program update its parameters?

        ArrayList<Double> left_velocities = new ArrayList<Double>();
        ArrayList<Double> right_velocities = new ArrayList<Double>();
        try {
            Scanner filereader1 = new Scanner(new File(file1));
            while (filereader1.hasNext()) {
                String line1 = filereader1.nextLine();
                // If the line is no the first line
                if (!(line1.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    double v1 = Double.valueOf(line1.split(",")[2]);
                    double v2 = Double.valueOf(line1.split(",")[3]);
                    double v3 = 0.5 * (v1 - v2);
                    voltages.add(Double.valueOf(line1.split(",")[1])); // Append voltage
                    velocities.add(v3); // Append average of the left and right motor velocities

                    if (left_velocities.size() >= 1) {
                        double a1 = 50 * (v1 - left_velocities.get(left_velocities.size() - 1)); // Derivative of left
                                                                                                 // velocity
                        double a2 = 50 * (v2 - right_velocities.get(right_velocities.size() - 1)); // Derivative of
                                                                                                   // right velocity
                        // System.out.println(a1 + "," + a2);
                        accelerations.add((Math.abs(a1) + Math.abs(a2)));
                    }

                    left_velocities.add(v1);
                    right_velocities.add(v2);
                }
            }
            filereader1.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }

        // Do the same thing for filereader2 as filereader1
        int previous_vel_size = left_velocities.size();
        try {
            Scanner filereader2 = new Scanner(new File(file2));
            while (filereader2.hasNext()) {
                String line2 = filereader2.nextLine();
                if (!(line2.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    double v1 = Double.valueOf(line2.split(",")[2]);
                    double v2 = Double.valueOf(line2.split(",")[3]);
                    double v3 = 0.5 * (v1 - v2);
                    velocities.add(v3);

                    if ((left_velocities.size() - previous_vel_size) >= 2) {
                        double a1 = 50 * (v1 - left_velocities.get(left_velocities.size() - 2));
                        double a2 = 50 * (v2 - right_velocities.get(right_velocities.size() - 2));
                        // System.out.println(a1 + "," + a2);
                        accelerations.add((Math.abs(a1) + Math.abs(a2)));
                    }

                    left_velocities.add(v1);
                    right_velocities.add(v2);
                    voltages.add(applied_voltage);
                }
            }
            filereader2.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }

        // Merge all the data into arraylist data.
        for (int i = 0; i < voltages.size(); i++) {
            double[] d = { velocities.get(i), accelerations.get(i), voltages.get(i) };
            data.add(d);
        }
        Collections.shuffle(data); // Shuffle the data.

        // Remove unnecessary arraylists.
        voltages = null;
        velocities = null;
        accelerations = null;
        left_velocities = null;
        right_velocities = null;
        System.gc(); // Force the garbage collector to run.

        double[] vels = new double[batch_size];
        double[] accs = new double[batch_size];
        double[] volts = new double[batch_size];
        double[] approx_volts = new double[batch_size];
        int batch_index = 0;

        for (int ts = 0; ts < training_steps; ts++) {
            for (int n = 0; n < batch_size; n++) {
                if (batch_index * batch_size + n > data.size()) { // If we cannot get the next data point...
                    batch_index = 0; // ...start over from the beginning.
                }
                vels[n] = data.get(batch_index * batch_size + n)[0];
                accs[n] = data.get(batch_index * batch_size + n)[1];
                volts[n] = data.get(batch_index * batch_size + n)[2];

                // This is the drivetrain characterization formula, Voltage = kv * velocity + ka
                // * acceleration + Voltage Intercept
                approx_volts[n] = kv * vels[n] + ka * vels[n] + voltage_intercept;
            }

            // Update parameters every update_every steps.
            if (ts % update_every == 0) {
                // Update formula is Var += -L_sub_var (gradient of loss with respect to var) *
                // learning_rate
                double[] grad = gradRMSE(vels, accs, approx_volts, volts);
                kv += -grad[0] * learning_rate;
                ka += -grad[1] * learning_rate;
                voltage_intercept += -grad[2] * learning_rate;
            }

            double loss = RMSE(approx_volts, volts);
            losses[ts] = loss;
            if (ts % print_every == 0) {
                System.out.println("Current loss is " + Double.toString(loss));
            }
            batch_index += 1;
        }

        System.out.print("Velocity Constant is " + Double.toString(12 * kv) /* Change inches to feet in printout */);
        System.out.print(" and the Acceleration Constant is " + Double.toString(12 * ka));
        System.out.print(" with a voltage intercept of " + Double.toString(voltage_intercept) + "\n");
    }

    // Simplistic approach to the problem.
    public static void simpleRegression(String file1, String file2) {
        // Argument 1 should be the filepath of the linear increase CSV file.
        // Argument 2 should be the filepath of the stepwise increase CSV file.
        double applied_voltage = 6.0; // The voltage expected to be applied during the Stepwise Testing.
        double kv = 0.0; // The parameter for velocity in the drivetrain characterization formula
        double ka = 0.0; // The parameter for acceleration in the drivetrain characterization formula
        double voltage_intercept = 0.0;
        int spread = 18; // How much data should our difference quotient cover?

        ArrayList<Double> linear_velocities = new ArrayList<Double>();
        ArrayList<Double> linear_voltages = new ArrayList<Double>();
        ArrayList<Double> stepwise_x = new ArrayList<Double>();
        ArrayList<Double> stepwise_acceleration = new ArrayList<Double>();
        double[] params = new double[2];

        try {
            Scanner filereader1 = new Scanner(new File(file1));
            while (filereader1.hasNext()) {
                String line1 = filereader1.nextLine();
                // If the line is not the first line
                if (!(line1.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    linear_voltages.add(Double.valueOf(line1.split(",")[1])); // Append voltage
                    // Append the average of the left and right velocities.
                    linear_velocities.add(0.5 * (Double.valueOf(line1.split(",")[2]) - Double.valueOf(line1.split(",")[3])));
                }
            }
            filereader1.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }

        /*
         * double[] a =
         * {1.47,1.50,1.52,1.55,1.57,1.60,1.63,1.65,1.68,1.70,1.73,1.75,1.78,1.80,1.83};
         * double[] b =
         * {52.21,53.12,54.48,55.84,57.20,58.57,59.93,61.29,63.11,64.47,66.28,68.10,69.
         * 92,72.19,74.46}; ArrayList<Double> A = new ArrayList<Double>(a.length);
         * ArrayList<Double> B = new ArrayList<Double>(a.length); for(int i = 0; i <
         * a.length; i++) { A.add(a[i]); B.add(b[i]); }
         * 
         * System.out.println(DrivetrainCharacterization.simpleRegression(A, B)[0]);
         */

        // System.out.println(linear_velocities);
        params = DrivetrainCharacterization.simpleRegressionFormula(linear_voltages, linear_velocities);
        kv = 1 / params[1]; // Voltseconds / inches
        voltage_intercept = -params[0] / params[1]; // Think of this as -(b/m) in y = mx + b

        try {
            Scanner filereader2 = new Scanner(new File(file2));
            ArrayList<Double> left_velocities = new ArrayList<Double>();
            ArrayList<Double> right_velocities = new ArrayList<Double>();
            while (filereader2.hasNext()) {
                String line2 = filereader2.nextLine();
                if (!(line2.equals(
                        "Timestamp (s),Voltage (V),LeftMotorVelocity (inches / s),RightMotorVelocity (inches / s)"))) {
                    double v1 = Double.valueOf(line2.split(",")[2]);
                    double v2 = Double.valueOf(line2.split(",")[3]);
                    double v3 = 0.5 * (v1 - v2);
                    // System.out.println(v1 + "," + v2);
                    // System.out.println(kv * v3 + voltage_intercept);
                    stepwise_x.add(applied_voltage - (kv * v3 + voltage_intercept));

                    if (left_velocities.size() >= spread) {
                        double a1 = (v1 - left_velocities.get(left_velocities.size() - spread)) / (spread * 0.02);
                        double a2 = (v2 - right_velocities.get(right_velocities.size() - spread)) / (spread * 0.02);
                        // System.out.println(a1 + "," + a2);
                        stepwise_acceleration.add((Math.abs(a1) + Math.abs(a2)));
                    }

                    left_velocities.add(v1);
                    right_velocities.add(v2);
                }
            }
            filereader2.close();

            // Remove unnecessary arraylists.
            left_velocities = null;
            right_velocities = null;
            System.gc(); // Force the garbage collector to run.

        } catch (FileNotFoundException e) {
            System.out.println("The file being referenced may not exist. Error: " + e.toString());
        }

        for (int i = 0; i < spread; i++) {
            stepwise_x.remove(stepwise_x.size() - 1);
        }

        // System.out.println(stepwise_x);
        // System.out.println(stepwise_acceleration);
        params = DrivetrainCharacterization.simpleRegressionFormula(stepwise_x, stepwise_acceleration);
        ka = 1 / params[1]; // Volt * (seconds^2) / inches.

        System.out.print("Velocity Constant is " + Double.toString(12 * kv) /* Change inches to feet in printout */);
        System.out.print(" and the Acceleration Constant is " + Double.toString(12 * ka));
        System.out.print(" with a voltage intercept of " + Double.toString(voltage_intercept) + "\n");
    };

    public static double[] simpleRegressionFormula(ArrayList<Double> xs, ArrayList<Double> ys) {
        double sx, sxx, sy, sxy, a, b;
        sx = sxx = sy = sxy = a = b = 0.0;
        int n = xs.size();

        for (int i = 0; i < n; i++) {
            sx += xs.get(i);
            sxx += xs.get(i) * xs.get(i);
            sxy += xs.get(i) * ys.get(i);
            sy += ys.get(i);
        }

        b = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        a = (sy - b * sx) / n;

        double[] params = { a, b };
        return params;
    }

    public static double RMSE(double[] ys, double[] Ys) {
        // Root Mean Square Error = sqrt( average( (ys - Ys) ^ 2 ) )
        int n = ys.length;
        double error = 0.0;
        for (int i = 0; i < n; i++) {
            error += (ys[i] - Ys[i]) * (ys[i] - Ys[i]);
        }
        return Math.sqrt(error / n);
    }

    public static double[] gradRMSE(double[] v, double[] a, double[] ys, double[] Ys) {
        // Gradient of the Root Mean Square Error function.
        double grad1, grad2, grad3;
        grad1 = grad2 = grad3 = 0.0;
        int n = Ys.length;

        for (int i = 0; i < n; i++) {
            grad1 += (Ys[i] - ys[i]) * v[i]; // Partial with respect to Kv
            grad2 += (Ys[i] - ys[i]) * a[i]; // Partial with respect to Ka
            grad3 += (Ys[i] - ys[i]); // Partial with respect to Kintercept
        }

        double loss = DrivetrainCharacterization.RMSE(ys, Ys);
        double[] gradients = { grad1 / (n * loss), grad2 / (n * loss), grad3 / (n * loss) };
        return gradients;
    }
}
