package soufix.area.map.labyrinth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import soufix.area.map.GameMap;
import soufix.client.other.Stats;
import soufix.entity.monster.MobGrade;
import soufix.entity.monster.MobGroup;
import soufix.entity.monster.Monster;
import soufix.job.JobConstant;
import soufix.main.Constant;
import soufix.main.Main;
import soufix.object.ObjectTemplate;
import soufix.utility.TimerWaiterPlus;

public class Gladiatrool
{

  private static final Set<Integer> BOSS_IDS=new HashSet<>(Arrays.asList(58,85,86,107,113,121,147,173,180,225,226,230,232,251,252,257,289,295,374,375,377,382,404,423,430,457,478,519,568,605,612,669,670,673,675,677,681,780,792,797,799,800,827,854,926,939,940,943,1015,1027,1045,1051,1071,1072,1085,1086,1087,1159,1184,1185,1186,1187,1188));
  private static final Set<Integer> RESOURCE_PROTECTOR_IDS=new HashSet<>();
  private static final Random RANDOM=new Random();
  private static List<BonusOption> TONIQUE_BONUS_POOL=null;

  static
  {
    for(int[] protector : JobConstant.JOB_PROTECTORS)
      RESOURCE_PROTECTOR_IDS.add(protector[0]);
  }

  public static void initialize()
  {
    initializeGladiatrool();
  }

  public static List<BonusOption> rollToniqueBonuses(int count)
  {
    List<BonusOption> pool=getToniqueBonusPool();
    if(pool.isEmpty())
      return Collections.emptyList();
    List<BonusOption> shuffled=new ArrayList<>(pool);
    Collections.shuffle(shuffled,RANDOM);
    List<BonusOption> choices=new ArrayList<>();
    for(int i=0;i<count;i++)
      choices.add(shuffled.get(i%shuffled.size()));
    return choices;
  }

  public static String formatBonusLabel(int statId, int value)
  {
    String statName=getStatName(statId);
    if(value>=0)
      return "+"+value+" "+statName;
    return value+" "+statName;
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

  private static List<BonusOption> getToniqueBonusPool()
  {
    if(TONIQUE_BONUS_POOL!=null)
      return TONIQUE_BONUS_POOL;
    List<BonusOption> options=new ArrayList<>();
    for(ObjectTemplate template : Main.world.getObjTemplates())
    {
      if(template==null||template.getName()==null)
        continue;
      String name=template.getName().toLowerCase(Locale.ROOT);
      if(!name.contains("tonique"))
        continue;
      Stats stats=template.generateNewStatsFromTemplate(template.getStrTemplate(),true);
      for(java.util.Map.Entry<Integer, Integer> entry : stats.getMap().entrySet())
      {
        if(entry.getValue()==null||entry.getValue()<=0)
          continue;
        options.add(new BonusOption(entry.getKey(),entry.getValue()));
      }
    }
    if(options.isEmpty())
    {
      options.add(new BonusOption(Constant.STATS_ADD_VITA,50));
      options.add(new BonusOption(Constant.STATS_ADD_SAGE,10));
      options.add(new BonusOption(Constant.STATS_ADD_FORC,10));
      options.add(new BonusOption(Constant.STATS_ADD_INTE,10));
      options.add(new BonusOption(Constant.STATS_ADD_CHAN,10));
      options.add(new BonusOption(Constant.STATS_ADD_AGIL,10));
    }
    TONIQUE_BONUS_POOL=options.stream().distinct().collect(Collectors.toList());
    return TONIQUE_BONUS_POOL;
  }

  private static String getStatName(int statId)
  {
    switch(statId)
    {
      case Constant.STATS_ADD_VITA:
        return "Vitalité";
      case Constant.STATS_ADD_SAGE:
        return "Sagesse";
      case Constant.STATS_ADD_FORC:
        return "Force";
      case Constant.STATS_ADD_INTE:
        return "Intelligence";
      case Constant.STATS_ADD_CHAN:
        return "Chance";
      case Constant.STATS_ADD_AGIL:
        return "Agilité";
      case Constant.STATS_ADD_PA:
      case Constant.STATS_ADD_PA2:
        return "PA";
      case Constant.STATS_ADD_PM:
      case Constant.STATS_ADD_PM2:
        return "PM";
      case Constant.STATS_ADD_PO:
        return "Portée";
      case Constant.STATS_ADD_DOMA:
        return "Dommages";
      case Constant.STATS_ADD_PERDOM:
        return "Dommages %";
      case Constant.STATS_ADD_PDOM:
        return "Dommages poussée";
      case Constant.STATS_ADD_SOIN:
        return "Soins";
      case Constant.STATS_ADD_INIT:
        return "Initiative";
      case Constant.STATS_ADD_PODS:
        return "Pods";
      default:
        return "Effet "+statId;
    }
  }

  public static class BonusOption
  {
    private final int statId;
    private final int value;

    public BonusOption(int statId, int value)
    {
      this.statId=statId;
      this.value=value;
    }

    public int getStatId()
    {
      return statId;
    }

    public int getValue()
    {
      return value;
    }

    public String getLabel()
    {
      return formatBonusLabel(statId,value);
    }

    @Override
    public boolean equals(Object other)
    {
      if(this==other)
        return true;
      if(other==null||getClass()!=other.getClass())
        return false;
      BonusOption that=(BonusOption)other;
      return statId==that.statId&&value==that.value;
    }

    @Override
    public int hashCode()
    {
      return 31*statId+value;
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
}
