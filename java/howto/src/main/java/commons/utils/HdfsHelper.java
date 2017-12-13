package commons.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HdfsHelper {
  public static FileSystem getFileSystem(String cluster) throws IOException {
    try {
      Configuration conf = new Configuration();
      conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
      conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

      FileSystem fileSystem = FileSystem.get(new URI("hdfs://" + cluster + ":6230"), conf);
      return fileSystem;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> readLines(String cluster, String path, int limit) {
    try (FileSystem fs = getFileSystem(cluster);
         FSDataInputStream in = fs.open(new Path(path))) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String textLine;
      List<String> lines = new ArrayList<>();
      while ((textLine = reader.readLine()) != null) {
        lines.add(textLine);
        if (lines.size() >= limit) break;
      }
      return lines;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
