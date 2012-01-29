package old;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *  This class is designed to convert Latitude and Longitude values on the GRS80
 * Elipsoid to National Grid Easting and Northings using the OSTN02 
 * transformation. The algorithms and transformation data used have been 
 * obtained from the Ordnance Survey Developer Information package that can be 
 * found at: 
 * http://www.ordnancesurvey.co.uk/oswebsite/gps/osnetfreeservices/furtherinfo/questdeveloper.html
 * 
 * @author Nathan Collins
 */
public class GPSConverter {

    private BufferedReader in;
    private final String src = "." + File.separator + "OSTN02" + File.separator + "OSTN02_OSGM02_GB.txt";
    private int fileSize = 876951;
    private int columns = 7;
    private double[][] OSTN02Data;
    // Conversion constants:
    public final int AIRY1820 = 0;
    public final int GRS80 = 1;
    public final int NATIONAL_GRID = 0;
    public final int ITM = 1;
    // Ellipsoid constants:
    private double[][] ELLIPSOID;
    // - AIRY830:
    private final double AIRY_a = 6377563.396;  //Semi-major axis a (meters)
    private final double AIRY_b = 6356256.910;  //Semi-minor axis b (meters)
    // - GRS80:
    private final double GRS80_a = 6378137.000; //Semi-major axis a (meters)
    private final double GRS80_b = 6356752.3141;//Semi-minor axis b (meters)
    // Projection constants:
    private double[][] PROJECTION;
    // - National Grid:
    private final double NG_F0 = 0.9996012717;  // Scale factor on central maridian
    private final double NG_lat = 0.8552113334/*772213*/;  // True origin - lat in radians (49"N)
    private final double NG_long = -0.03490658503/*988659*/; // True origin - long in radians (2"W)
    private final double NG_E0 = 400000;        // Easting of true origin (meters)
    private final double NG_N0 = -100000;       // Northing of true origin (meters)
    // - ITM:
    private final double ITM_F0 = 0.99982;      // Scale factor on central maridian
    private final double ITM_lat = 0.9337511498169663;  // True origin - lat in radians (53"30'N)
    private final double ITM_long = -0.13962634015954636;// True origin - long in radians (8"W)
    private final double ITM_E0 = 600000;       // Easting of true origin (meters)
    private final double ITM_N0 = 750000;       // Northing of true origin (meters)

    /**
     *  The constructor fills the arrays with their values, and is also 
     * responsible for reading the OSTN02 data file.
     */
    public GPSConverter() {
        ELLIPSOID = new double[2][2];
        ELLIPSOID[0][0] = AIRY_a;
        ELLIPSOID[0][1] = AIRY_b;
        ELLIPSOID[1][0] = GRS80_a;
        ELLIPSOID[1][1] = GRS80_b;
        PROJECTION = new double[2][5];
        PROJECTION[0][0] = NG_F0;
        PROJECTION[0][1] = NG_lat;
        PROJECTION[0][2] = NG_long;
        PROJECTION[0][3] = NG_E0;
        PROJECTION[0][4] = NG_N0;
        PROJECTION[1][0] = ITM_F0;
        PROJECTION[1][1] = ITM_lat;
        PROJECTION[1][2] = ITM_long;
        PROJECTION[1][3] = ITM_E0;
        PROJECTION[1][4] = ITM_N0;
//        OSTN02Data = readTable();
    }

    /** This method enables the user to convert from Latitude and Longitude to
     * OSGB36 grid references.
     * 
     * @param latitude - The Latitude to convert from.
     * @param longitude - The Longitude to convert from.
     * @param ellipsoid - The ellipsoid constants to use: GPSConverter.AIRY1830
     * or GPSConverter.GRS80
     * @param projection - The projection constants to use: 
     * GPSConverter.NATIONAL_GRID or GPSConverter.ITM
     * @return - A double array with two values: [0] = Easting, [1] = Northing
     * @throws OutsideTransformationBoundaryException 
     */
    public double[] convertToOSGB36(double latitude, double longitude, int ellipsoid, int projection) {
        return ETRS89toOSGB36(gratToGrid(latitude, longitude, ellipsoid, projection));

        /*  Test values and expected results:
         * Lat:     51:50:6.2584
         * Long:    -2:12:0.4806
         * 
         * E:       386306.384  [n]
         * N:       215180.687  [y]
         * 
         */
    }

