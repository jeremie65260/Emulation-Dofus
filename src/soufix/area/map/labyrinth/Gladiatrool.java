package soufix.area.map.labyrinth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import soufix.area.map.GameMap;
import soufix.entity.monster.MobGrade;
import soufix.entity.monster.MobGroup;
import soufix.entity.monster.Monster;
import soufix.job.JobConstant;
import soufix.main.Constant;
import soufix.main.Main;
import soufix.utility.TimerWaiterPlus;

public class Gladiatrool
{

  private static final Set<Integer> BOSS_IDS=new HashSet<>(Arrays.asList(58,85,86,107,113,121,147,173,180,225,226,230,232,251,252,257,289,295,374,375,377,382,404,423,430,457,478,519,568,605,612,669,670,673,675,677,681,780,792,797,799,800,827,854,926,939,940,943,1015,1027,1045,1051,1071,1072,1085,1086,1087,1159,1184,1185,1186,1187,1188));
  private static final Set<Integer> EXCLUDED_IDS=new HashSet<>(Arrays.asList(5002,5003,5004,5005,5006,5007,5008,5009,5010,5011,5012,5013,5014,5015,5016,5017,5018,5019,5020,5021,5022,5030,5031,5032,5033,5034,5035,5036,5037,5038,5039,5040,5041,5042,5043,5044,5045,5046,5047,5048,5049,5050,5051,5052,5053,5054,5056,5057,5058,5059,5060,5061,5062,5063,5064,5065,5066,5067,5068,5069,5070,5071,5072,5073,5074,5075,5076,5077,5078,5079,5080,5081,5082));
  private static final Set<String> EXCLUDED_NAMES=new HashSet<>(Arrays.asList("TEST","GARDIENNE DES EGOUTS","GARDIENNE DES ÉGOUTS","MOMIE NOVA","SPHINCTER CELL"));
  private static final Set<Integer> RESOURCE_PROTECTOR_IDS=new HashSet<>();
  private static final Random RANDOM=new Random();

  static
  {
    for(int[] protector : JobConstant.JOB_PROTECTORS)
      RESOURCE_PROTECTOR_IDS.add(protector[0]);
  }

  public static void initialize()
  {
    initializeGladiatrool();
  }

  public static void respawn(short mapid)
  {
    TimerWaiterPlus.addNext(() -> spawnGroupGladiatrool(mapid),10,TimeUnit.SECONDS);
  }

  public static void respawnSameGroup(short mapid, MobGroup group)
  {
    String groupData=buildGroupData(group);
    if(groupData==null||groupData.isEmpty())
      return;
    respawnSameGroup(mapid,groupData);
  }

  public static void respawnSameGroup(short mapid, String groupData)
  {
    TimerWaiterPlus.addNext(() -> spawnGroupGladiatrool(mapid,groupData),10,TimeUnit.SECONDS);
  }

  private static void initializeGladiatrool()
  {
    spawnGroups();
  }

  private static void spawnGroups()
  {
    for(short i=15000;Constant.isInGladiatorDonjon(i);i=(short)(i+8))
    {
      if(Constant.isInGladiatorDonjon(i))
        spawnGroupGladiatrool(i);
    }
  }


  private static void spawnGroupGladiatrool(short mapid)
  {
    spawnGroupGladiatrool(mapid,null);
  }

