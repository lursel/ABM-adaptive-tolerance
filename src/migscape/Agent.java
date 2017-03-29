package migscape;

import java.util.LinkedList;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class Agent
{
	public int posX, posY;
	public int pos[] = new int[2];
	public int moveCounter = 0;
	public boolean amIalone = false;
	public boolean isBlue;
	public int isHappy = 2;
	public double threshold; // % of similar WANTED
	public LinkedList<Double> memory;
	
	public Agent(int setPosX, int setPosY, boolean isBlue, double thethreshold, int happeh)
	{
		posX = setPosX;
		posY = setPosY;
		pos[0] = setPosX;
		pos[1] = setPosY;
		this.isBlue = isBlue; //blue or green agents
		isHappy = happeh; //agent creation defaults to no happiness which should be determined by agents later on
		threshold = thethreshold;
		memory = new LinkedList<Double>();
	}
	public void setPosition(int newPosX, int newPosY)
	{
		pos[0] = newPosX;
		pos[1] = newPosY;
		posX = newPosX;
		posY = newPosY;
		moveCounter++;
	}
	
	public int[] getPosition()
	{
		return pos;
	}
	
	public double getThreshold()
	{
		return threshold;
	}

	public void setThreshold(double value)
	{
		this.threshold = value;
	}
	
	public int getHappy()
	{
		return isHappy;
	}
}
