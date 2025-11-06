package soufix.fight.ia.type;

import java.util.Collections;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;
import soufix.main.Config;

public class IA204 extends AbstractNeedSpell
{
    private final Spell.SortStats spell420;
    private int lastSpellTurn;

    public IA204(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
        this.spell420=initSpell420();
        this.lastSpellTurn=0;
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

            if(!usedAction&&canCastSpell420())
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,Collections.singletonList(this.spell420));
                if(value!=-1)
                {
                    time=Math.max(time,value);
                    usedAction=true;
                    this.lastSpellTurn=this.fighter.getTour();
                }
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

    private Spell.SortStats initSpell420()
    {
        if(this.fighter==null||this.fighter.getMob()==null)
            return null;
        try
        {
            return this.fighter.getMob().getSpells().get(420);
        }
        catch(Exception e)
        {
            return null;
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

    private boolean canCastSpell420()
    {
        if(this.spell420==null)
            return false;
        if(this.fight==null)
            return false;
        if(this.fighter==null)
            return false;
        if(this.fighter.getCurPa(this.fight)<=0)
            return false;
        if(this.fighter.getTour()<3)
            return false;
        return this.lastSpellTurn!=this.fighter.getTour();
    }
}
