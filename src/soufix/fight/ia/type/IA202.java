package soufix.fight.ia.type;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;

public class IA202 extends AbstractNeedSpell
{
    private int attacksThisTurn = 0;
    private final byte startCount; // valeur de count au début du tour

    public IA202(Fight fight, Fighter fighter, byte count)
    {
        super(fight, fighter, count);
        this.startCount = count; // sert à détecter le 1er passage du tour
    }

    @Override
    public void apply()
    {
        if (!this.stop && this.fighter.canPlay() && this.count > 0)
        {
            // Reset compteur au début du tour
            if (this.count == this.startCount) {
                attacksThisTurn = 0;
            }

            int time = 100, maxPo = 1;
            boolean action = false;

            // PO max des sorts distance
            for (Spell.SortStats s : this.highests)
                if (s.getMaxPO() > maxPo)
                    maxPo = s.getMaxPO();

            // cibles (même logique que IA37)
            Fighter ennemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter);
            Fighter L = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 1, maxPo + 1); // distance
            Fighter C = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 0, 2);        // CàC (0..2)

            if (maxPo == 1) L = null;
            if (C != null && C.isHide()) C = null;
            if (L != null && L.isHide()) L = null;

            // si hors portée, se rapprocher (logique IA37)
            if (this.fighter.getCurPm(this.fight) > 0 && L == null && C == null)
            {
                int v = Function.getInstance().moveautourIfPossible(this.fight, this.fighter, ennemy);
                if (v != 0)
                {
                    time = v;
                    action = true;

                    // recalcul des cibles
                    L = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 1, maxPo + 1);
                    C = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 0, 2);
                    if (maxPo == 1) L = null;
                    if (C != null && C.isHide()) C = null;
                    if (L != null && L.isHide()) L = null;
                }
            }

            // invoc
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().invocIfPossible(this.fight, this.fighter, this.invocations))
                {
                    time = 600;
                    action = true;
                }
            }

            // BUFF (sur soi) — priorité avant soins
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().buffIfPossible(this.fight, this.fighter, this.fighter, this.buffs))
                {
                    time = 1200;
                    action = true;
                }
            }

            // soin perso < 85%
            int percentPdv = (this.fighter.getPdv() * 100) / this.fighter.getPdvMax();
            if (this.fighter.getCurPa(this.fight) > 0 && !action && percentPdv < 85)
            {
                if (Function.getInstance().HealIfPossible(this.fight, this.fighter, true, 85) != 0)
                {
                    time = 400;
                    action = true;
                }
            }

            // soin alliés
            if (this.fighter.getCurPa(this.fight) > 0 && !action)
            {
                if (Function.getInstance().HealIfPossible(this.fight, this.fighter, false, 80) != 0)
                {
                    time = 400;
                    action = true;
                }
            }

            // attaque distance
            if (this.fighter.getCurPa(this.fight) > 0 && L != null && C == null && !action)
            {
                int v = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests);
                if (v != -1)
                {
                    time = v;
                    action = true;
                    attacksThisTurn++;
                }
            }
            // attaque CàC (même collé) avec fallback sur highests
            else if (this.fighter.getCurPa(this.fight) > 0 && C != null && !action)
            {
                int v = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.cacs);
                if (v == -1) {
                    // aucun sort CàC → utilise highests
                    v = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests);
                }
                if (v != -1)
                {
                    time = v;
                    action = true;
                    attacksThisTurn++;
                }
            }

            // fuite uniquement après 2 attaques mini
            if (this.fighter.getCurPm(this.fight) > 0 && attacksThisTurn >= 2 && !action)
            {
                int v = Function.getInstance().moveFarIfPossible(this.fight, this.fighter);
                if (v != 0) time = v;
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
