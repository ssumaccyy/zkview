package util;

import dao.FastButtonCfg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FastConnectHelper {
  public static final String UUID_TMP_FILE_FAST_BUTTON = "A8E4F7C4-9E2C-4C11-9A4C-8E0F4F4F4F4F";
  public static final int FAST_BUTTON_CFG_TRUNK_COUNT = 3;

  public static List<FastButtonCfg> fetchFastButtonCfg() {
    var list = parseAndMaintainFastButtonCfg();
    LinkedList<FastButtonCfg> fastButtonCfgList = new LinkedList<>(list);
    if (fastButtonCfgList.isEmpty()) {
      fastButtonCfgList.add(new FastButtonCfg("10.106.1.1", 12181));
    }
    if (fastButtonCfgList.size() == 1) {
      fastButtonCfgList.add(new FastButtonCfg("10.106.1.1", 22181));
    }
    if (fastButtonCfgList.size() == 2) {
      fastButtonCfgList.add(new FastButtonCfg("10.106.1.1", 32181));
    }

    return fastButtonCfgList;
  }

  public static void writeToTmpFile(String content) {
    try {
      String tmpDir = System.getProperty("java.io.tmpdir");
      File tmpFile = new File(tmpDir, UUID_TMP_FILE_FAST_BUTTON);
      if (!tmpFile.exists()) {
        // noinspection ResultOfMethodCallIgnored
        tmpFile.createNewFile();
      }
      FileWriter fw = new FileWriter(tmpFile, StandardCharsets.UTF_8, true);
      fw.write(content);
      fw.write(Character.LINE_SEPARATOR);
      fw.close();
    } catch (IOException ignored) {

    }
  }

  public static List<FastButtonCfg> parseAndMaintainFastButtonCfg() {
    String tmpDir = System.getProperty("java.io.tmpdir");
    File tmpFile = new File(tmpDir, UUID_TMP_FILE_FAST_BUTTON);
    if (!tmpFile.exists() || tmpFile.isDirectory()) {
      return Collections.emptyList();
    }
    LinkedList<FastButtonCfg> cfgList = new LinkedList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(tmpFile));
      var readLines = new ArrayList<>(br.lines().toList()); // 读出来的所有行，可能有重复
      var noRepeatSet = new LinkedHashSet<>(readLines); // 去重之后的行
      br.close();
      var arrayList = new ArrayList<>(noRepeatSet);
      Collections.reverse(arrayList); // 顺序翻转   // 1,2,3 -> 3,2,1
      String[] sArray = arrayList.toArray(new String[0]);
      if (sArray.length == 0) {
        return Collections.emptyList();
      }

      // 如果行数过大就直接截断
      if (sArray.length > FAST_BUTTON_CFG_TRUNK_COUNT) {
        sArray = Arrays.copyOf(sArray, FAST_BUTTON_CFG_TRUNK_COUNT);
      }


      var toWrite = new ArrayList<String>(sArray.length);
      for (String s : sArray) {
        if (s.isBlank()) {
          continue;
        }
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
          toWrite.add(s);
        }
      }
      Collections.reverse(toWrite);
      FileWriter fw = new FileWriter(tmpFile, false);
      for (String toWriteStr : toWrite) {
        fw.write(toWriteStr);
        fw.write(Character.LINE_SEPARATOR);
      }
      fw.close();
      return cfgList;
    } catch (IOException e) {
      return Collections.emptyList();
    }
  }
}