    /**
     *  This method converts from ETRS89 Easting/Northing to OSGB36 Easting/
     * Northing.
     * 
     * @param input - A double array with two values: [0] = Easting, 
     * [1] = Northing to be converted.
     * @return - A double array with two values: [0] = Easting, [1] = Northing
     * @throws OutsideTransformationBoundaryException - Thrown when the input 
     * values do not reside within the OSGB36 map area.
     */
    public double[] ETRS89toOSGB36(double[] input) {
        double[] results = new double[2];
        int eastIndex = (int) input[0] / 1000;
        int northIndex = (int) input[1] / 1000;
        int recordNumber = eastIndex + (northIndex * 701) + 1;
//            System.out.println("Record Number: " + recordNumber);

        // Compute shifts:
        double se0, se1, se2, se3, sn0, sn1, sn2, sn3;
        try {
            se0 = getEastShift(eastIndex, northIndex);
            se1 = getEastShift(eastIndex + 1, northIndex);
            se2 = getEastShift(eastIndex + 1, northIndex + 1);
            se3 = getEastShift(eastIndex, northIndex + 1);

            sn0 = getNorthShift(eastIndex, northIndex);
            sn1 = getNorthShift(eastIndex + 1, northIndex);
            sn2 = getNorthShift(eastIndex + 1, northIndex + 1);
            sn3 = getNorthShift(eastIndex, northIndex + 1);
        } catch (OutsideTransformationBoundaryException ex) {
            System.err.println("OutsideTransformationBoundaryException!");
            return new double[]{0, 0};
        }

        // Compute offsets:
        double dx = input[0] - OSTN02Data[recordNumber][1];
        double dy = input[1] - OSTN02Data[recordNumber][2];

        double t = dx / 1000;
        double u = dy / 1000;

        double se = (1 - t) * (1 - u) * se0 + t * (1 - u) * se1
                + t * u * se2 + (1 - t) * u * se3;
        double sn = (1 - t) * (1 - u) * sn0 + t * (1 - u) * sn1
                + t * u * sn2 + (1 - t) * u * sn3;

        results[0] = input[0] + se;
        results[1] = input[1] + sn;
        return results;

        /*  Tested with values from the OSTN02 Test Data supplied with documentation.
         * This method functions correctly at 3 decimal places.
         * 
         */
    }

    /** A private method to return the east shift value for a given pair of 
     * indexes.
     * 
     * @param x - The east index.
     * @param y - The north index.
     * @return - The value to shift the easting value by.
     * @throws OutsideTransformationBoundaryException - Thrown when the input 
     * values do not reside within the OSGB36 map area.
     */
    private double getEastShift(int x, int y) throws OutsideTransformationBoundaryException {
        int recordNumber = x + (y * 701) + 1;
//        if (OSTN02Data[recordNumber][3] == 0) {
//            throw new OutsideTransformationBoundaryException();
//        } else {
        return OSTN02Data[recordNumber][3];
//        }
    }

    /** A private method to return the north shift value for a given pair of 
     * indexes.
     * 
     * @param x - The east index.
     * @param y - The north index.
     * @return - The value to shift the northing value by.
     * @throws OutsideTransformationBoundaryException - Thrown when the input 
     * values do not reside within the OSGB36 map area.
     */
    private double getNorthShift(int x, int y) throws OutsideTransformationBoundaryException {
        int recordNumber = x + (y * 701) + 1;
//        if (OSTN02Data[recordNumber][4] == 0) {
//            throw new OutsideTransformationBoundaryException();
//        } else {
        return OSTN02Data[recordNumber][4];
//        }
    }

