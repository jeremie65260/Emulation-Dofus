package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.List;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.spells.Spell;
import soufix.fight.spells.Spell.SortStats;
import soufix.fight.spells.SpellEffect;
import soufix.utility.Pair;

/**
 * Variante de {@link IA203} qui ne permet au monstre de lancer ses sorts de
 * résurrection (effet 780) que lorsque l'un de ses alliés est déjà tombé au
 * combat. Tant qu'aucun allié n'est mort, l'IA retire temporairement ces sorts
 * de toutes ses listes (buffs, sorts à priorité haute, corps à corps et
 * invocations) avant de déléguer la décision à la logique parente.
 */
public class IA204 extends IA203
{
    /** Identifiant d'effet utilisé par "Laisse spirituelle" pour relever un allié. */
    private static final int RESURRECTION_EFFECT_ID=780;

    public IA204(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
    }

    @Override
    public void apply()
    {
        // Sauvegarde de l'état courant pour restauration en fin de tour d'IA.
        List<SortStats> originalBuffs=this.buffs;
        List<SortStats> originalHighests=this.highests;
        List<SortStats> originalCacs=this.cacs;
        List<SortStats> originalInvocations=this.invocations;
        try
        {
            // Détermine si un allié mort est disponible pour justifier l'usage du sort.
            boolean resurrectAllowed=hasDeadAlly();
            if(!resurrectAllowed)
            {
                this.buffs=selectListForCurrentState(originalBuffs,false);
                this.highests=selectListForCurrentState(originalHighests,false);
                this.cacs=selectListForCurrentState(originalCacs,false);
                this.invocations=selectListForCurrentState(originalInvocations,false);
            }
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

    /**
     * Retourne la liste de sorts à utiliser pour ce tour.
     * <p>
     * Lorsque la résurrection n'est pas autorisée, les sorts contenant
     * l'effet 780 sont supprimés d'une copie temporaire de la liste, ce qui
     * évite de les proposer à la logique parente.
     */
    private List<SortStats> selectListForCurrentState(List<SortStats> source, boolean resurrectAllowed)
    {
        if(source==null)
            return null;
        if(resurrectAllowed)
            return source;

        List<SortStats> filtered=null;
        for(SortStats spell : source)
        {
            if(spell==null||containsResurrectionEffect(spell))
                continue;
            if(filtered==null)
                filtered=new ArrayList<>(source.size());
            filtered.add(spell);
        }

        if(filtered==null)
            return new ArrayList<>();

        return filtered;
    }

    /**
     * Vérifie s'il existe un allié mort n'ayant pas quitté le combat, condition
     * nécessaire pour autoriser la résurrection.
     */
    private boolean hasDeadAlly()
    {
        if(this.fight==null||this.fighter==null)
            return false;

        List<Pair<Integer, Fighter>> deadList=this.fight.getDeadList();
        if(deadList==null||deadList.isEmpty())
            return false;

        for(int i=deadList.size()-1;i>=0;i--)
        {
            Pair<Integer, Fighter> entry=deadList.get(i);
            if(entry==null)
                continue;
            Fighter dead=entry.getRight();
            if(dead==null)
                continue;
            if(dead.getFight()!=this.fight)
                continue;
            if(dead.hasLeft()||!dead.isDead())
                continue;
            if(dead.isInvocation()||dead.isDouble())
                continue;
            if(dead.getTeam()==this.fighter.getTeam())
                return true;
        }

        return false;
    }

    /**
     * Indique si le sort passé en paramètre déclenche l'effet de résurrection.
     * Les effets de coup critique sont également inspectés pour éviter une
     * résurrection involontaire via un CC.
     */
    private boolean containsResurrectionEffect(SortStats spell)
    {
        return containsResurrectionEffect(spell,spell.getEffects())
                || containsResurrectionEffect(spell,spell.getCCeffects());
    }

    private boolean containsResurrectionEffect(SortStats spell, List<SpellEffect> effects)
    {
        if(effects==null||effects.isEmpty())
            return false;

        boolean hasResurrectionEffect=false;

        for(int index=0;index<effects.size();index++)
        {
            SpellEffect effect=effects.get(index);
            if(effect==null)
                continue;

            int effectId=effect.getEffectID();
            if(effectId==RESURRECTION_EFFECT_ID)
            {
                if(isAllyRestrictedResurrection(spell,index))
                    hasResurrectionEffect=true;
            }
            else if(effectId!=0)
            {
                return false;
            }
        }

        return hasResurrectionEffect;
    }

    private boolean isAllyRestrictedResurrection(SortStats spell, int effectIndex)
    {
        Integer targetFlags=getTargetFlags(spell,effectIndex);
        if(targetFlags==null)
            return false;

        if((targetFlags&4)!=0)
            return true;
        if((targetFlags&32)!=0)
            return true;
        if((targetFlags&64)!=0)
            return true;
        if((targetFlags&128)!=0)
            return true;

        return false;
    }

    private Integer getTargetFlags(SortStats spell, int effectIndex)
    {
        if(spell==null)
            return null;

        Spell baseSpell=spell.getSpell();
        if(baseSpell==null)
            return null;

        List<ArrayList<Integer>> targets=baseSpell.getEffectTargets();
        if(targets==null||targets.isEmpty())
            return null;

        int level=Math.max(1,spell.getLevel());
        int levelIndex=Math.min(level-1,targets.size()-1);
        ArrayList<Integer> levelTargets=targets.get(levelIndex);
        if(levelTargets==null||effectIndex<0||effectIndex>=levelTargets.size())
            return null;

        return levelTargets.get(effectIndex);
    }
}
