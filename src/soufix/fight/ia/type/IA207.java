package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;
import soufix.fight.spells.Spell.SortStats;
import soufix.fight.spells.SpellEffect;

/**
 * IA spécialisée dans l'utilisation des sorts de piège :
 * <ul>
 *   <li>tente de poser un piège avant toute autre action offensive ;</li>
 *   <li>se replace pour trouver une case valable s'il n'en existe pas immédiatement ;</li>
 *   <li>utilise ensuite ses sorts classiques ou sa fuite en dernier recours.</li>
 * </ul>
 */
public class IA207 extends AbstractNeedSpell
{
    public IA207(Fight fight, Fighter fighter, byte count)
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

            Fighter target=Function.getInstance().getNearestEnnemy(this.fight,this.fighter);
            List<SortStats> trapSpells=getTrapSpells();

            if(this.fighter.getCurPa(this.fight)>0&&!action&&!trapSpells.isEmpty())
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,trapSpells);
                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPa(this.fight)>0&&!action)
            {
                if(Function.getInstance().buffIfPossible(this.fight,this.fighter,this.fighter,this.buffs))
                {
                    time=400;
                    action=true;
                }
            }

            if(this.fighter.getCurPm(this.fight)>0&&!action&&target!=null)
            {
                int value=Function.getInstance().moveautourIfPossible(this.fight,this.fighter,target);
                if(value==0)
                    value=Function.getInstance().moveNearIfPossible(this.fight,this.fighter,target);
                if(value!=0)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPa(this.fight)>0&&!action&&!trapSpells.isEmpty())
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,trapSpells);
                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPa(this.fight)>0&&!action)
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.highests);
                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPa(this.fight)>0&&!action)
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.cacs);
                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            if(this.fighter.getCurPm(this.fight)>0&&!action)
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

            addNext(this::decrementCount,time);
        }
        else
        {
            this.stop=true;
        }
    }

    private List<SortStats> getTrapSpells()
    {
        Set<SortStats> traps=new LinkedHashSet<>();
        collectTrapSpells(this.highests,traps);
        collectTrapSpells(this.glyphs,traps);
        if(traps.isEmpty()&&this.fighter.getMob()!=null)
            collectTrapSpells(this.fighter.getMob().getSpells().values(),traps);
        return new ArrayList<>(traps);
    }

    private void collectTrapSpells(Collection<SortStats> source, Set<SortStats> traps)
    {
        if(source==null||traps==null)
            return;
        for(SortStats spell : source)
            if(isTrapSpell(spell))
                traps.add(spell);
    }

    private boolean isTrapSpell(SortStats spell)
    {
        if(spell==null)
            return false;
        if(hasTrapEffect(spell.getEffects())||hasTrapEffect(spell.getCCeffects()))
            return true;
        Spell base=spell.getSpell();
        if(base==null)
            return false;
        for(Map.Entry<Integer, SortStats> entry : base.getSortsStats().entrySet())
        {
            SortStats level=entry.getValue();
            if(level!=null&&hasTrapEffect(level.getEffects()))
                return true;
        }
        return false;
    }

    private boolean hasTrapEffect(List<SpellEffect> effects)
    {
        if(effects==null)
            return false;
        for(SpellEffect effect : effects)
            if(effect!=null&&effect.getEffectID()==400)
                return true;
        return false;
    }
}
