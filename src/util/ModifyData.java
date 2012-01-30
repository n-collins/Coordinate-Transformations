package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModifyData {

    private static final String src = File.separator + "home" + File.separator + "id3" + File.separator + "dev" 
    + File.separator + "workspace" + File.separator + "Coordinate Transformations" + File.separatorChar 
    + "data" + File.separator;
    
    private static String dataFileName = "OSTN02_OSGM02_GB.txt";
    
    private static BufferedReader in;
    private static BufferedWriter out;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readData();

	}
	
	private static void readData() {
		System.out.println("Reading at: " + src);
		System.out.println("Reading table...");
		int maxE = 0;
		int maxN = 0;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minH = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double maxH = Double.MIN_VALUE;
        try {
            in = new BufferedReader(new FileReader(src + dataFileName));
            out = new BufferedWriter(new FileWriter(src + "out.txt"));
            int i = 0;
            String temp = in.readLine();
            while (temp != null) {
                String[] s = temp.split(",");
                if (!(s[6].contentEquals(new StringBuffer("0")))) {
                	i++;
                	int E = Integer.parseInt(s[1]);
                	int N = Integer.parseInt(s[1]);
                	double x = Double.parseDouble(s[3]);
                	double y = Double.parseDouble(s[4]);
                	double h = Double.parseDouble(s[5]);
                	if (E > maxE) { maxE = E; }
                	if (N > maxN) { maxN = N; }
                	if (x < minX) { minX = x; }
                	if (y < minY) { minY = y; }
                	if (h < minH) { minH = h; }
                	if (x < maxX) { maxX = x; }
                	if (y < maxY) { maxY = y; }
                	if (h < maxH) { maxH = h; }
                	out.write(temp);
                	out.newLine();
                }

                temp = in.readLine();
            }
            String r1 = "maxE = " + maxE + " maxN = " + maxN;
            String r2 = "minX = " + minX + " maxX = " + maxX 
            		+ " minY = " + minY + " maxY = " + maxY + " minH = " + minH + " maxH = " + maxH 
            		+" count = " + i;
            System.out.println(r1);
            System.out.println(r2);
            out.write(r1);
            out.newLine();
            out.write(r2);
        } catch (FileNotFoundException ex) {
            System.err.println("OSTN02 data file not found!");
        } catch (IOException ex) {
            System.err.println("Unable to read file!");
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ex) {
                System.err.println("Unable to close the OSTN02 file reader!");
            }
        }
        System.out.print(" Complete!");
	}

}