    /**
     *  This method converts latitude and longitude to easting and northing 
     * using the either the Airy830 or GRS80 ellipsoid and either the National 
     * Grid or ITM projection.
     * 
     * @param latitude
     * @param longitude
     * @param ellipsoid - Use either GPSConverter.ARIY1920 or GPSConverter.GRS80
     * @param projection - Use either GPSConverter.NATIONAL_GRID or GPSConverter.ITM
     * @return The easting and nothing values results[0] = easting, results[1] = northing
     */
    public double[] gratToGrid(double latitude, double longitude, int ellipsoid,
            int projection) {
        int a = ellipsoid;
        int b = projection;
        // Convert to radians:
        latitude = Math.toRadians(latitude);
        longitude = Math.toRadians(longitude);

        // B1
        double e2 = Math.pow(ELLIPSOID[a][0], 2) - Math.pow(ELLIPSOID[a][1], 2);
        e2 = e2 / Math.pow(ELLIPSOID[a][0], 2);

        // B2
        double n = ELLIPSOID[a][0] - ELLIPSOID[a][1];
        double ni = ELLIPSOID[a][0] + ELLIPSOID[a][1];
        n = n / ni;

        // B3
        double vi = 1 - e2 * Math.pow(Math.sin(latitude), 2);
        double v = ELLIPSOID[a][0] * PROJECTION[b][0] * Math.pow(vi, -0.5);
        System.out.println("v = " + v);

        // B4
        double p = ELLIPSOID[a][0] * PROJECTION[b][0] * (1 - e2) * Math.pow(vi, -1.5);
        System.out.println("p = " + p);

        // B5
        double eta2 = (v / p) - 1;
        System.out.println("eta2 = " + eta2);

        // B6
        double x1 = 1 + n + (5.0 / 4.0) * Math.pow(n, 2) + (5.0 / 4.0) * Math.pow(n, 3);
        x1 = x1 * (latitude - PROJECTION[b][1]);
        double x2 = 3 * n + 3 * Math.pow(n, 2) + (21.0 / 8.0) * Math.pow(n, 3);
        x2 = x2 * Math.sin(latitude - PROJECTION[b][1]) * Math.cos(latitude + PROJECTION[b][1]);
        double x3 = (15.0 / 8.0) * Math.pow(n, 2) + (15.0 / 8.0) * Math.pow(n, 3);
        x3 = x3 * Math.sin(2 * (latitude - PROJECTION[b][1]))
                * Math.cos(2 * (latitude + PROJECTION[b][1]));
        double x4 = (35.0 / 24.0) * Math.pow(n, 3);
        x4 = x4 * Math.sin(3 * (latitude - PROJECTION[b][1]))
                * Math.cos(3 * (latitude + PROJECTION[b][1]));

        double M = ELLIPSOID[a][1] * PROJECTION[b][0] * (x1 - x2 + x3 - x4);
        System.out.println("M = " + M);

        double I = M + PROJECTION[b][4];
        System.out.println("I = " + I);

        double II = (v / 2) * Math.sin(latitude) * Math.cos(latitude);
        System.out.println("II = " + II);

        double III = 5 - Math.pow(Math.tan(latitude), 2) + 9 * eta2;
        III = (v / 24) * Math.sin(latitude) * Math.pow(Math.cos(latitude), 3)
                * III;
        System.out.println("III = " + III);

        double IIIA = 61 - 58 * Math.pow(Math.tan(latitude), 2)
                + Math.pow(Math.tan(latitude), 4);
        IIIA = (v / 720) * Math.sin(latitude) * Math.pow(Math.cos(latitude), 5)
                * IIIA;
        System.out.println("IIIA = " + IIIA);

        double IV = v * Math.cos(latitude);
        System.out.println("IV = " + IV);

        double V = (v / 6) * Math.pow(Math.cos(latitude), 3) * ((v / p)
                - Math.pow(Math.tan(latitude), 2));
        System.out.println("V = " + V);

        double VI = 5 - 18 * Math.pow(Math.tan(latitude), 2)
                + Math.pow(Math.tan(latitude), 4) + 14 * eta2
                - 58 * Math.pow(Math.tan(latitude), 2) * eta2;
        VI = (v / 120) * Math.pow(Math.cos(latitude), 5) * VI;
        System.out.println("VI = " + VI);

        // B7
        double[] results = new double[2];

        results[1] = I + II * Math.pow((longitude - PROJECTION[b][2]), 2)
                + III * Math.pow((longitude - PROJECTION[b][2]), 4)
                + IIIA * Math.pow((longitude - PROJECTION[b][2]), 6);


        // B8
        results[0] = PROJECTION[b][3] + IV * (longitude - PROJECTION[b][2])
                + V * Math.pow((longitude - PROJECTION[b][2]), 3)
                + VI * Math.pow((longitude - PROJECTION[b][2]), 5);

        System.out.println("\nE = " + results[0]);
        System.out.println("N = " + results[1]);
        return results;

        /*  Testing values and results:
         * AIRY1830 ellipsoid and National Grid projection
         * 
         * Lat: 52° 39' 27.2531" N
         * Long: 1° 43' 4.5177" E
         * 
         * v    6.3885023333e+06    [y]
         * p    6.3727564399e+06    [y]
         * eta2 2.4708136169e–03    [y]
         * M    4.0668829596e+05    [6 s.f.]
         * I    3.0668829596e+05    [6 s.f.]
         * II   1.5404079092e+06    [y]
         * III  1.5606875424e+05    [y]
         * IIIA –2.0671123011e+04   [y]
         * IV   3.8751205749e+06    [y]
         * V    –1.7000078208e+05   [y]
         * VI   –1.0134470432e+05   [y]
         * 
         * E    651 409.903 m       [y]
         * N    313 177.270 m       [5 s.f.]
         * 
         */
        /* TODO: find out why this is not accurate to more than 1m */
    }

    /**
     *  A simple method to convert Longitude/Latitude to a decimal format.
     * @param value Must be of the format DD:MM:SS.SSSS
     * @return the decimal format as a double
     */
    public double stringToDecimal(String value) {
        String[] s = value.split(":");
        return Double.parseDouble(s[0]) + Double.parseDouble(s[1])
                / 60 + Double.parseDouble(s[2]) / 3600;
    }

    /**
     *  This utility method is responsible for reading the OSTN02 transformation
     * data and loading it to system memory.
     * 
     * @return - The 2D double array full of transformation values. Returns null
     * if the file is unable to be read or found.
     */
    private double[][] readTable() {
        /* TODO: Break this table down into smaller chunks for Android */
        System.out.println("Reading table...");
        double[][] results = new double[fileSize][columns];
        try {
            in = new BufferedReader(new FileReader(src));
            int i = 0;
            String temp = in.readLine();
            while (temp != null && i < results.length) {
                String[] s = temp.split(",");
                for (int j = 0; j < columns; j++) {
                    results[i][j] = Double.parseDouble(s[j]);
                }
                temp = in.readLine();
                i++;
            }
        } catch (FileNotFoundException ex) {
            System.err.println("OSTN02 data file not found!");
            results = null;
        } catch (IOException ex) {
            System.err.println("Unable to read file!");
            results = null;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                System.err.println("Unable to close the OSTN02 file reader!");
            }
        }
        System.out.print(" Complete!");
        return results;
    }
}
