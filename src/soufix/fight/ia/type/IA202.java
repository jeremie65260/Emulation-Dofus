package soufix.fight.ia.type;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;

public class IA202 extends AbstractNeedSpell
{
    private boolean attackedThisTurn = false;

    public IA202(Fight fight, Fighter fighter, byte count)
    {
        super(fight, fighter, count);
    }

    @Override
    public void apply()
    {
        if (!this.stop && this.fighter.canPlay() && this.count > 0)
        {
            int time = 100, maxPo = 1;
            boolean action = false;
            attackedThisTurn = false;

            Fighter ennemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter);

            for (Spell.SortStats spellStats : this.highests)
                if (spellStats.getMaxPO() > maxPo)
                    maxPo = spellStats.getMaxPO();

            Fighter L = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 1, maxPo + 1);
            Fighter C = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 0, 2);

            if (maxPo == 1) L = null;
            if (C != null && C.isHide()) C = null;
            if (L != null && L.isHide()) L = null;

            // Avancer si personne n'est à portée
            if (this.fighter.getCurPm(this.fight) > 0 && L == null && C == null)
            {
                int value = Function.getInstance().moveautourIfPossible(this.fight, this.fighter, ennemy);
                if (value != 0)
                {
                    time = value;
                    action = true;

                    // Recalcule des cibles après déplacement
                    L = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 1, maxPo + 1);
                    C = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 0, 2);
                    if (maxPo == 1) L = null;
                    if (C != null && C.isHide()) C = null;
                    if (L != null && L.isHide()) L = null;
                }
            }

            // Invocation si possible
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().invocIfPossible(this.fight, this.fighter, this.invocations))
                {
                    time = 600;
                    action = true;
                }
            }

            // Buff (sur soi) si possible
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().buffIfPossible(this.fight, this.fighter, this.fighter, this.buffs))
                {
                    time = 1200;
                    action = true;
                }
            }

            // Auto-soin < 85%
            int percentPdv = (this.fighter.getPdv() * 100) / this.fighter.getPdvMax();
            if (this.fighter.getCurPa(this.fight) > 0 && !action && percentPdv < 85)
            {
                if (Function.getInstance().HealIfPossible(this.fight, this.fighter, true, 85) != 0)
                {
                    time = 400;
                    action = true;
                }
            }

            // Soin alliés (jusqu'à 80%)
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().HealIfPossible(this.fight, this.fighter, false, 80) != 0)
                {
                    time = 400;
                    action = true;
                }
            }

            // Attaque distance ou CAC
            if (this.fighter.getCurPa(this.fight) > 0 && L != null && C == null && !action)
            {
                int value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests);
                if (value != -1)
                {
                    time = value;
                    action = true;
                    attackedThisTurn = true;
                }
            }
            else if (this.fighter.getCurPa(this.fight) > 0 && C != null && !action)
            {
                int value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.cacs);
                if (value != -1)
                {
                    time = value;
                    action = true;
                    attackedThisTurn = true;
                }
            }

            // Fuite seulement si une attaque a été faite ce tour
            if (this.fighter.getCurPm(this.fight) > 0 && attackedThisTurn)
            {
                int value = Function.getInstance().moveFarIfPossible(this.fight, this.fighter);
                if (value != 0) time = value;
            }

            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0)
                this.stop = true;

            addNext(this::decrementCount, time);
        }
        else
        {
            this.stop = true;
        }
    }
}
