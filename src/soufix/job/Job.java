package soufix.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import soufix.job.fm.Rune;
import soufix.object.GameObject;

public class Job {

  private int id;
  private final ArrayList<Integer> tools = new ArrayList<>();
  private final Map<Integer, ArrayList<Integer>> crafts = new HashMap<>();
  private final Map<Integer, ArrayList<Integer>> skills = new HashMap<>();
  private String name;

  public Job(int id, String tools, String crafts, String skills, String name) {
    this.name = name;
    this.id = id;

    // Outils
    if (tools != null && !tools.isEmpty()) {
      for (String str : tools.split(",")) {
        try {
          this.tools.add(Integer.parseInt(str));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // Crafts
    if (crafts != null && !crafts.isEmpty()) {
      for (String str : crafts.split("\\|")) {
        try {
          String[] parts = str.split(";");
          int skID = Integer.parseInt(parts[0]);

          ArrayList<Integer> list = new ArrayList<>();
          for (String str2 : parts[1].split(",")) {
            list.add(Integer.parseInt(str2));
          }

          this.crafts.put(skID, list);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // Skills
    if (skills != null && !skills.isEmpty()) {
      for (String arg0 : skills.split("\\|")) {
        String[] split = arg0.split(";");
        String io = split[0];
        String skill = split[1];

        ArrayList<Integer> list = new ArrayList<>();
        for (String arg1 : skill.split(",")) {
          list.add(Integer.parseInt(arg1));
        }

        for (String arg1 : io.split(",")) {
          this.skills.put(Integer.parseInt(arg1), list);
        }
      }
    }
  }

  public int getId() {
    return this.id;
  }

  public Map<Integer, ArrayList<Integer>> getSkills() {
    return skills;
  }

  public boolean isValidTool(int id1) {
    if (this.tools.isEmpty()) {
      return true;
    }
    return this.tools.contains(id1);
  }

  public ArrayList<Integer> getListBySkill(int skill) {
    return this.crafts.get(skill);
  }

  public boolean canCraft(int skill, int template) {
    ArrayList<Integer> list = this.crafts.get(skill);
    if (list != null) {
      for (int id : list) {
        if (id == template) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isMaging() {
    return (this.id > 42 && this.id < 51) || (this.id > 61 && this.id < 65);
  }

  public static int getActualJet(GameObject obj, String statsModif) {
    for (Entry<Integer, Integer> entry : obj.getStats().getMap().entrySet()) {
      String statKey = Integer.toHexString(entry.getKey());
      int cmp = statKey.compareTo(statsModif);

      if (cmp > 0) { // Effets inutiles
        continue;
      } else if (cmp == 0) { // L'effet existe bien !
        return entry.getValue();
      }
    }
    return 0;
  }

  // v2.8 - complete negative stat maging
  public static int viewActualStatsItem(GameObject obj, String runeStat) {
    if (!obj.parseStatsString().isEmpty()) {
      for (Entry<Integer, Integer> entry : obj.getStats().getMap().entrySet()) {
        String key = Integer.toHexString(entry.getKey());

        if (!key.equalsIgnoreCase(runeStat)) { // Rune is not on item
          if (Rune.getNegativeStatByRuneStat(runeStat).equalsIgnoreCase(key)) {
            return 2;
          } else {
            continue;
          }
        } else { // Rune is on item
          return 1;
        }
      }
    }
    return 0;
  }

  public String getName() {
    return name;
  }
}
