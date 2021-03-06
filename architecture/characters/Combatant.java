package architecture.characters;

import com.sun.javafx.geom.Point2D;

import java.awt.*;
import java.util.TimerTask;


/**
 * The abstract class for all objects which engage in battle. Combatant handles
 * attribute and stats (numerical values relevant to calculating various aspects
 * of battle), storage of volatile effects, changes in health and mana, location
 * and movement etc.
 *
 * @author Kevin Liu
 * @version May 8, 2017
 * @author Period: 5
 * @author Assignment: APCS Final
 *
 * @author Sources: none
 */
public abstract class Combatant extends TimerTask implements Renderable
{
    private Point2D previousTopLeftCorner;

    protected Point2D topLeftCorner;

    public static final int WIDTH = 100;

    public static final int HEIGHT = WIDTH;

    private double xVelocity = 0, yVelocity = 0;

    protected int level, health, mana;

    // [STR, INT, DEX, SPD, VIT, WIS, LUK]
    protected int[] baseAttributes = new int[7];

    protected int[] modifiedAttributes = new int[7];

    // [HP, mana, ATK, DEF, ACC, AVO, CRIT, CRITAVO]
    protected Stats stats = new Stats();

    private int actionBar = 0;

    public boolean canAttack = true;

    public CombatResult result;

    /**
     * Abbreviated codes for each attribute, in order of storage.
     */
    public static final String[] attributeNames = { "STR", "INT", "DEX", "SPD",
        "VIT", "WIS", "LUK" };

    /**
     * Abbreviated codes for each stat, in order of storage.
     */
    public static final String[] statNames = { "HP", "MP", "ATK", "DEF", "ACC",
        "AVO", "CRIT", "CRITAVO" };

    // Constants used to calculate stats from attributes.
    protected static final double healthFactor = 4, manaFactor = 3,
                    accuracyFactor = 5, critFactor = 5, baseCrit = 5,
                    damageFactor = .332;


    // Constants used to calculate damage.
    private static final double damageBase = Math.pow( 3, 1 / 10 );

    // Constants used to calculate variation in damage dealt.
    private static final double varFactor = 1.23;

    private static final double inverseVar = Math.pow( varFactor, -1 );

    protected static final int actionLimit = 2500;

    private static final int resultTime = 100;

    private int resultTimer;


    protected Combatant( Point2D initPose )
    {
        this.topLeftCorner = initPose;
    }


    protected Combatant()
    {
        this( new Point2D( 0, 0 ) );
    }


    /**
     * @return terminal velocity
     */
    protected int getTerminalVelocity()
    {
        return 100;
    }


    /**
     * @return friction
     */
    protected double getFriction()
    {
        return 0.10;
    }


    /**
     * @return acceleration given friction and terminal velocity
     */
    protected double getAcceleration()
    {
        return getTerminalVelocity() * ( 1 / getFriction() - 1 ) / 10D;
    }


    /**
     * @return the current location
     */
    public Point2D getPose()
    {
        return topLeftCorner;
    }


    /**
     * @return the previous location
     */
    public Point2D getPreviousPose()
    {
        return previousTopLeftCorner;
    }


    /**
     * Moves the Combatant back to the previous location.
     */
    public void resetPoseToPrevios()
    {
        topLeftCorner = previousTopLeftCorner;
        previousTopLeftCorner = null;
    }


    /**
     * Moves the Combatant by x units left and y units down.
     * 
     * @param x
     *            net leftward change
     * @param y
     *            net downward change
     */
    public void move( float x, float y )
    {
        previousTopLeftCorner = topLeftCorner;
        topLeftCorner = new Point2D( topLeftCorner.x + x, topLeftCorner.y + y );
    }


    /**
     * Moves (teleports) the Combatant to a new location
     * 
     * @param x
     *            x-coordinate
     * @param y
     *            y-coordinate
     */
    public void moveTo( float x, float y )
    {
        previousTopLeftCorner = topLeftCorner;
        topLeftCorner = new Point2D( x, y );
    }


    /**
     * @return a Rectangle occupying the same space as the combatant
     */
    public Rectangle getBoundingBox()
    {
        return new Rectangle(
            (int)getPose().x,
            (int)getPose().y,
            WIDTH,
            HEIGHT );
    }


    /**
     * Moves (teleports) the Combatant to a new location
     * 
     * @param point2D
     *            new location
     */
    public void moveTo( Point2D point2D )
    {
        moveTo( point2D.x, point2D.y );
    }


