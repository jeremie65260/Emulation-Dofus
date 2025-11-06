package soufix.fight.ia.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import soufix.fight.Fight;
import soufix.fight.Fighter;
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

    /** Copie des sorts de buff d'origine, conservée pour pouvoir les restaurer. */
    private final List<SortStats> baseBuffs;
    /** Copie des sorts à priorité élevée. */
    private final List<SortStats> baseHighests;
    /** Copie des sorts de corps à corps. */
    private final List<SortStats> baseCacs;
    /** Copie des sorts d'invocation. */
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
        // Sauvegarde de l'état courant pour restauration en fin de tour d'IA.
        List<SortStats> originalBuffs=this.buffs;
        List<SortStats> originalHighests=this.highests;
        List<SortStats> originalCacs=this.cacs;
        List<SortStats> originalInvocations=this.invocations;
        try
        {
            // Détermine si un allié mort est disponible pour justifier l'usage du sort.
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

    /**
     * Retourne la liste de sorts à utiliser pour ce tour.
     * <p>
     * Lorsque la résurrection n'est pas autorisée, les sorts contenant
     * l'effet 780 sont supprimés de la liste, ce qui évite de les proposer à
     * la logique parente.
     */
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

    /**
     * Crée une copie superficielle de la liste de sorts d'origine afin de pouvoir
     * la restaurer plus tard.
     */
    private List<SortStats> copyList(List<SortStats> source)
    {
        if(source==null||source.isEmpty())
            return source;
        return new ArrayList<>(source);
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
        if(deadList!=null&&!deadList.isEmpty())
        {
            for(int i=deadList.size()-1;i>=0;i--)
            {
                Pair<Integer, Fighter> deadEntry=deadList.get(i);
                Fighter dead=deadEntry==null?null:deadEntry.getRight();
                if(!isValidResurrectionCandidate(dead))
                {
                    if(dead!=null)
                        this.fight.removeDead(dead);
                    else
                        deadList.remove(i);
                    continue;
                }

                if(dead.getTeam()==this.fighter.getTeam())
                    return true;
            }
        }

        // File d'attente nettoyée : on vérifie directement l'état des combattants de l'équipe.
        for(Fighter ally : this.fight.getTeam(this.fighter.getTeam()).values())
        {
            if(isValidResurrectionCandidate(ally))
                return true;
        }

        return false;
    }

    private boolean isValidResurrectionCandidate(Fighter fighter)
    {
        return fighter!=null&&fighter.getFight()==this.fight&&!fighter.hasLeft()&&fighter.isDead()&&!fighter.isInvocation()&&!fighter.isDouble();
    }

    /**
     * Indique si le sort passé en paramètre déclenche l'effet de résurrection.
     * Les effets de coup critique sont également inspectés pour éviter une
     * résurrection involontaire via un CC.
     */
    private boolean containsResurrectionEffect(SortStats spell)
    {
        boolean hasResurrectionEffect=false;
        boolean hasOtherEffect=false;

        if(spell.getEffects()!=null)
            for(SpellEffect effect : spell.getEffects())
            {
                if(effect==null)
                    continue;
                if(effect.getEffectID()==RESURRECTION_EFFECT_ID)
                    hasResurrectionEffect=true;
                else if(effect.getEffectID()!=0)
                    hasOtherEffect=true;
            }

        if(spell.getCCeffects()!=null)
            for(SpellEffect effect : spell.getCCeffects())
            {
                if(effect==null)
                    continue;
                if(effect.getEffectID()==RESURRECTION_EFFECT_ID)
                    hasResurrectionEffect=true;
                else if(effect.getEffectID()!=0)
                    hasOtherEffect=true;
            }

        return hasResurrectionEffect&&!hasOtherEffect;
    }
}
