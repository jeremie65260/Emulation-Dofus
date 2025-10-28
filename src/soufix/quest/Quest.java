package soufix.quest;

import soufix.client.Player;
import soufix.common.SocketManager;
import soufix.database.Database;
import soufix.entity.npc.NpcTemplate;
import soufix.game.World;
import soufix.game.action.ExchangeAction;
import soufix.main.Config;
import soufix.main.Constant;
import soufix.main.Main;
import soufix.object.GameObject;
import soufix.object.ObjectTemplate;
import soufix.other.Action;
import soufix.utility.Pair;
import soufix.object.entity.Capture;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Objects;

public class Quest
{
  private static Map<Integer, Quest> questList=new HashMap<>();

  public static Map<Integer, Quest> getQuestList()
  {
    return questList;
  }

  public static Quest getQuestById(int id)
  {
    return questList.get(id);
  }

  public static void addQuest(Quest quest)
  {
    questList.put(quest.getId(),quest);
  }

  private int id;
  private ArrayList<QuestStep> questSteps=new ArrayList<>();
  private ArrayList<QuestObjectif> questObjectifList=new ArrayList<>();
  private NpcTemplate npc=null;
  private ArrayList<Action> actions=new ArrayList<>();
  private boolean delete;
  private Pair<Integer, Integer> condition=null;
  private static final String CAPTURE_CLASS_NAME="soufix.object.entity.Capture";
  private static Class<?> captureClassCache;
  private static Field captureMonstersField;
  private static boolean captureFieldLookupDone;

