package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.List;

import soufix.common.PathFinding;
import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.main.Config;

/**
 * IA orientée soutien : buff en priorité le lanceur, puis ses alliés, ensuite
 * les invocations alliées, avant de se mettre à l'abri. Aucun sort offensif
 * n'est volontairement lancé.
 */
public class IA205 extends AbstractNeedSpell
{
    public IA205(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
    }

    @Override
    public void apply()
    {
        if(!this.stop&&this.fighter.canPlay()&&this.count>0)
        {
            int time=100;
            boolean action=false;

            if(buffSelf())
            {
                time=400;
                action=true;
            }

            if(buffAllies(false))
            {
                time=400;
                action=true;
            }

            if(buffAllies(true))
            {
                time=400;
                action=true;
            }

            if(!action&&this.fighter.getCurPm(this.fight)>0)
            {
                Fighter nearest=getNearestAlly(false);
                if(nearest==null)
                    nearest=getNearestAlly(true);
                if(nearest!=null&&Function.getInstance().moveNearIfPossible(this.fight,this.fighter,nearest))
                {
                    time=600;
                    action=true;
                }
            }

            if(!action&&this.fighter.getCurPm(this.fight)>0)
            {
                int value=Function.getInstance().moveFarIfPossible(this.fight,this.fighter);
                if(value!=0)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPa(this.fight)==0&&this.fighter.getCurPm(this.fight)==0)
                this.stop=true;

            addNext(this::decrementCount,time+Config.getInstance().AIDelay);
        }
        else
        {
            this.stop=true;
        }
    }

    private boolean buffSelf()
    {
        boolean buffed=false;
        boolean progress;
        do
        {
            progress=false;
            if(this.fighter.getCurPa(this.fight)<=0)
                break;
            if(castBuffOnTarget(this.fighter))
            {
                buffed=true;
                progress=true;
            }
        }
        while(progress&&this.fighter.getCurPa(this.fight)>0);
        return buffed;
    }

    private boolean buffAllies(boolean invocation)
    {
        boolean buffed=false;
        boolean progress;
        do
        {
            progress=false;
            if(this.fighter.getCurPa(this.fight)<=0)
                break;
            for(Fighter candidate : getAlliesByDistance(invocation))
            {
                if(this.fighter.getCurPa(this.fight)<=0)
                    break;
                if(castBuffOnTarget(candidate))
                {
                    buffed=true;
                    progress=true;
                }
            }
        }
        while(progress&&this.fighter.getCurPa(this.fight)>0);
        return buffed;
    }

    private boolean castBuffOnTarget(Fighter target)
    {
        if(target==null)
            return false;
        int paBefore=this.fighter.getCurPa(this.fight);
        if(paBefore<=0)
            return false;
        Function.getInstance().buffIfPossible(this.fight,this.fighter,target,this.buffs);
        return this.fighter.getCurPa(this.fight)<paBefore;
    }

    private List<Fighter> getAlliesByDistance(boolean invocation)
    {
        List<Fighter> candidates=new ArrayList<>();
        for(Fighter target : this.fight.getFighters(3))
        {
            if(target==null||target.isDead()||target==this.fighter)
                continue;
            if(target.getTeam()!=this.fighter.getTeam())
                continue;
            if(invocation!=target.isInvocation())
                continue;
            candidates.add(target);
        }

        candidates.sort((first,second)->Integer.compare(getDistance(first),getDistance(second)));
        return candidates;
    }

    private Fighter getNearestAlly(boolean invocation)
    {
        List<Fighter> candidates=getAlliesByDistance(invocation);
        return candidates.isEmpty()?null:candidates.get(0);
    }

    private int getDistance(Fighter candidate)
    {
        if(this.fight.getMap()==null||this.fighter.getCell()==null||candidate==null||candidate.getCell()==null)
            return Integer.MAX_VALUE;
        return PathFinding.getDistanceBetween(this.fight.getMap(),this.fighter.getCell().getId(),candidate.getCell().getId());
    }
}