    /**
     * Accelerates left if x is positive, right if negative. Accelerates down if
     * y is positive, up if negative.
     * 
     * @param x
     * @param y
     */
    public void accelerate( float x, float y )
    {
        xVelocity += Math.signum( x ) * getAcceleration();
        yVelocity += Math.signum( y ) * getAcceleration();

        xVelocity *= getFriction();
        yVelocity *= getFriction();
    }


    /**
     * Moves the Combatant based on current velocity.
     */
    public void move()
    {
        move( (float)xVelocity, (float)yVelocity );
    }


    /**
     * Stops the Combatant, setting velocities to zero.
     */
    public void stop()
    {
        xVelocity = 0;
        yVelocity = 0;
    }


    public void run()
    {
        resultTimer++;
        if ( resultTimer >= resultTime )
        {
            result = null;
            resultTimer = 0;
        }
        if ( actionBar >= actionLimit )
            canAttack = true;

        else
        {
            actionBar += modifiedAttributes[3];

        }
    }


    /**
     * Attacks another Combatant. The Attack is only carried out if the Attacker
     * is read (bar is full). Attacking resets the bar.
     * 
     * @param defender
     *            the other Combatant
     */
    public void attack( Combatant defender )
    {
        if ( !canAttack || !isInRange( defender ) )
        {

            return;
        }
        canAttack = false;
        actionBar = 0;


        defender.receiveAttack( stats.getATK(),
            stats.getACC(),
            stats.getCRIT(),
            this );
        // defender.printVitals();
        resultTimer = 0;
    }


    /**
     * This method is called when the Combatant receives a normal attack. Damage
     * is calculated using passed stats from the attacker and defensive stats of
     * the defender (this object). This method also sets the last combatResult
     * as the result of this combat.
     * 
     * @param atk
     *            nominal attack value of attacker
     * @param acc
     *            accuracy value of attacker
     * @param crit
     *            critical value of attacker
     * @param attacker
     *            the attacker
     */
    private void receiveAttack( int atk, int acc, int crit, Combatant attacker )
    {
        CombatResult result = new CombatResult( attacker, this );

        // Test for miss
        if ( Math.random() * 100 > acc - stats.getAVO() )
        {
            result.setDamage( 0 );
            result.setHit( false );
            result.setCritical( false );
            this.result = result;
            return;
        }
        result.setHit( true );

        // Calculate damage
        int diff = atk - stats.getDEF();

        // Generates a random number between the reciprocal of varFactor and
        // varFactor
        double var = Math.random() * ( varFactor - inverseVar ) + inverseVar;

        int damage = (int)Math
            .round( Math.pow( damageBase, diff ) * var * atk * damageFactor );

        // Test for critical hit.
        if ( Math.random() * 100 <= crit - stats.getCRITAVO() )
        {
            damage *= 2;
            result.setCritical( true );
        }

        result.setDamage( damage );

        healthLoss( damage );
        this.result = result;
    }


    /**
     * @return if the Combatant is dead
     */
    public boolean isDead()
    {
        return getHealth() <= 0;
    }


    /**
     * Tests if the Combatant is "in range" of another Combatant, which is a
     * prerequisite to attacking.
     * 
     * @param other
     *            the other Combatant
     * @return if this is in range of another Combatant
     */
    public boolean isInRange( Combatant other )
    {
        double distance = getPose().distance( other.getPose() );
        return ( distance <= getRange() );
    }


    /**
     * Updates the attributes of the Combatant, which in turn updates the stats.
     * Method is called whenever equipment or volatile effects change.
     */
    public abstract void updateAttributes();


    /**
     * @return attack range
     */
    public abstract int getRange();


    /**
     * Resets attribute to their base values. To be called only when updating
     * attributes.
     */
    protected void resetAttributes()
    {
        modifiedAttributes = baseAttributes.clone();
    }


    /**
     * Updates the stats of the combatant based purely on attributes and
     * volatile effects.
     */
    protected void updateStats()
    {
        stats.setAll( 0 );

        // HP = VIT * healthFactor
        stats.setHP( (int)Math.round( baseAttributes[4] * healthFactor ) );

        // mana = WIS * manaFactor
        stats.setMP( (int)Math.round( baseAttributes[5] * manaFactor ) );

        // ATK, DEF, ACC are calculated differently for monsters and players.

        // AVO = SPD * speedFactor
        stats.setAVO( (int)Math.round( baseAttributes[3] * accuracyFactor ) );

        // CRIT = LUK * critFactor + baseCrit
        stats.setCRIT(
            (int)Math.round( baseAttributes[6] * critFactor + baseCrit ) );

        // CRITAVO = LUK * critFactor
        stats.setCRITAVO( (int)Math.round( baseAttributes[6] * critFactor ) );

    }