  public Quest(int id, String steps, String objectifs, int npc, String action, String args, boolean delete, String condition)
  {
    this.id=id;
    this.delete=delete;
    try
    {
      if(!steps.equalsIgnoreCase(""))
      {
        String[] split=steps.split(";");

        if(split.length>0)
        {
          for(String stepId : split)
          {
            QuestStep questStep=QuestStep.getQuestStepById(Integer.parseInt(stepId));
            if(questStep==null)
              continue;
            questStep.setQuestData(this);
            questSteps.add(questStep);
          }
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      if(!objectifs.equalsIgnoreCase(""))
      {
        String[] split=objectifs.split(";");

        if(split.length>0)
        {
          for(String objectifId : split)
          {
            if(objectifId==null||objectifId.isEmpty())
              continue;
            QuestObjectif questObjectif=QuestObjectif.getQuestObjectifById(Integer.parseInt(objectifId));
            if(questObjectif!=null)
              questObjectifList.add(questObjectif);
            //fin case 12
          }
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    if(!condition.equalsIgnoreCase(""))
    {
      try
      {
        String[] split=condition.split(":");
        if(split.length>0)
        {
          this.condition=new Pair<>(Integer.parseInt(split[0]),Integer.parseInt(split[1]));
        }
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
    this.npc=Main.world.getNPCTemplate(npc);
    try
    {
      if(!action.equalsIgnoreCase("")&&!args.equalsIgnoreCase(""))
      {
        String[] arguments=args.split(";");
        int nbr=0;
        for(String loc0 : action.split(","))
        {
          int actionId=Integer.parseInt(loc0);
          String arg=arguments[nbr];
          actions.add(new Action(actionId,arg,-1+"",null));
          nbr++;
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
      Main.world.logger.error("Erreur avec l action et les args de la quete "+this.id+".");
    }
  }

  public int getId()
  {
    return id;
  }

  public boolean isDelete()
  {
    return this.delete;
  }

  public NpcTemplate getNpcTemplate()
  {
    return npc;
  }

  public ArrayList<QuestStep> getQuestSteps()
  {
    return questSteps;
  }

  private boolean haveRespectCondition(QuestPlayer questPlayer, QuestStep questStep)
  {
    switch(questStep.getCondition())
    {
      case "1": //Valider les etapes d'avant
        boolean loc2=true;
        for(QuestStep step : this.questSteps)
        {
          if(step!=null&&step.getId()!=questStep.getId()&&!questPlayer.isQuestStepIsValidate(step))
          {
            loc2=false;
          }
        }
        return loc2;

      case "0":
        return true;
    }
    return false;
  }

  public String getGmQuestDataPacket(Player player)
  {
    QuestPlayer questPlayer=player.getQuestPersoByQuest(this);
    // Case 12
    if(questPlayer==null)
      return "";
    // Case 12
    int loc1=getObjectifCurrent(questPlayer);
    int loc2=getObjectifPrevious(questPlayer);
    int loc3=getNextObjectif(QuestObjectif.getQuestObjectifById(getObjectifCurrent(questPlayer)));
    StringBuilder str=new StringBuilder();
    str.append(id).append("|");
    str.append(loc1>0 ? loc1 : "");
    str.append("|");

    StringBuilder str_prev=new StringBuilder();
    boolean loc4=true;
    // Il y a une exeption dans le code ici pour la seconde étape de papotage
    for(QuestStep step : questSteps)
    {
      if(step.getObjectif()!=loc1)
        continue;
      if(!haveRespectCondition(questPlayer,step))
        continue;
      if(!loc4)
        str_prev.append(";");
      str_prev.append(step.getId());
      str_prev.append(",");
      str_prev.append(questPlayer.isQuestStepIsValidate(step) ? 1 : 0);
      loc4=false;
    }
    str.append(str_prev);
    str.append("|");
    str.append(loc2>0 ? loc2 : "").append("|");
    str.append(loc3>0 ? loc3 : "");
    if(npc!=null)
    {
      str.append("|");
      str.append(npc.getInitQuestionId(player.getCurMap().getId())).append("|");
    }
    return str.toString();
  }

  public QuestStep getCurrentQuestStep(QuestPlayer questPlayer)
  {
    for(QuestStep step : getQuestSteps())
    {
      if(!questPlayer.isQuestStepIsValidate(step))
      {
        return step;
      }
    }
    return null;
  }

  private int getObjectifCurrent(QuestPlayer questPlayer)
  {
    for(QuestStep step : questSteps)
    {
      if(!questPlayer.isQuestStepIsValidate(step))
      {
        return step.getObjectif();
      }
    }
    return 0;
  }

  private int getObjectifPrevious(QuestPlayer questPlayer)
  {
    if(questObjectifList.size()==1)
      return 0;
    else
    {
      int previous=0;
      for(QuestObjectif qObjectif : questObjectifList)
      {
        if(qObjectif.getId()==getObjectifCurrent(questPlayer))
          return previous;
        else
          previous=qObjectif.getId();
      }
    }
    return 0;
  }

  private int getNextObjectif(QuestObjectif questObjectif)
  {
    if(questObjectif==null)
      return 0;
    for(QuestObjectif objectif : questObjectifList)
    {
      if(objectif.getId()==questObjectif.getId())
      {
        int index=questObjectifList.indexOf(objectif);
        if(questObjectifList.size()<=index+1)
          return 0;
        return questObjectifList.get(index+1).getId();
      }
    }
    return 0;
  }

  public void applyQuest(Player player)
  {
    if(this.condition!=null)
    {
      switch(this.condition.getLeft())
      {
        case 1: // Niveau
          if(player.getLevel()<this.condition.getRight())
          {
            SocketManager.GAME_SEND_MESSAGE(player,"Votre niveau n'est pas assez élevé pour recevoir cette quéte.");
            return;
          }
          break;
      }
    }

    QuestPlayer questPlayer=new QuestPlayer(Database.getDynamics().getQuestPlayerData().getNextId(),id,false,player.getId(),"");
    player.addQuestPerso(questPlayer);
    SocketManager.GAME_SEND_Im_PACKET(player,"054;"+this.id);
    Database.getDynamics().getQuestPlayerData().add(questPlayer);
    SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(player.getGameClient(),player.getCurMap());

    if(!this.actions.isEmpty())
    {
      for(Action aAction : this.actions)
      {
        aAction.apply(player,player,-1,-1);
      }
    }

    //Database.getStatics().getPlayerData().update(player);
  }

  public void updateQuestData(Player player, boolean validation, int type)
  {
    QuestPlayer questPlayer=player.getQuestPersoByQuest(this);
    // début type 12
    if(questPlayer==null)
      return;
    // fin type 12
    for(QuestStep questStep : this.questSteps)
    {
      if(questStep.getValidationType()!=type||questPlayer.isQuestStepIsValidate(questStep)) //On a déjé validé la questEtape on passe
        continue;
      if(questStep.getObjectif()!=getObjectifCurrent(questPlayer)||!haveRespectCondition(questPlayer,questStep))
        continue;

      boolean refresh=false;

      if(validation)
        refresh=true;
      switch(questStep.getType())
      {
        case 3://Donner item
          if(player.getExchangeAction()!=null&&player.getExchangeAction().getType()==ExchangeAction.TALKING_WITH&&player.getCurMap().getNpc((Integer)player.getExchangeAction().getValue()).getTemplate().getId()==questStep.getNpc().getId())
          {
            for(Entry<Integer, Integer> entry : questStep.getItemNecessaryList().entrySet())
            {
              if(player.hasItemTemplate(entry.getKey(),entry.getValue()))
              { //Il a l'item et la quantité
                player.removeByTemplateID(entry.getKey(),entry.getValue()); //On supprime donc
                refresh=true;
              }
            }
          }
          break;

        case 0:
        case 1://Aller voir %
        case 9://Retourner voir %
          if(questStep.getCondition().equalsIgnoreCase("1"))
          { //Valider les questEtape avant
            if(player.getExchangeAction()!=null&&player.getExchangeAction().getType()==ExchangeAction.TALKING_WITH&&player.getCurMap().getNpc((Integer)player.getExchangeAction().getValue()).getTemplate().getId()==questStep.getNpc().getId())
            {
              if(haveRespectCondition(questPlayer,questStep))
              {
                refresh=true;
              }
            }
          }
          else
          {
            if(player.getExchangeAction()!=null&&player.getExchangeAction().getType()==ExchangeAction.TALKING_WITH&&player.getCurMap().getNpc((Integer)player.getExchangeAction().getValue()).getTemplate().getId()==questStep.getNpc().getId())
              refresh=true;
          }
          break;

        case 6: // monstres
          for(Entry<Integer, Short> entry : questPlayer.getMonsterKill().entrySet())
            if(entry.getKey()==questStep.getMonsterId()&&entry.getValue()>=questStep.getQua())
              refresh=true;
          break;

        case 10://Ramener prisonnier
          if(player.getExchangeAction()!=null&&player.getExchangeAction().getType()==ExchangeAction.TALKING_WITH&&player.getCurMap().getNpc((Integer)player.getExchangeAction().getValue()).getTemplate().getId()==questStep.getNpc().getId())
          {
            GameObject follower=player.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR);
            if(follower!=null)
            {
              Map<Integer, Integer> itemNecessaryList=questStep.getItemNecessaryList();
              for(Entry<Integer, Integer> entry2 : itemNecessaryList.entrySet())
              {
                if(entry2.getKey()==follower.getTemplate().getId())
                {
                  refresh=true;
                  player.setMascotte(0);
                }
              }
            }
          }
          break;
        case 12://Remettre une âme de monstre
          NpcTemplate stepNpc=questStep.getNpc();
          if(stepNpc==null)
            break;
          if(player.getExchangeAction()!=null&&player.getExchangeAction().getType()==ExchangeAction.TALKING_WITH&&player.getCurMap().getNpc((Integer)player.getExchangeAction().getValue()).getTemplate().getId()==stepNpc.getId())
          {
            int monsterId=questStep.getMonsterId();
            int requiredSouls=questStep.getQua()>0 ? questStep.getQua() : 1;

            if(monsterId>0&&requiredSouls>0)
            {
              int soulsNeeded=requiredSouls;
              LinkedHashMap<GameObject, Integer> consumptionPlan=new LinkedHashMap<>();
              Map<Integer, GameObject> inventory=player.getItems();

              if(inventory!=null&&!inventory.isEmpty())
              {
                for(GameObject object : new ArrayList<>(inventory.values()))
                {
                  if(object==null)
                    continue;
                  if(object.getPosition()!=Constant.ITEM_POS_NO_EQUIPED)
                    continue;
                  if(object.getTemplate()==null||object.getTemplate().getType()!=Constant.ITEM_TYPE_PIERRE_AME_PLEINE)
                    continue;

                  int soulsInStone=countSoulsForMonster(object,monsterId);
                  if(soulsInStone<=0)
                    continue;

                  int soulsToConsume=Math.min(soulsNeeded,soulsInStone);
                  consumptionPlan.put(object,soulsToConsume);
                  soulsNeeded-=soulsToConsume;

                  if(soulsNeeded<=0)
                    break;
                }
              }

              if(soulsNeeded<=0&&!consumptionPlan.isEmpty())
              {
                boolean consumedAny=false;
                for(Map.Entry<GameObject, Integer> entry : consumptionPlan.entrySet())
                {
                  GameObject soulStone=entry.getKey();
                  int consumeCount=entry.getValue();

                  int removedFromCapture=consumeSoulsFromCapture(player,soulStone,monsterId,consumeCount);
                  if(removedFromCapture>0)
                    consumedAny=true;
                  if(removedFromCapture>=consumeCount)
                    continue;

                  int remainingToConsume=Math.max(0,consumeCount-removedFromCapture);
                  if(remainingToConsume<=0)
                    continue;

                  Map<Integer, Integer> soulStats=soulStone.getSoulStat();
                  if(soulStats==null||soulStats.isEmpty())
                    continue;

                  int current=soulStats.getOrDefault(monsterId,0);
                  int remaining=Math.max(0,current-remainingToConsume);

                  if(remaining<=0)
                  {
                    soulStats.remove(monsterId);
                  }
                  else
                  {
                    soulStats.put(monsterId,remaining);
                  }

                  if(soulStats.isEmpty())
                  {
                    player.removeItem(soulStone.getGuid(),soulStone.getQuantity(),true,true);
                  }
                  else
                  {
                    soulStone.setModification();
                    SocketManager.GAME_SEND_UPDATE_ITEM(player,soulStone);
                    Database.getDynamics().getObjectData().update(soulStone);
                  }
                  consumedAny=true;
                }
                if(consumedAny)
                  refresh=true;
              }
            }
          }
          break;
      }

      if(refresh)
      {
        QuestObjectif ansObjectif=QuestObjectif.getQuestObjectifById(getObjectifCurrent(questPlayer));
        questPlayer.setQuestStepValidate(questStep);
        SocketManager.GAME_SEND_Im_PACKET(player,"055;"+id);
        if(haveFinish(questPlayer,ansObjectif))
        {
          SocketManager.GAME_SEND_Im_PACKET(player,"056;"+id);
          applyButinOfQuest(player,ansObjectif);
          questPlayer.setFinish(true);
        }
        else
        {
          if(getNextObjectif(ansObjectif)!=0)
          {
            if(questPlayer.overQuestStep(ansObjectif))
              applyButinOfQuest(player,ansObjectif);
          }
        }
        //Database.getStatics().getPlayerData().update(player);
      }
    }
  }

  private boolean haveFinish(QuestPlayer questPlayer, QuestObjectif questObjectif)
  {
    return questPlayer.overQuestStep(questObjectif)&&getNextObjectif(questObjectif)==0;
  }

  private void applyButinOfQuest(Player player, QuestObjectif questObjectif)
  {
    long xp;
    int kamas;

    if((xp=questObjectif.getXp())>0)
    { //Xp a donner
      player.addXp(xp*((int)Config.getInstance().rateXp));
      SocketManager.GAME_SEND_Im_PACKET(player,"08;"+(xp*((int)Config.getInstance().rateXp)));
    }

    if(!questObjectif.getObjects().isEmpty())
    { //Item a donner
      for(Entry<Integer, Integer> entry : questObjectif.getObjects().entrySet())
      {
        ObjectTemplate template=Main.world.getObjTemplate(entry.getKey());
        int quantity=entry.getValue();
        GameObject object=template.createNewItem(quantity,false);
        if(player.addObjet(object,true))
        {
          World.addGameObject(object,true);
        }
        SocketManager.GAME_SEND_Im_PACKET(player,"021;"+quantity+"~"+template.getId());
      }

    }

    if((kamas=questObjectif.getKamas())>0)
    { //Kams a donner
      long newKamas=player.getKamas()+(long)kamas;
      if(newKamas<0)
        return;
      player.setKamas(player.getKamas()+(long)kamas);
      Main.world.kamas_total += kamas;
      SocketManager.GAME_SEND_Im_PACKET(player,"045;"+kamas);
    }

    if(getNextObjectif(questObjectif)!=questObjectif.getId())
    { //On passe au nouveau objectif on applique les actions
      for(Action action : questObjectif.getActions())
      {
        action.apply(player,null,0,0);
      }
    }

    SocketManager.GAME_SEND_STATS_PACKET(player);
  }
  // debut case 12
  private int countSoulsForMonster(GameObject object, int monsterId)
  {
    if(object==null||monsterId<=0)
      return 0;

    int total=0;

    Map<Integer, Integer> soulStats=object.getSoulStat();
    if(soulStats!=null&&!soulStats.isEmpty())
    {
      total+=Math.max(0,soulStats.getOrDefault(monsterId,0));
    }

    if(isCapture(object))
    {
      ArrayList<Pair<Integer, Integer>> monsters=getCaptureMonsters(object);
      if(monsters!=null&&!monsters.isEmpty())
      {
        for(Pair<Integer, Integer> storedMonster : monsters)
        {
          if(storedMonster!=null&&Objects.equals(storedMonster.getLeft(),monsterId))
            total++;
        }
      }
    }

    return total;
  }

  private int consumeSoulsFromCapture(Player player, GameObject soulStone, int monsterId, int consumeCount)
  {
    if(player==null||soulStone==null||monsterId<=0||consumeCount<=0)
      return 0;

    if(!isCapture(soulStone))
      return 0;

    ArrayList<Pair<Integer, Integer>> monsters=getCaptureMonsters(soulStone);
    if(monsters==null||monsters.isEmpty())
      return 0;

    int removed=0;
    for(Iterator<Pair<Integer, Integer>> iterator=monsters.iterator(); iterator.hasNext()&&removed<consumeCount;)
    {
      Pair<Integer, Integer> storedMonster=iterator.next();
      if(storedMonster!=null&&Objects.equals(storedMonster.getLeft(),monsterId))
      {
        iterator.remove();
        removed++;
      }
    }

    if(removed>0)
    {
      soulStone.setModification();
      if(monsters.isEmpty())
      {
        player.removeItem(soulStone.getGuid(),soulStone.getQuantity(),true,true);
      }
      else
      {
        SocketManager.GAME_SEND_UPDATE_ITEM(player,soulStone);
        Database.getDynamics().getObjectData().update(soulStone);
      }
    }

    return removed;
  }

  private boolean isCapture(GameObject object)
  {
    Class<?> captureClass=getCaptureClass();
    return captureClass!=null&&captureClass.isInstance(object);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<Pair<Integer, Integer>> getCaptureMonsters(GameObject object)
  {
    Field monstersField=getCaptureMonstersField();
    if(monstersField==null)
      return null;
    try
    {
      return (ArrayList<Pair<Integer, Integer>>)monstersField.get(object);
    }
    catch(IllegalAccessException e)
    {
      Main.world.logger.error("Impossible d'accéder aux monstres stockés dans la pierre d'âme",e);
      return null;
    }
  }

  private static Class<?> getCaptureClass()
  {
    if(captureClassCache!=null)
      return captureClassCache;
    try
    {
      captureClassCache=Class.forName(CAPTURE_CLASS_NAME);
    }
    catch(ClassNotFoundException e)
    {
      captureClassCache=null;
    }
    return captureClassCache;
  }

  private static Field getCaptureMonstersField()
  {
    if(captureFieldLookupDone)
      return captureMonstersField;
    captureFieldLookupDone=true;

    Class<?> captureClass=getCaptureClass();
    if(captureClass==null)
      return null;

    try
    {
      captureMonstersField=captureClass.getDeclaredField("monsters");
      captureMonstersField.setAccessible(true);
    }
    catch(NoSuchFieldException e)
    {
      Main.world.logger.error("Le champ 'monsters' est introuvable sur la classe Capture",e);
      captureMonstersField=null;
    }

    return captureMonstersField;
  }
  // Fin case 12
}
