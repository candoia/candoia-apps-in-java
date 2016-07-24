package methodCallFrequency;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by nmtiwari on 7/23/16.
 */
public class MethodUsageCharting {
    static String option = "       var options = {\n" +
            "          title: 'My Daily Activities',\n" +
            "          is3D: true,\n" +
            "        };";
    static String footer = "var chart = new google.visualization.PieChart(document.getElementById('piechart_3d'));\n" +
            "        chart.draw(data, options);\n" +
            "      }\n" +
            "    </script>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    <div id=\"piechart_3d\" style=\"width: 900px; height: 500px;\"></div>\n" +
            "  </body>\n" +
            "</html>";
    private static String data;
    private static String header = "<html>\n" +
            "  <head>\n" +
            "    <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
            "    <script type=\"text/javascript\">\n" +
            "      google.charts.load(\"current\", {packages:[\"corechart\"]});\n" +
            "      google.charts.setOnLoadCallback(drawChart);\n" +
            "      function drawChart() {\nvar data = google.visualization.arrayToDataTable([\n";
    private static String headerE = " ]);";

    private static String convert(HashMap<String, Integer> map) {
        String result = "['METHOD', 'NUMBER OF USAGE'],";
        for (String k : map.keySet()) {
            result += "['" + k + "', " + map.get(k) + "],\n";
        }
        result = result.substring(0, result.lastIndexOf(','));
        return result;
    }

    private static String getDataAsGraph(String grpD) {
        data = grpD;
        return header + data + headerE + "\n" + option + "\n" + footer;
    }

    public static void saveGraph(HashMap<String, Integer> grpData, String path) {
        String result = convert(grpData);
        result = getDataAsGraph(result);

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
