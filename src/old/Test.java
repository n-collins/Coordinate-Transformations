package old;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GPSConverter c = new GPSConverter();
		
		String Lat =  "52:39:27.2531";
		String Long = "1:43:4.5177";
		
		c.gratToGrid(c.stringToDecimal(Lat), c.stringToDecimal(Long), c.AIRY1820, c.NATIONAL_GRID);

	}

}
