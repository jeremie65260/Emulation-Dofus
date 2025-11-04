package soufix.object.entity;

import soufix.object.GameObject;
import soufix.utility.Pair;
import java.util.Map;
import java.util.ArrayList;

public class Capture extends GameObject
{
  private ArrayList<Pair<Integer, Integer>> monsters;
  
  public Capture(int id, int quantity, int template, int pos, String strStats)
  {
    super(id,template,quantity,pos,strStats,0);
    this.monsters=new ArrayList<>();
    this.parseStringToStats(strStats);
  }

  public void parseStringToStats(String m)
  {
    if(!m.equalsIgnoreCase(""))
    {
      if(this.monsters==null)
        this.monsters=new ArrayList<>();

      String[] split=m.split("\\|");
      for(String s : split)
      {
        try
        {
          int monstre=Integer.parseInt(s.split(",")[0]);
          int level=Integer.parseInt(s.split(",")[1]);
          Pair<Integer, Integer> couple=new Pair<Integer, Integer>(monstre,level);
          this.monsters.add(couple);
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }

  public String parseStatsString()
  {
    StringBuilder stats=new StringBuilder();
    boolean isFirst=true;
    for(Pair<Integer, Integer> coupl : this.monsters)
    {
      if(!isFirst)
        stats.append(",");
      try
      {
        stats.append("274#0#0#").append(Integer.toHexString(coupl.getLeft()));
      }
      catch(Exception e)
      {
        e.printStackTrace();
        continue;
      }
      isFirst=false;
    }
    return stats.toString();
  }

  public String parseGroupData()
  {
    StringBuilder toReturn=new StringBuilder();
    boolean isFirst=true;
    for(Pair<Integer, Integer> curMob : this.monsters)
    {
      if(!isFirst)
        toReturn.append(";");
      toReturn.append(curMob.getLeft()).append(",").append(curMob.getRight()).append(",").append(curMob.getRight());
      isFirst=false;
    }
    return toReturn.toString();
  }

  public String parseToSave()
  {
    StringBuilder toReturn=new StringBuilder();
    boolean isFirst=true;
    for(Pair<Integer, Integer> curMob : this.monsters)
    {
      if(!isFirst)
        toReturn.append("|");
      toReturn.append(curMob.getLeft()).append(",").append(curMob.getRight());
      isFirst=false;
    }
    return toReturn.toString();
  }
// Debut case 12 pour ramener ame à pnj
public int countMonster(int monsterId)
{
  if(monsterId<=0)
    return 0;

  int total=0;

  if(this.monsters!=null&&!this.monsters.isEmpty())
  {
    for(Pair<Integer, Integer> storedMonster : this.monsters)
    {
      if(storedMonster.getLeft()==monsterId)
        total++;
    }
  }

  if(total==0)
  {
    Map<Integer, Integer> soulStats=this.getSoulStat();
    if(soulStats!=null)
    {
      Integer souls=soulStats.get(monsterId);
      if(souls!=null&&souls>0)
        total+=souls;
    }
  }

  return total;
}

  public int consumeMonster(int monsterId, int amount)
  {
    if(monsterId<=0||amount<=0||this.monsters==null||this.monsters.isEmpty())
      return 0;

    int removed=0;
    for(int index=0;index<this.monsters.size()&&removed<amount;index++)
    {
      Pair<Integer, Integer> storedMonster=this.monsters.get(index);
      if(storedMonster.getLeft()!=monsterId)
        continue;
      this.monsters.remove(index);
      index--;
      removed++;
    }

    if(removed>0)
      this.setModification();

    return removed;
  }

  public boolean hasMonsters()
  {
    return this.monsters!=null&&!this.monsters.isEmpty();
  }
// fin case 12
  public static boolean isInArenaMap(int id)
  {
    return ",10131,10132,10133,10134,10135,10136,10137,10138,".contains(String.valueOf(","+id+","));
  }
}
