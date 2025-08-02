package soufix.fight.ia.type;

import soufix.fight.Fight;
import soufix.fight.Fighter;
import soufix.fight.ia.AbstractNeedSpell;
import soufix.fight.ia.util.Function;
import soufix.fight.spells.Spell;

public class IA201 extends AbstractNeedSpell {

    public IA201(Fight fight, Fighter fighter, byte count) {
        super(fight, fighter, count);
    }

    @Override
    public void apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            int time = 100, maxPo = 1;
            boolean action = false;

            // Détection de la portée maximale des sorts
            for (Spell.SortStats spellStats : this.highests) {
                if (spellStats.getMaxPO() > maxPo) {
                    maxPo = spellStats.getMaxPO();
                }
            }

            // Recherche des cibles
            Fighter ennemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter);
            Fighter L = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 1, maxPo + 1);
            Fighter C = Function.getInstance().getNearestEnnemynbrcasemax(this.fight, this.fighter, 0, 2);
            Fighter A = Function.getInstance().getNearestAminbrcasemax(this.fight, this.fighter, 0, maxPo);

            // Filtrage des cibles invisibles
            if (C != null && C.isHide()) C = null;
            if (L != null && L.isHide()) L = null;

            // 1. Priorité : Déplacement pour se rapprocher des alliés à booster
            if (this.fighter.getCurPm(this.fight) > 0 && !action && A != null) {
                int value = Function.getInstance().moveautourIfPossible(this.fight, this.fighter, A);
                if (value != 0) {
                    time = value; // Temps après déplacement
                    action = true;
                    System.out.println("IA201 : Déplacement effectué pour se rapprocher d'un allié.");
                }
            }

            // 2. Buff allié prioritaire
            if (this.fighter.getCurPa(this.fight) > 0 && !action && A != null) {
                if (Function.getInstance().buffIfPossible(this.fight, this.fighter, A, this.buffs)) {
                    time = 400;
                    action = true;
                    System.out.println("IA201 : Buff lancé sur un allié.");
                }
            }

            // 3. Invocation à 1 case de lui-même
            if (this.fighter.getCurPa(this.fight) > 0 && !action) {
                if (Function.getInstance().invocIfPossible(this.fight, this.fighter, this.invocations)) {
                    time = 2000; // Temps après invocation
                    action = true;
                    System.out.println("IA201 : Invocation réalisée à 1 case.");
                }
            }

            // 4. Attaque à distance (si possible)
            if (this.fighter.getCurPa(this.fight) > 0 && !action && L != null) {
                int value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests);
                if (value != -1) {
                    time = value; // Temps après attaque à distance
                    action = true;
                    System.out.println("IA201 : Attaque à distance réalisée.");
                }
            }

            // 5. Attaque au corps à corps
            if (this.fighter.getCurPa(this.fight) > 0 && !action && C != null) {
                int value = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.cacs);
                if (value != -1) {
                    time = value; // Temps après attaque au corps à corps
                    action = true;
                    System.out.println("IA201 : Attaque au corps à corps réalisée.");
                }
            }

            // 6. Déplacement défensif en dernier recours
            if (this.fighter.getCurPm(this.fight) > 0 && !action) {
                int value = Function.getInstance().moveFarIfPossible(this.fight, this.fighter);
                if (value != 0) {
                    time = value; // Temps après déplacement défensif
                    System.out.println("IA201 : Déplacement défensif réalisé.");
                }
            }

            // Fin du tour si aucun PA ou PM disponibles
            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) {
                this.stop = true;
            }

            // Planifie la prochaine action
            addNext(this::decrementCount, time);
        } else {
            this.stop = true;
        }
    }
}