  private static void spawnGroupGladiatrool(short mapid, String groupData)
  {
    if(!Constant.isInGladiatorDonjon(mapid))
      return;

    GameMap map=Main.world.getMap(mapid);
    if(map==null)
    {
      Main.world.logger.warn("Gladiatrool map {} not found, skipping spawn.",mapid);
      return;
    }

    int min=1,max=1,minArchi=1,maxArchi=1,minBoss=1,maxBoss=1,nbMob=0;
    boolean hasBoss=false;
    boolean hasArchi=false;
    String generatedGroupData="";

    if(groupData==null||groupData.isEmpty())
    {
      switch(mapid)
      {
        case 15000: // 10 jeton
          min=40;
          max=51;
          break;
        case 15008: // 30 jeton
          min=50;
          max=70;
          break;
        case 15016: // 70 jeton
          hasArchi=true;
          minArchi=40;
          maxArchi=50;
          min=60;
          max=80;
          break;
        case 15024: // 130 jeton
          hasArchi=true;
          minArchi=50;
          maxArchi=60;
          min=80;
          max=100;
          break;
        case 15032: // 220 jeton
          hasBoss=true;
          minBoss=140;
          maxBoss=190;
          min=90;
          max=120;
          break;
        case 15040: // 340 jeton
          hasBoss=true;
          min=115;
          max=140;
          minBoss=140;
          maxBoss=190;
          break;
        case 15048: // 500 jeton
          hasBoss=true;
          hasArchi=true;
          minArchi=90;
          maxArchi=110;
          minBoss=140;
          maxBoss=200;
          min=120;
          max=170;
          break;
        case 15056: // 700 jeton
          hasArchi=true;
          hasBoss=true;
          minArchi=100;
          maxArchi=120;
          minBoss=140;
          maxBoss=440;
          min=125;
          max=200;
          break;
        case 15064: // 950 jeton
          hasBoss=true;
          hasArchi=true;
          minArchi=120;
          maxArchi=140;
          minBoss=180;
          maxBoss=480;
          min=130;
          max=210;
          break;
        case 15072: // 1250 jeton
          hasArchi=true;
          hasBoss=true;
          minArchi=140;
          maxArchi=200;
          minBoss=440;
          maxBoss=1000;
          min=170;
          max=250;
          break;
        case 15080: // palier final
          hasArchi=true;
          hasBoss=true;
          minArchi=140;
          maxArchi=200;
          minBoss=440;
          maxBoss=1000;
          min=170;
          max=250;
          break;
      }

      ArrayList<MobGrade> regularGrades=collectMobGrades(min,max,Gladiatrool::isStandardMonster);
      ArrayList<MobGrade> bossGrades=collectMobGrades(minBoss,maxBoss,Gladiatrool::isBossMonster);
      ArrayList<MobGrade> archiGrades=collectMobGrades(minArchi,maxArchi,Gladiatrool::isArchiMonster);

      if(hasBoss)
      {
        MobGrade grade=pickRandom(bossGrades);
        if(grade==null)
          grade=pickRandom(regularGrades);
        if(grade!=null)
        {
          generatedGroupData+=grade.getTemplate().getId()+","+grade.getLevel()+","+grade.getLevel()+";";
          nbMob++;
        }
      }
      if(hasArchi)
      {
        MobGrade grade=pickRandom(archiGrades);
        if(grade==null)
          grade=pickRandom(regularGrades);
        if(grade!=null)
        {
          generatedGroupData+=grade.getTemplate().getId()+","+grade.getLevel()+","+grade.getLevel()+";";
          nbMob++;
        }
      }
      while(nbMob<4&&!regularGrades.isEmpty())
      {
        int randomIndex=RANDOM.nextInt(regularGrades.size());
        MobGrade grade=regularGrades.remove(randomIndex);
        generatedGroupData+=grade.getTemplate().getId()+","+grade.getLevel()+","+grade.getLevel()+";";
        nbMob++;
      }

      if(generatedGroupData.isEmpty())
      {
        Main.world.logger.warn("Gladiatrool spawn skipped on map {} due to missing eligible mobs.",mapid);
        return;
      }
      groupData=generatedGroupData;
    }
    else
    {
      groupData=sanitizeGroupData(groupData);
      if(groupData.isEmpty())
      {
        Main.world.logger.warn("Gladiatrool spawn skipped on map {} due to excluded mobs in group data.",mapid);
        return;
      }
    }

    if(map.getMobGroups().size()<1)
    {
      map.spawnGroupGladiatrool(groupData);
      Main.world.logger.trace("   >> new gladiatrool groupe in {}.",mapid);
    }
    else
    {
      Main.world.logger.trace("   >> ignore new gladiatrool groupe in {}.",mapid);
    }
  }

  private static ArrayList<MobGrade> collectMobGrades(int min, int max, Predicate<Monster> filter)
  {
    ArrayList<MobGrade> grades=new ArrayList<>();
    for(Monster monster : Main.world.getMonstres())
    {
      if(monster==null)
        continue;
      if(isExcludedFromGladiatrool(monster))
        continue;
      if(isResourceProtector(monster))
        continue;
      if(filter!=null&&!filter.test(monster))
        continue;
      for(MobGrade grade : monster.getGrades().values())
      {
        if(grade==null)
          continue;
        if(grade.getLevel()>=min&&grade.getLevel()<=max)
        {
          // Filtre les mobs ultra faibles (10 PV, 1 PA, 1 PM) pour le Gladiatrool
          if(grade.getPdvMax()==10&&grade.getPa()==1&&grade.getPm()==1)
            continue;
          grades.add(grade);
        }
      }
    }
    return grades;
  }

  private static boolean isBossMonster(Monster monster)
  {
    return monster!=null&&BOSS_IDS.contains(monster.getId());
  }

  private static boolean isExcludedFromGladiatrool(Monster monster)
  {
    if(monster==null)
      return false;
    if(EXCLUDED_IDS.contains(monster.getId()))
      return true;
    String name=monster.getName();
    return name!=null&&EXCLUDED_NAMES.contains(name.toUpperCase());
  }

  private static boolean isArchiMonster(Monster monster)
  {
    return monster!=null&&Main.world.archi.contains("."+monster.getId()+".");
  }

  private static boolean isStandardMonster(Monster monster)
  {
    return monster!=null&&!isBossMonster(monster)&&!isArchiMonster(monster);
  }

  private static boolean isResourceProtector(Monster monster)
  {
    return monster!=null&&RESOURCE_PROTECTOR_IDS.contains(monster.getId());
  }

  private static MobGrade pickRandom(List<MobGrade> grades)
  {
    if(grades==null||grades.isEmpty())
      return null;
    return grades.get(RANDOM.nextInt(grades.size()));
  }

  private static String buildGroupData(MobGroup group)
  {
    if(group==null||group.getMobs()==null||group.getMobs().isEmpty())
      return "";
    StringBuilder groupData=new StringBuilder();
    for(MobGrade grade : group.getMobs().values())
    {
      if(grade==null||grade.getTemplate()==null)
        continue;
      if(groupData.length()>0)
        groupData.append(";");
      int level=grade.getLevel();
      groupData.append(grade.getTemplate().getId()).append(",").append(level).append(",").append(level);
    }
    return groupData.toString();
  }

  private static String sanitizeGroupData(String groupData)
  {
    if(groupData==null||groupData.isEmpty())
      return "";
    StringBuilder sanitized=new StringBuilder();
    for(String entry : groupData.split(";"))
    {
      if(entry==null||entry.isEmpty())
        continue;
      String[] parts=entry.split(",");
      if(parts.length==0)
        continue;
      try
      {
        int mobId=Integer.parseInt(parts[0]);
        if(EXCLUDED_IDS.contains(mobId))
          continue;
      }
      catch(Exception e)
      {
        continue;
      }
      if(sanitized.length()>0)
        sanitized.append(";");
      sanitized.append(entry);
    }
    return sanitized.toString();
  }
}
