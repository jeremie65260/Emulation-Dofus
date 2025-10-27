package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.spells.Spell.SortStats;
import soufix.fight.spells.SpellEffect;
import soufix.utility.Pair;

public class IA204 extends IA203
{
    private static final int RESURRECTION_EFFECT_ID=780;
    private final List<SortStats> baseBuffs;
    private final List<SortStats> baseHighests;
    private final List<SortStats> baseCacs;
    private final List<SortStats> baseInvocations;

    public IA204(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
        this.baseBuffs=copyList(this.buffs);
        this.baseHighests=copyList(this.highests);
        this.baseCacs=copyList(this.cacs);
        this.baseInvocations=copyList(this.invocations);
    }

    @Override
    public void apply()
    {
        List<SortStats> originalBuffs=this.buffs;
        List<SortStats> originalHighests=this.highests;
        List<SortStats> originalCacs=this.cacs;
        List<SortStats> originalInvocations=this.invocations;
        try
        {
            boolean resurrectAllowed=hasDeadAlly();
            this.buffs=selectListForCurrentState(baseBuffs,resurrectAllowed);
            this.highests=selectListForCurrentState(baseHighests,resurrectAllowed);
            this.cacs=selectListForCurrentState(baseCacs,resurrectAllowed);
            this.invocations=selectListForCurrentState(baseInvocations,resurrectAllowed);
            super.apply();
        }
        finally
        {
            this.buffs=originalBuffs;
            this.highests=originalHighests;
            this.cacs=originalCacs;
            this.invocations=originalInvocations;
        }
    }

    private List<SortStats> selectListForCurrentState(List<SortStats> baseList, boolean resurrectAllowed)
    {
        if(baseList==null)
            return null;
        if(resurrectAllowed)
            return baseList;

        List<SortStats> filtered=new ArrayList<>();
        for(SortStats spell : baseList)
        {
            if(spell==null)
                continue;
            if(!containsResurrectionEffect(spell))
                filtered.add(spell);
        }
        return filtered.isEmpty()?Collections.emptyList():filtered;
    }

    private List<SortStats> copyList(List<SortStats> source)
    {
        if(source==null||source.isEmpty())
            return source;
        return new ArrayList<>(source);
    }

    private boolean hasDeadAlly()
    {
        if(this.fight==null||this.fighter==null)
            return false;

        for(Pair<Integer, Fighter> entry : this.fight.getDeadList())
        {
            Fighter dead=entry.getRight();
            if(dead!=null&&dead.getTeam()==this.fighter.getTeam()&&!dead.hasLeft())
                return true;
        }
        return false;
    }

    private boolean containsResurrectionEffect(SortStats spell)
    {
        if(spell.getEffects()!=null)
            for(SpellEffect effect : spell.getEffects())
                if(effect!=null&&effect.getEffectID()==RESURRECTION_EFFECT_ID)
                    return true;

        if(spell.getCCeffects()!=null)
            for(SpellEffect effect : spell.getCCeffects())
                if(effect!=null&&effect.getEffectID()==RESURRECTION_EFFECT_ID)
                    return true;

        return false;
    }
}