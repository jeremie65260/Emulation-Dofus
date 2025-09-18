package soufix.fight.ia.type;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;
import soufix.main.Config;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class IA203 extends AbstractNeedSpell
{
    boolean hasSummons=false;
    Fighter summon=null;

    public IA203(Fight fight, Fighter fighter, byte count)
    {
        super(fight,fighter,count);
    }

    @Override
    public void apply()
    {
        if(!this.stop&&this.fighter.canPlay()&&this.count>0)
        {
            int time=100,maxPo=1;
            boolean action=false;
            Fighter ennemy=Function.getInstance().getNearestEnnemy(this.fight,this.fighter);

            // PO max depuis les sorts distance
            for(Spell.SortStats spellStats : this.highests)
                if(spellStats.getMaxPO()>maxPo)
                    maxPo=spellStats.getMaxPO();

            // Détections (IA37-like)
            Fighter L=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,1,maxPo+1); // 1..maxPo
            Fighter C=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,0,2);        // 0..2

            if(maxPo==1) L=null;
            if(C!=null && C.isHide()) C=null;
            if(L!=null && L.isHide()) L=null;

            // 1) Se rapprocher si rien à portée
            if(this.fighter.getCurPm(this.fight)>0 && L==null && C==null)
            {
                int value=Function.getInstance().moveautourIfPossible(this.fight,this.fighter,ennemy);
                if(value!=0)
                {
                    time=value;
                    action=true;
                    // Recalc
                    L=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,1,maxPo+1);
                    C=Function.getInstance().getNearestEnnemynbrcasemax(this.fight,this.fighter,0,2);
                    if(maxPo==1) L=null;
                    if(C!=null && C.isHide()) C=null;
                    if(L!=null && L.isHide()) L=null;
                }
            }

            // 2) Invocation
            if(this.fighter.getCurPa(this.fight)>0 && !action)
            {
                if(Function.getInstance().invocIfPossible(this.fight,this.fighter,this.invocations))
                {
                    time=600;
                    action=true;
                }
            }

            // 3) Soin perso si <50%
            int percentPdv=(this.fighter.getPdv()*100)/this.fighter.getPdvMax();
            if(this.fighter.getCurPa(this.fight)>0 && !action && percentPdv<50)
            {
                if(Function.getInstance().HealIfPossible(this.fight,this.fighter,true,50)!=0)
                {
                    time=400;
                    action=true;
                }
            }

            // 4) Buff sur soi
            if(this.fighter.getCurPa(this.fight)>0 && !action)
            {
                if(Function.getInstance().buffIfPossible(this.fight,this.fighter,this.fighter,this.buffs))
                {
                    time=400;
                    action=true;
                }
            }

            // 5) Détection d’invocations ennemies (anti-summon)
            if(!hasSummons)
            {
                Iterator<Fighter> it=this.fight.getFighters(this.fighter.getOtherTeam()).iterator();
                while(it.hasNext())
                {
                    Fighter next=it.next();
                    if(next.isInvocation())
                    {
                        hasSummons=true;
                        summon=next;
                    }
                }
            }

            if(this.fighter.getCurPa(this.fight)>0 && !action && hasSummons && summon!=null)
            {
                int value=this.fight.tryCastSpell(this.fighter,this.antisummon.get(0),this.summon.getCell().getId());
                if(value==0)
                {
                    time=value;
                    action=true;
                    hasSummons=false;
                    summon=null;
                }
            }

            // 6) Attaque DISTANCE si L présent et pas de C sur ce tick
            if(this.fighter.getCurPa(this.fight)>0 && L!=null && C==null && !action)
            {
                int value=Function.getInstance().attackIfPossible(this.fight,this.fighter,this.highests);
                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            // 7) Attaque CàC avec PRIORITÉ PO=1 (Exécution) puis fallback CàC normal (1..2), puis fallback highests
            if(this.fighter.getCurPa(this.fight)>0 && C!=null && !action)
            {
                // Liste filtrée des CàC stricts (PO max == 1)
                List<Spell.SortStats> cacStrict = new ArrayList<Spell.SortStats>();
                for (Spell.SortStats s : this.cacs)
                {
                    if (s != null && s.getMaxPO() == 1) cacStrict.add(s); // Exécution typiquement
                }

                int value = -1;

                // a) Essayer d'abord un CàC strict 1–1
                if (!cacStrict.isEmpty())
                    value = Function.getInstance().attackIfPossible(this.fight, this.fighter, cacStrict);

                // b) Sinon, essayer les CàC "classiques" (inclut 1–2)
                if (value == -1)
                    value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.cacs);

                // c) Dernier recours : un sort highests (beaucoup passent à 1–2)
                if (value == -1)
                    value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests);

                if(value!=-1)
                {
                    time=value;
                    action=true;
                }
            }

            // 8) Soin alliés si possible
            if(this.fighter.getCurPa(this.fight)>0 && !action)
            {
                if(Function.getInstance().HealIfPossible(this.fight,this.fighter,false,80)!=0)
                {
                    time=400;
                    action=true;
                }
            }

            // 9) Déplacement de fin
            if(this.fighter.getCurPm(this.fight)>0 && !action)
            {
                int value=Function.getInstance().moveautourIfPossible(this.fight,this.fighter,ennemy);
                if(value!=0)
                    time=value;
            }

            if(this.fighter.getCurPa(this.fight)==0 && this.fighter.getCurPm(this.fight)==0)
                this.stop=true;

            addNext(this::decrementCount, time + Config.getInstance().AIDelay);
        }
        else
        {
            this.stop=true;
        }
    }
}
