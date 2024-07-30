package util;

import dao.FastButtonCfg;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FastConnectHelper {
  public static final String UUID_TMP_FILE_FAST_BUTTON = "A8E4F7C4-9E2C-4C11-9A4C-8E0F4F4F4F4F";

  public static List<FastButtonCfg> fetchFastButtonCfg() {
    var list = parseFastButtonCfg();
    LinkedList<FastButtonCfg> fastButtonCfgList = new LinkedList<>(list);
    if (fastButtonCfgList.isEmpty()) {
      fastButtonCfgList.add(new FastButtonCfg("10.106.1.1", 12181));
    }
    if (fastButtonCfgList.size() == 1) {
      fastButtonCfgList.add(new FastButtonCfg("192.168.189.215", 12181));
    }
    if (fastButtonCfgList.size() == 2) {
      fastButtonCfgList.add(new FastButtonCfg("192.168.189.13", 12181));
    }

    return fastButtonCfgList;
  }

  public static void writeToTmpFile(String content) {
    try{
      String tmpDir = System.getProperty("java.io.tmpdir");
      File tmpFile = new File(tmpDir, UUID_TMP_FILE_FAST_BUTTON);
      if (!tmpFile.exists()) {
        tmpFile.createNewFile();
      }
      FileWriter fw = new FileWriter(tmpFile);

      fw.write(content);
      fw.close();
    } catch (IOException e) {

    }
  }

  public static List<FastButtonCfg> parseFastButtonCfg() {
    String tmpDir = System.getProperty("java.io.tmpdir");
    File tmpFile = new File(tmpDir, UUID_TMP_FILE_FAST_BUTTON);
    if (!tmpFile.exists() || tmpFile.isDirectory()) {
      return Collections.emptyList();
    }
    LinkedList<FastButtonCfg> cfgList = new LinkedList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(tmpFile));
      String line = br.readLine();
      br.close();
      String[] sArray = line.split(";");
      if (sArray.length == 0 || sArray.length > 2) {
        return Collections.emptyList();
      }
      for (String s : sArray) {
        String[] sArray2 = s.split(":");
        if (sArray2.length != 2) {
          return Collections.emptyList();
        }
        if (
            sArray2[0].matches("((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)") &&
                sArray2[1].matches("(6553[0-5]|6552[0-9]|65[0-2][0-9]{2}|62[0-4][0-9]{1,2}|61[0-9]{1,2}|[1-5][0-9]{4}|[1-9][0-9]{3}|[1-9][0-9]{2}|[1-9][0-9]|1)")
        ) {
          FastButtonCfg cfg = new FastButtonCfg(sArray2[0], Integer.parseInt(sArray2[1]));
          cfgList.add(cfg);
        }
      }

    } catch (IOException e) {
      return Collections.emptyList();
    }
    return cfgList;
  }
}
