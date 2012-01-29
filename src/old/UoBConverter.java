package old;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * This class is designed to be used in conjunction with a .csv file generated
 * by the UoB Mapper Android app. This app will read the Latitude and Longitude
 * values from the original file and generate new files with Easting and
 * Northings.
 * 
 * @author Nathan Collins
 */
public class UoBConverter {

	private static String fileIn = null;
	private static BufferedReader in;
	private static BufferedWriter out;
	private static String validation = "id,Date,Latitude,Longitude,Dip_Azimuth,Dip,LocalityId,Easting,Northing";

	/**
	 * Checks that the correct arguments have been passed.
	 * 
	 * @param args
	 *            the command line arguments: Should only be the path of the
	 *            file to convert.
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			fileIn = args[0];
			System.out.println("Converting file: " + fileIn);
			convertFile();
		} else {
			System.err.println("Incorrect arguments passed!");
			System.exit(1);
		}
	}

	/**
	 * This method reads in the file and checks that the column titles are
	 * correct. If they are, the Latitude and Longitude are converted and a new
	 * file is created, otherwise an error message is printed.
	 */
	private static void convertFile() {
		try {
			in = new BufferedReader(new FileReader(fileIn));
			String fileOut = fileIn.substring(0, fileIn.length() - 4);
			fileOut += "-converted.csv";
			out = new BufferedWriter(new FileWriter(fileOut));
			// Directly copy the column titles:
			String temp = in.readLine();
			System.out.println(temp);
			if (!temp.equals(validation)) {
				System.err.println("Invalid file passed!");
				System.exit(2);
			}
			System.out.println("Writing to file: " + fileOut);
			out.write(temp);
			out.newLine();
			temp = in.readLine();
			GPSConverter c = new GPSConverter();
			while (temp != null) {
				String[] values = temp.split(",");
				Double lat = Double.valueOf(values[2]);
				Double lon = Double.valueOf(values[3]);
				double[] results = c.convertToOSGB36(lat, lon, c.GRS80,
						c.NATIONAL_GRID);
				temp += results[0] + "," + results[1];
				out.write(temp);
				out.newLine();
				temp = in.readLine();
			}
			System.out.println("\nConversion complete!");
		} catch (FileNotFoundException ex) {
			System.err.println("FileNotFound Exceoption: " + ex);
		} catch (IOException ex) {
			System.err.println("IOExceoption: " + ex);
		} finally {
			try {
				in.close();
				out.close();
			} catch (IOException ex) {
				System.err.println("IOExceoption: unable to close IO! " + ex);
			}
		}

	}
}