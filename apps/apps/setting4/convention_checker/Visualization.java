package setting4.convention_checker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by nmtiwari on 7/23/16.
 */
public class Visualization {
	static String option = "       var options = {\n" + "          title: '#Null Checks',\n" + "          is3D: true,\n"
			+ "        };";
	static String footer = "  </head>\n" + "  <body>\n";
	static String footerEnd = "  </body>\n" + "</html>";
	private static String data;
	private static String header = "<html>\n" + "  <head>\n";
	public static void saveGraph(String grpData, String path) {
		String result = header + footer + grpData.replaceAll("\n", "<br>") + Visualization.footerEnd;
		File graph = new File(path);
		try {
			FileWriter gWriter = new FileWriter(graph, false); // true to append
			// false to overwrite.
			gWriter.write(result);
			gWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