    /**
     * Restores health to the combatant. Health must remain under HP stat (max
     * HP).
     * 
     * @param value
     *            health to restore
     */
    public void restoreHealth( int value )
    {
        int missingHealth = stats.getHP() - health;

        if ( value < missingHealth )
        {
            health += value;
        }
        else
        {
            setHealthFull();
        }
    }


    /**
     * Restores combatant health to full (HP stat).
     */
    public void setHealthFull()
    {
        health = stats.getHP();
    }


    /**
     * Restores mana to the combatant. Mana must remain under mana stat (max
     * mana).
     * 
     * @param value
     *            mana to restore
     */
    public void restoreMana( int value )
    {
        int missingMana = stats.getMP() - mana;

        if ( value < missingMana )
        {
            mana += value;
        }
        else
        {
            setManaFull();
        }
    }


    /**
     * Restores combatant mana to full (mana stat).
     * 
     */
    public void setManaFull()
    {
        mana = stats.getMP();
    }


    /**
     * Deducts health from combatant. If new health is less than 0, death() is
     * called.
     * 
     * @param value
     *            health to deduct
     */
    public void healthLoss( int value )
    {
        if ( health <= value )
        {
            health = 0;
            death();
        }
        else
        {
            health -= value;
        }
    }


    /**
     * Method called when health of Combatant reaches zero. Death of monster
     * removes it from the game and awards exp/items to player, while death of
     * player ends the game.
     */
    public void death()
    {
    }


    /**
     * @return Returns the level.
     */
    public int getLevel()
    {
        return level;
    }


    /**
     * @param level
     *            The level to set.
     */
    public void setLevel( int level )
    {
        this.level = level;
    }


    /**
     * @return Returns the health.
     */
    public int getHealth()
    {
        return health;
    }


    /**
     * @param health
     *            The health to set.
     */
    public void setHealth( int health )
    {
        this.health = health;
    }


    /**
     * @return Returns the mana.
     */
    public int getMana()
    {
        return mana;
    }


    /**
     * Returns the amount of action preparedness.
     * 
     * @return actionBar
     */
    public int getActionBar()
    {
        return actionBar;
    }


    /**
     * @param mana
     *            The mana to set.
     */
    public void setMana( int mana )
    {
        this.mana = mana;
    }


    /**
     * @return Returns the attributes in array form. Attributes are (in order):
     *         [STR, INT, DEX, SPD, VIT, WIS, LUK].
     */
    public int[] getBaseAttributes()
    {
        return baseAttributes;
    }


    /**
     * @param baseAttributes
     *            The baseAttributes to set.
     */
    public void setBaseAttributes( int[] baseAttributes )
    {
        this.baseAttributes = baseAttributes;
    }


    /**
     * @return Returns the modifiedAttributes.
     */
    public int[] getModifiedAttributes()
    {
        return modifiedAttributes;
    }


    /**
     * @return Returns the stats in array form. Stats are (in order): [HP, mana,
     *         ATK, DEF, ACC, AVO, CRIT, CRITAVO].
     */
    public Stats getStats()
    {
        return stats;
    }




    /**
     * Prints a summary of Monster's type, level, HP, mana, attributes, and
     * stats. For testing only.
     */
    public void printStatus()
    {
        String divider = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";

        System.out.println( this + " Lv. " + getLevel() );
        System.out.println( "HP: " + getHealth() + "/" + stats.getHP()
            + " mana: " + getMana() + "/" + stats.getMP() );

        System.out.println( "Attributes" );
        for ( int j = 0; j < 7; j++ )
        {
            System.out.print(
                Combatant.attributeNames[j] + " " + getBaseAttributes()[j]
                    + " (" + getModifiedAttributes()[j] + ") " );
        }

        System.out.println( "\nStats" );
        for ( int j = 0; j < Combatant.statNames.length; j++ )
        {
            System.out.print(
                Combatant.statNames[j] + " " + stats.getStats()[j] + " " );
        }

        System.out.println( "\n" + divider );
    }


}
