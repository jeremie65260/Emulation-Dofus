package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soufix.area.map.GameCase;
import soufix.common.PathFinding;
import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;
import soufix.fight.spells.SpellEffect;
import soufix.main.Config;
import soufix.utility.Pair;

public class IA204 extends AbstractNeedSpell
{
    private final List<Spell.SortStats> resurrections = new ArrayList<>();

    public IA204(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
        initResurrectionSpells();
    }

    @Override
    public void apply()
    {
        if(!this.stop&&this.fighter.canPlay()&&this.count>0)
        {
            int time=100;
            boolean usedAction=false;
            boolean moved=false;

            Fighter ennemy=Function.getInstance().getNearestEnnemy(this.fight,this.fighter);
            int maxPo=getMaxRange();

            Fighter targetDistance=getRangedTarget(maxPo);
            Fighter targetMelee=getMeleeTarget();

            if(!usedAction&&this.fighter.getCurPa(this.fight)>0&&tryResurrection())
            {
                time=600;
                usedAction=true;
            }

            if(this.fighter.getCurPm(this.fight)>0&&!usedAction&&targetDistance==null&&targetMelee==null)
            {
                int value=Function.getInstance().moveautourIfPossible(this.fight,this.fighter,ennemy);
                if(value!=0)
                {
                    time=Math.max(time,value);
                    moved=true;
                    targetDistance=getRangedTarget(maxPo);
                    targetMelee=getMeleeTarget();
                }
            }

            if(!usedAction&&this.fighter.getCurPa(this.fight)>0&&targetDistance!=null)
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.highests);
                if(value!=-1)
                {
                    time=Math.max(time,value);
                    usedAction=true;
                }
            }

            if(!usedAction&&this.fighter.getCurPa(this.fight)>0&&targetMelee!=null)
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.cacs);
                if(value==-1)
                    value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.highests);
                if(value!=-1)
                {
                    time=Math.max(time,value);
                    usedAction=true;
                }
            }

            if(this.fighter.getCurPm(this.fight)>0&&!moved)
            {
                int value=Function.getInstance().moveautourIfPossible(this.fight,this.fighter,ennemy);
                if(value!=0)
                {
                    time=Math.max(time,value);
                    moved=true;
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

    private void initResurrectionSpells()
    {
        try
        {
            if(this.fighter==null||this.fighter.getMob()==null)
                return;
            Map<Integer, Spell.SortStats> spells=this.fighter.getMob().getSpells();
            for(Spell.SortStats stats : spells.values())
            {
                if(stats==null)
                    continue;
                for(SpellEffect effect : stats.getEffects())
                {
                    if(effect!=null&&effect.getEffectID()==780)
                    {
                        this.resurrections.add(stats);
                        break;
                    }
                }
            }
        }
        catch(Exception e)
        {
            // ignore
        }
    }

    private Fighter getLastDeadAlly()
    {
        if(this.fight==null)
            return null;
        List<Pair<Integer, Fighter>> dead=this.fight.getDeadList();
        for(int i=dead.size()-1;i>=0;i--)
        {
            Pair<Integer, Fighter> entry=dead.get(i);
            Fighter candidate=entry==null?null:entry.getRight();
            if(candidate==null)
                continue;
            if(candidate.hasLeft())
                continue;
            if(!candidate.isDead())
                continue;
            if(candidate.getTeam()!=this.fighter.getTeam())
                continue;
            return candidate;
        }
        return null;
    }

    private boolean tryResurrection()
    {
        if(this.resurrections.isEmpty())
            return false;
        Fighter dead=getLastDeadAlly();
        if(dead==null)
            return false;

        Set<Integer> cells=new LinkedHashSet<>();
        addDeadCell(cells,dead);
        addAroundCell(cells,dead.getCell());
        addAroundCell(cells,this.fighter.getCell());
        Fighter ennemy=Function.getInstance().getNearestEnnemy(this.fight,this.fighter);
        if(ennemy!=null)
            addAroundCell(cells,ennemy.getCell());

        for(Spell.SortStats spell : this.resurrections)
        {
            if(spell==null)
                continue;
            for(Integer cellId : cells)
            {
                if(cellId==null||cellId<=0)
                    continue;
                if(this.fight.tryCastSpell(this.fighter,spell,cellId)==0)
                    return true;
            }
        }
        return false;
    }

    private void addDeadCell(Set<Integer> cells, Fighter dead)
    {
        if(dead==null)
            return;
        GameCase cell=dead.getCell();
        if(cell==null)
            return;
        if(!cell.getFighters().isEmpty())
            return;
        if(!cell.isWalkable(false))
            return;
        cells.add(cell.getId());
    }

    private void addAroundCell(Set<Integer> cells, GameCase base)
    {
        if(base==null)
            return;
        List<Integer> used=new ArrayList<>();
        for(int i=0;i<4;i++)
        {
            int cellId=PathFinding.getAvailableCellArround(this.fight,base.getId(),used);
            if(cellId<=0)
                break;
            used.add(cellId);
            cells.add(cellId);
        }
    }

    private int getMaxRange()
    {
        int maxPo=1;
        for(Spell.SortStats spellStats : this.highests)
        {
            if(spellStats!=null&&spellStats.getMaxPO()>maxPo)
                maxPo=spellStats.getMaxPO();
        }
        return maxPo;
    }

    private Fighter getRangedTarget(int maxPo)
    {
        if(maxPo<=1)
            return null;
        Fighter target=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,1,maxPo+1);
        if(target!=null&&target.isHide())
            return null;
        return target;
    }

    private Fighter getMeleeTarget()
    {
        Fighter target=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,0,2);
        if(target!=null&&target.isHide())
            return null;
        return target;
    }
}
