package soufix.fight;

import soufix.client.Player;
import soufix.common.Formulas;
import soufix.common.PathFinding;
import soufix.common.SocketManager;
import soufix.fight.spells.Spell;
import soufix.fight.spells.SpellEffect;
import soufix.game.GameClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Challenge
{
  private int Type, xpWin, dropWin, Arg=0;
  private boolean challengeAlive=false, challengeWin=false;
  private String looseBy="", Args="", lastActions="";
  private Fight fight;
  private Fighter target;
  private List<Fighter> _ordreJeu=new ArrayList<>();
  private final List<Integer> orderedTargets=new ArrayList<>();
  private final Map<Integer,Integer> orderedTargetInitiatives=new HashMap<>();
  private int lastKilledMonsterId=-1;
  private static final String ARG_DELIMITER=";";

  public Challenge(Fight fight, int Type, int xp, int drop)
  {
    this.challengeAlive=true;
    this.fight=fight;
    this.Type=Type;
    this.xpWin=xp;
    this.dropWin=drop;
    this._ordreJeu.clear();
    this._ordreJeu.addAll(fight.getOrderPlaying());
  }

  public int getType()
  {
    return this.Type;
  }

  public boolean getAlive()
  {
    return challengeAlive;
  }

  public int getXp()
  {
    return xpWin;
  }

  public int getDrop()
  {
    return dropWin;
  }

  public boolean getWin()
  {
    return challengeWin;
  }

  public boolean loose()
  {
    return looseBy.isEmpty();
  }

  public String getPacketEndFight()
  {
    return (this.challengeWin ? "OK"+Type : "KO"+Type);
  }

  private void challengeWin()
  {
    challengeWin=true;
    challengeAlive=false;
    SocketManager.GAME_SEND_CHALLENGE_FIGHT(fight,1,"OK"+Type);
  }

  public void challengeLoose(Fighter fighter)
  {
    String name="";
    if(fighter!=null&&fighter.getPersonnage()!=null)
      name=fighter.getPersonnage().getName();
    looseBy=name;
    challengeWin=false;
    challengeAlive=false;
    SocketManager.GAME_SEND_CHALLENGE_FIGHT(fight,7,"KO"+Type);
    SocketManager.GAME_SEND_Im_PACKET_TO_CHALLENGE(fight,1,"0188;"+name);
  }

  public void challengeSpecLoose(Player player)
  {
    SocketManager.GAME_SEND_CHALLENGE_PERSO(player,"KO"+Type);
    SocketManager.GAME_SEND_Im_PACKET_TO_CHALLENGE_PERSO(player,"0188;"+looseBy);
  }

  public String parseToPacket()
  {
    StringBuilder packet=new StringBuilder();
    packet.append(Type).append(";").append(target!=null ? "1" : "0").append(";").append(target!=null ? (Integer.valueOf(target.getId())) : "").append(";").append(xpWin).append(";0;").append(dropWin).append(";0;");
    if(!challengeAlive)
    {
      if(challengeWin)
        packet.append("").append(Type);
      else
        packet.append("").append(Type);
    }
    return packet.toString();
  }

  public void showCibleToPerso(Player p)
  {
    if(!challengeAlive||target==null||target.getCell()==null||p==null)
      return;
    ArrayList<GameClient> Pws=new ArrayList<>();
    Pws.add(p.getGameClient());
    SocketManager.GAME_SEND_FIGHT_SHOW_CASE(Pws,target.getId(),target.getCell().getId());
  }

  public void showCibleToFight()
  {
    if(!challengeAlive||target==null||target.getCell()==null)
      return;
    ArrayList<GameClient> Pws=new ArrayList<>();
    for(Fighter fighter : fight.getFighters(1))
    {
      if(fighter.hasLeft())
        continue;
      if(fighter.getPersonnage()==null||!fighter.getPersonnage().isOnline())
        continue;
      Pws.add(fighter.getPersonnage().getGameClient());
    }
    SocketManager.GAME_SEND_FIGHT_SHOW_CASE(Pws,target.getId(),target.getCell().getId());
  }

  private String buildDelimitedArg(int id)
  {
    return ARG_DELIMITER+id+ARG_DELIMITER;
  }

  private boolean argsContainsId(int id)
  {
    return !Args.isEmpty()&&Args.contains(buildDelimitedArg(id));
  }

  private void addIdToArgs(int id)
  {
    String token=buildDelimitedArg(id);
    if(!Args.contains(token))
      Args+=token;
  }

  public void fightStart()
  {//Définit les cibles au début du combat
    if(!challengeAlive)
      return;

    switch(Type)
    {
      case 3://Désigné Volontaire
      case 4://Sursis
      case 32://Elitiste
      case 35://Tueur é gages
        if(target==null)
          target=getRandomMonsterTarget();
        if(Type==4)
          lastKilledMonsterId=-1;
        showCibleToFight();//On le montre a tous les joueurs
        break;
      case 10://Cruel
        initializeOrderedChallenge(Comparator.comparingInt(Fighter::getInitiative).reversed());
        break;
      case 25://Ordonné
        initializeOrderedChallenge(Comparator.comparingInt(Fighter::getInitiative));
        break;
    }
  }

  private void initializeOrderedChallenge(Comparator<Fighter> ordering)
  {
    orderedTargets.clear();
    orderedTargetInitiatives.clear();
    target=null;

    ArrayList<Fighter> monsters=new ArrayList<>(fight.getFighters(2));
    monsters.removeIf(fighter -> fighter==null||fighter.isInvocation()||fighter.isDouble()||fighter.isDead()||fighter.getPersonnage()!=null);

    if(monsters.isEmpty())
      return;

    if(ordering!=null)
      monsters.sort(ordering);
    else
      Collections.shuffle(monsters);

    for(Fighter fighter : monsters)
    {
      orderedTargets.add(fighter.getId());
      orderedTargetInitiatives.put(fighter.getId(),fighter.getInitiative());
    }

    refreshOrderedTarget();
  }

  public Fighter getCurrentOrderedTarget()
  {
    if(!challengeAlive)
      return null;
    switch(Type)
    {
      case 10://Cruel
      case 25://Ordonné
        return target;
    }
    return null;
  }

  private boolean validateOrderedKill(Fighter mob)
  {
    if(mob==null||mob.getPersonnage()!=null||mob.isInvocation()||mob.isDouble())
      return true;
    pruneOrderedTargets(mob.getId());
    if(orderedTargets.isEmpty())
      return true;

    int expectedInitiative=getOrderedTargetInitiative(orderedTargets.get(0));
    int killerInitiative=getOrderedTargetInitiative(mob.getId());
    if(expectedInitiative==Integer.MIN_VALUE||killerInitiative==Integer.MIN_VALUE)
      return true;

    if(killerInitiative!=expectedInitiative)
      return false;

    orderedTargets.remove((Integer)mob.getId());
    orderedTargetInitiatives.remove(mob.getId());
    return true;
  }

  private void refreshOrderedTarget()
  {
    if(!challengeAlive)
      return;
    target=null;
    pruneOrderedTargets(-1);
    if(orderedTargets.isEmpty())
      return;
    Fighter nextTarget=getFighterById(orderedTargets.get(0));
    if(nextTarget==null||nextTarget.isDead()||nextTarget.hasLeft())
      return;
    target=nextTarget;
    showCibleToFight();
  }

  private void pruneOrderedTargets(int protectedId)
  {
    Iterator<Integer> iterator=orderedTargets.iterator();
    while(iterator.hasNext())
    {
      int fighterId=iterator.next();
      if(fighterId==protectedId)
        continue;
      Fighter fighter=getFighterById(fighterId);
      if(fighter==null||fighter.isDead()||fighter.hasLeft())
      {
        iterator.remove();
        orderedTargetInitiatives.remove(fighterId);
      }
    }
  }

  private int getOrderedTargetInitiative(int fighterId)
  {
    Integer value=orderedTargetInitiatives.get(fighterId);
    if(value!=null)
      return value;
    Fighter fighter=getFighterById(fighterId);
    if(fighter==null)
      return Integer.MIN_VALUE;
    return fighter.getInitiative();
  }

  private Fighter getRandomMonsterTarget()
  {
    ArrayList<Fighter> choices=new ArrayList<>();
    choices.addAll(fight.getFighters(2));
    choices.removeIf(fighter -> fighter==null||fighter.getPersonnage()!=null||fighter.isDead()||fighter.isInvocation()||fighter.isDouble());
    if(choices.isEmpty())
      return null;
    Collections.shuffle(choices);
    return choices.get(0);
  }

  private Fighter getFighterById(int fighterId)
  {
    for(Fighter fighter : fight.getFighters(3))
      if(fighter!=null&&fighter.getId()==fighterId)
        return fighter;
    return null;
  }

  private boolean hasOtherLivingMonsters(int excludedId)
  {
    for(Fighter fighter : fight.getTeam1().values())
    {
      if(fighter==null||fighter.isInvocation()||fighter.isDouble())
        continue;
      if(fighter.isDead()||fighter.hasLeft())
        continue;
      if(fighter.getId()!=excludedId)
        return true;
    }
    return false;
  }

  public void fightEnd()
  {//Vérifie la validité des challenges en fin de combat (si nécessaire)
    if(!challengeAlive)
      return;
    switch(Type)
    {
      case 4://Sursis
        if(target==null)
          break;
        if(lastKilledMonsterId!=target.getId())
        {
          challengeLoose(fight.getFighterByOrdreJeu());
          return;
        }
        break;
      case 44://Partage
      case 46://Chacun son monstre
        for(Fighter fighter : fight.getFighters(1))
        {
          if(fighter.isInvocation())
            continue;
          if(!argsContainsId(fighter.getId()))
          {
            challengeLoose(fighter);
            return;
          }
        }
        break;
    }
    challengeWin();
  }

  public void onFighterDie(Fighter fighter)
  {
    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
	 return;
    switch(Type)
    {
      case 33: // survivant
      case 49: // Protégez vos mules
        if(fighter.getPersonnage()!=null)
          challengeLoose(fight.getFighterByOrdreJeu());
        break;
      case 44://Partage
        if(fighter.getPersonnage()!=null)
          if(!argsContainsId(fighter.getId()))
            challengeLoose(fighter);
        break;
    }
  }

  public void onFighterAttacked(Fighter caster, Fighter target)
  {
    if(!challengeAlive)
      return;
    if(caster.isInvocation() || caster.isDouble())
   	 return;
    switch(Type)
    {
      case 17:// Intouchable
        if(target.getTeam()==0&&!target.isInvocation())
        {
          if(target.getBuff(9)==null) // Si dérobade
            challengeLoose(target);
        }
        break;
      case 31: // Focus
        if(caster.getTeam()==0&&target.getTeam()==1&&!caster.isInvocation())
        {
          if(Args.isEmpty())
            addIdToArgs(target.getId());
          else if(!argsContainsId(target.getId()))
            challengeLoose(caster);
        }
        break;
      case 47: // Contamination
        if(target.getTeam()==0&&!target.isInvocation()&&!Args.contains(";"+target.getId()+","))
          Args+=";"+target.getId()+","+"3;";
        break;
    }
  }

  //v2.8 - Fixed summons losing a bunch of challs
  public void onFightersAttacked(ArrayList<Fighter> targets, Fighter caster, SpellEffect SE, int spell, boolean isTrap)
  {
    int effectID=SE.getEffectID();
    if(!challengeAlive)
      return;
    if(caster.isInvocation() || caster.isDouble())
   	 return;
    String DamagingEffects="|82|85|86|87|88|89|91|92|93|94|95|96|97|98|99|100|141|671|672|1014|1015|";
    String HealingEffects="|108|";
    String MPEffects="|77|127|169|";
    String APEffects="|84|101|";
    String OPEffects="|116|320|";
    switch(Type)
    {
      case 18: // Incurable
        if(caster.getTeam()==0&&!caster.isInvocation()&&HealingEffects.contains("|"+effectID+"|"))
          targets.stream().filter(fighter -> fighter.getTeam()==0).forEach(fighter -> challengeLoose(caster));
        break;
      case 19: // Mains propres
        if(caster.getTeam()!=0)
          return;
        if(caster.isInvocation())
          return;
        if(SE.getTurn()>0)
          return;
        if(isTrap)
          return;

        for(Fighter target : targets)
        {
          if(target.getTeam()==1&&!target.isInvocation())
          {
            if(DamagingEffects.contains("|"+effectID+"|"))
              challengeLoose(caster);
            break;
          }
        }

        break;
      case 20: // Elémentaire
        if(caster.getTeam()==0&&!caster.isInvocation()&&DamagingEffects.contains("|"+effectID+"|")&&effectID!=141)
        {
          switch(spell)
          {
            case 126://Mot stimulant
            case 149://Mutilation
            case 106://Roue de la fortune
            case 111://Contrecoup
            case 108://Esprit félin
            case 435://Transfert de vie
            case 135://Mot de sacrifice
            case 123://Mot drainant
              return;
          }
          if(Arg==0)
          {
            Arg=effectID;
            break;
          }
          //Furie
          if(spell == 447)
          if(effectID == 89)
          break;
          //Furie
          
          if(Arg!=effectID)
          {
            String eau="85 91 96 1014",terre="86 92 97 1015",air="87 93 98",feu="88 94 99",neutre="89 95 100";
            if(eau.contains(String.valueOf(Arg))&&eau.contains(String.valueOf(effectID)))
            {
              break;
            }
            else if(terre.contains(String.valueOf(Arg))&&terre.contains(String.valueOf(effectID)))
            {
              break;
            }
            else if(air.contains(String.valueOf(Arg))&&air.contains(String.valueOf(effectID)))
            {
              break;
            }
            else if(feu.contains(String.valueOf(Arg))&&feu.contains(String.valueOf(effectID)))
            {
              break;
            }
            else if(neutre.contains(String.valueOf(Arg))&&neutre.contains(String.valueOf(effectID)))
            {
              break;
            }
            challengeLoose(caster);
            break;
          }
        }
        break;
      case 21: // Circulez !
        if(caster.getTeam()==0&&!caster.isInvocation()&&MPEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(target.getTeam()==1)
            {
              challengeLoose(caster);
              break;
            }
          }
        }
        break;
      case 22: // Le temps qui court !
        if(caster.getTeam()==0&&!caster.isInvocation()&&APEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(target.getTeam()==1)
            {
              challengeLoose(caster);
              break;
            }
          }
        }
        break;
      case 23: // Perdu de vue !
        if(caster.getTeam()==0&&!caster.isInvocation()&&OPEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(target.getTeam()==1)
            {
              challengeLoose(caster);
              break;
            }
          }
        }
        break;
      case 32: // Elitiste
      case 34: // Imprévisible
        if(caster.getTeam()==0&&DamagingEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(this.target!=null)
            {
              if(target.getTeam()==1)
              {
                if(this.target.getId()!=target.getId()&&!target.isInvocation())
                  challengeLoose(caster);
              }
            }
          }
        }
        break;
      case 38: // Blitzkrieg
        if(caster.getTeam()==0&&!caster.isInvocation()&&DamagingEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(target.getTeam()==1&&!target.isInvocation())
            {
              StringBuilder id=new StringBuilder();
              id.append(";").append(target.getId()).append(",");
              if(!this.Args.contains(id.toString()))
              {
                id.append(caster.getId());
                this.Args+=id.toString();
              }
            }
          }
        }
        break;
      case 43: // Abnégation
        if(caster.getTeam()==0&&!caster.isInvocation()&&HealingEffects.contains("|"+effectID+"|"))
          for(Fighter target : targets)
            if(target.getId()==caster.getId())
              challengeLoose(caster);
        break;
      case 45: // Duel
      case 46: // Chacun son monstre
        if(caster.getTeam()==0&&!caster.isInvocation()&&DamagingEffects.contains("|"+effectID+"|"))
        {
          for(Fighter target : targets)
          {
            if(target.getTeam()==1&&!caster.isInvocation())
            {
              if(!Args.contains(";"+target.getId()+","))
                Args+=";"+target.getId()+","+caster.getId()+";";
              else if(Args.contains(";"+target.getId()+",")&&!Args.contains(";"+target.getId()+","+caster.getId()+";"))
                challengeLoose(target);
            }
          }
        }
        break;
    }
  }

  //v2.8 - Random Contract Killer target
  public void onMobDie(Fighter mob, Fighter killer)
  {
	  if(killer.isInvocation() || killer.isDouble())
		   	 return;
    if(mob.getMob()==null||mob.getPersonnage()!=null||mob.getTeam()!=1)
      return;
    if(mob.isInvocation()&&mob.getInvocator().getPersonnage()!=null)
      return;
    boolean isKiller=(killer.getId()!=mob.getId());

    if(!challengeAlive)
      return;

    switch(Type)
    {
      case 3: // Désigné Volontaire
        if(target==null)
          return;
        if(mob.isInvocation())
          return;

        if(mob.getInvocator()!=null)
          if(mob.getInvocator().getId()==target.getId())
            return;

        if(target.getId()!=mob.getId())
        {
          challengeLoose(fight.getFighterByOrdreJeu());
        }
        else
        {
          challengeWin();
        }
        target=null;
        break;

      case 4: // Sursis
        if(mob.isInvocation()||mob.isDouble())
          return;
        lastKilledMonsterId=mob.getId();
        if(target==null)
          return;

        if(target.getId()==mob.getId()&&hasOtherLivingMonsters(mob.getId()))
        {
          challengeLoose(fight.getFighterByOrdreJeu());
        }
        break;

      case 28: // Ni Pioutes ni Soumises
        if(isKiller&&killer.getPersonnage()!=null)
          if(killer.getPersonnage().getSexe()==0)
          {
            challengeLoose(fight.getFighterByOrdreJeu());
          }
        break;

      case 29: // Ni Pious ni Soumis
        if(isKiller&&killer.getPersonnage()!=null)
        {
          if(killer.getPersonnage().getSexe()==1)
          {
        	  if(!mob.isInvocation())
            challengeLoose(fight.getFighterByOrdreJeu());
          }
        }
        break;

      case 31: // Focus
        if(mob.getLevelUp())
          break;
        if(argsContainsId(mob.getId()))
          Args="";
        else if(!mob.isInvocation())
          challengeLoose(killer);
        break;

      case 32: // Elitiste
        if(target.getId()==mob.getId())
          challengeWin();
        break;

      case 34: // Imprévisible
        target=null;
        break;
      case 42: // Deux pour le prix d'un
        if(mob.isInvocation()||killer.isInvocation())
          return;
        Args+=(Args.isEmpty() ? killer.getId() : ";"+killer.getId());
        break;
      case 44: // Partage
      case 46: // Chacun son monstre
        if(isKiller&&killer!=null&&!mob.isInvocation())
          addIdToArgs(killer.getId());
        break;
      case 30: // Les petits d'abord
      case 48: // Les mules d'abord
        if(mob.isInvocation()||mob.isDouble())
          return;
        if(mob.getId()!=killer.getId())
        {
          int lvlMin=5000;
          for(Fighter f : fight.getFighters2(1))
          {
            if(f.isInvocation())
              continue;
            if(f.getLvl()<lvlMin)
              lvlMin=f.getLvl();
          }
          if(killer.getLvl()>lvlMin)
            challengeLoose(fight.getFighterByOrdreJeu());
        }
        break;

      case 35: //Contract killer
        if(target==null)
          return;
        if(target.getId()!=mob.getId()&&killer.getPersonnage()!=null) //wrong target killed
        {
          if(!mob.isInvocation())
            challengeLoose(fight.getFighterByOrdreJeu());
        }
        else
        {
          try
          {
            target=getRandomMonsterTarget();
            showCibleToFight();
          }
          catch(Exception e)
          {
            e.printStackTrace();
          }
        }
        break;

      case 10: // Cruel
        if(mob.getPersonnage()!=null||mob.isInvocation()||mob.isDouble())
          return;
        if(!validateOrderedKill(mob))
        {
          challengeLoose(fight.getFighterByOrdreJeu());
          return;
        }
        refreshOrderedTarget();
        break;

      case 25: // Ordonné
        if(mob.getPersonnage()!=null||mob.isInvocation()||mob.isDouble())
          return;
        if(!validateOrderedKill(mob))
        {
          challengeLoose(fight.getFighterByOrdreJeu());
          return;
        }
        refreshOrderedTarget();
        break;
    }
  }

  public void onPlayerMove(Fighter fighter)
  {
    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
      	 return;
    switch(Type)
    {
      case 1: // Zombie
        if(this.fight.getCurFighterUsedPm()>1) // Si l'on a utilisé plus d'un PM
          challengeLoose(fight.getFighterByOrdreJeu());
        break;
    }
  }

  public void onPlayerAction(Fighter fighter, int actionID)
  {
	  if(fighter.isInvocation() || fighter.isDouble())
	      	 return;
    if(!challengeAlive||fighter.getTeam()==1)
      return;
    StringBuilder action=new StringBuilder();
    action.append(";").append(fighter.getId());
    action.append(",").append(actionID).append(";");
    switch(Type)
    {
      case 6: // Versatile
      case 5: // Econome
        if(lastActions.contains(action.toString()))
          challengeLoose(fight.getFighterByOrdreJeu());
        lastActions+=action.toString();
        break;

      case 24: // Borné
        if(!lastActions.contains(action.toString())&&lastActions.contains(";"+fighter.getId()+","))
          challengeLoose(fight.getFighterByOrdreJeu());
        lastActions+=action.toString();
        break;
    }

  }

  public void onPlayerCac(Fighter fighter)
  {

    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
     	 return;
    switch(Type)
    {
      case 11: // Mystique
        challengeLoose(fight.getFighterByOrdreJeu());
        break;
      case 6: // Versatile
      case 5: // Econome
        StringBuilder action=new StringBuilder();
        action.append(";").append(fighter.getId());
        action.append(",").append("cac").append(";");
        if(lastActions.contains(action.toString()))
          challengeLoose(fight.getFighterByOrdreJeu());
        lastActions+=action.toString();
        break;
    }
  }

  public void onPlayerSpell(Fighter fighter, Spell.SortStats spellStats)
  {
    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
     	 return;
    if(fighter.getPersonnage()==null)
      return;
    switch(Type)
    {
      case 9: // Barbare
        challengeLoose(fight.getFighterByOrdreJeu());
        break;
      case 14: // Casino Royal (sort #101)
        if(fighter.getPersonnage()!=null)
          if(spellStats.getSpellID()==101)
            Args="cast";
        break;
    }
  }

  public void onPlayerStartTurn(Fighter fighter)
  {
    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
     	 return;
    switch(Type)
    {
      case 2: // Statue
        if(fighter.getPersonnage()==null)
          return;
        Arg=fighter.getCell().getId();
        break;
      case 6: // Versatile
        lastActions="";
        break;
      case 14: // Casino Royal (sort #101)
        if(fighter.getPersonnage()!=null)
          if(fighter.canLaunchSpell(101))
            Args="ok";
          else
            Args="cant";
        break;
      case 34: // Imprévisible
        if(fighter.getTeam()==1)
          return;
        try
        {
          int noBoucle=0,GUID=0;
          target=null;
          while(target==null)
          {
            if(_ordreJeu.size()>0)
            {
              GUID=Formulas.getRandomValue(0,_ordreJeu.size()-1);
              Fighter f=_ordreJeu.get(GUID);
              if(f.getPersonnage()==null&&!f.isDead())
                target=f;
              noBoucle++;
              if(noBoucle>150)
                return;
            }
          }
          showCibleToFight();
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
        break;
      case 38: // Blitzkrieg
        if(fighter.getTeam()==1&&Args.contains(";"+fighter.getId()+","))
        {
          if(fighter.isDead())
            return;

          int id=0;

          for(String string : this.Args.split(";"))
          {
            if(string.contains(""+fighter.getId()))
            {
              for(String test : string.split(","))
                id=Integer.parseInt(test);
              break;
            }
          }

          for(Fighter target : this.fight.getFighters(1))
            if(target.getId()==id)
              if(fighter.getPdv()!=fighter.getPdvMax())
                challengeLoose(target);
        }
        break;
      case 47: // Contamination
        if(fighter.getTeam()==0)
        {
          String str=";"+fighter.getId()+",";
          if(Args.contains(str+"1;"))
            challengeLoose(fighter);
          else if(Args.contains(str+"2;"))
            Args+=str+"1;";
          else if(Args.contains(str+"3;"))
            Args+=str+"2;";
        }
        break;
    }
  }

  public void onPlayerEndTurn(Fighter fighter)
  {
    if(!challengeAlive)
      return;
    if(fighter.isInvocation() || fighter.isDouble())
     	 return;
    boolean hasFailed=false;
    ArrayList<Fighter> fighters=PathFinding.getFightersAround(fighter.getCell().getId(),fight.getMap(),fight);

    switch(Type)
    {
      case 1: // Zombie
        if(this.fight.getCurFighterUsedPm()<=0) // Si l'on a pas bougé
          challengeLoose(fighter);
        break;

      case 2: // Statue
        if(fighter.getPersonnage()!=null)
          if(fighter.getCell().getId()!=Arg)
            challengeLoose(fighter);
        break;

      case 7: // Jardinier (sort #367)
        if(fighter.getPersonnage()!=null)
          if(fighter.canLaunchSpell(367))
            challengeLoose(fighter);
        break;

      case 8: // Nomade
        if(this.fight.getCurFighterPm()>0)
          challengeLoose(fighter);
        break;

      case 12: // Fossoyeur (sort #373)
        if(fighter.getPersonnage()!=null)
          if(fighter.canLaunchSpell(373))
            challengeLoose(fighter);
        break;

      case 14: // Casino Royal (sort #101)
        if(fighter.getPersonnage()!=null)
          if(Args.equals("ok"))
            challengeLoose(fighter);
        break;

      case 15: // Araknophile (sort #370)
        if(fighter.getPersonnage()!=null)
          if(fighter.canLaunchSpell(370))
            challengeLoose(fighter);
        break;

      case 36: // Hardi
        hasFailed=true;
        if(!fighters.isEmpty())
          for(Fighter f : fighters)
            if(f.getTeam()!=fighter.getTeam())
              hasFailed=false;
        break;

      case 37: // Collant
        hasFailed=true;
        if(!fighters.isEmpty())
          for(Fighter f : fighters)
            if(f.getTeam()==fighter.getTeam())
              hasFailed=false;
        break;

      case 39: // Anachoréte
        if(!fighters.isEmpty())
          fighters.stream().filter(f -> f.getTeam()==fighter.getTeam()).forEach(f -> challengeLoose(fighter));
        break;

      case 40: // Pusillanime
        if(!fighters.isEmpty())
          fighters.stream().filter(f -> f.getTeam()!=fighter.getTeam()).forEach(f -> challengeLoose(fighter));
        break;

      case 41: // Pétulant
        if(this.fight.getCurFighterPa()>0)
          challengeLoose(fighter);
        break;

      case 42: // Deux pour le prix d'un
        if(!Args.isEmpty())
          if(!(Args.split(";").length%2==0))
            hasFailed=true;
        Args="";
        break;
    }
    if(hasFailed)
      challengeLoose(fighter);
  }
}
