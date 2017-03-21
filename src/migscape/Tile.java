package migscape;

/**
 * The Tile class creates objects of type Tile that contain
 * the Position on X/Y space, an Agent and a boolean of
 * whether the tile has an agent assigned to it.
 * @author Linda
 */

public class Tile
{
	public int[] position = new int[2]; //0 = x, 1 = y
	boolean hasAgent;
	public boolean hasNeighbour;
	boolean isBlue;
	int isHappy; // 0 no, 1 yes, 2 null
	public boolean belongsToGroup;
	public boolean belongsToInflux;
	public boolean isBestStart;
	public boolean isStartCandidate;
	int appealint;
	double agentTolerance;

	public Tile(int newPosX, int newPosY)
	{
		position[0] = newPosX;
		position[1] = newPosY;
		belongsToGroup = false;
		belongsToInflux = false;
		isBestStart = false;
		isStartCandidate = false;
		agentTolerance = -7.0;
		appealint =  -9;
	}
	public void setAppeal(int newappeal)
	{
		appealint = newappeal;
	}
	
	public int getAppeal()
	{
		return appealint;
	}
	public boolean hasAgent()
	{
		if (hasAgent == true)
		{

			return true;
		}
		else
			return false;
	}

	public boolean isAgentBlue()
	{
		return isBlue;
	}

	public int isAgentHappy()
	{
		return isHappy;
	}

	public void setAgentIsHappy(int isagenthappy)
	{
		isHappy = isagenthappy;
	}

	public void setAgentIsBlue(boolean isagentBlue)
	{
		isBlue = isagentBlue;
	}

	public void setAgentAvgF(double average)
	{
		agentTolerance = average;
	}

	public boolean hasNeighbour()
	{
		return hasNeighbour;
	}
	
	public double getT()
	{
		return agentTolerance;
	}

	public void removeAgent()
	{
		hasAgent = false;
		agentTolerance = -7.0;
		isHappy = 2;
	}

	public void setNeighbour()
	{
		hasNeighbour = true;
	}

	public void setBestStart(boolean isBest)
	{
		isBestStart = true;
	}

	public boolean addAgent(boolean isItBlue, int isItHappy, double f)
	{

		if (this.hasAgent) return false;
		hasAgent = true;
		if (isItBlue)
		{
			isBlue = true;
		}
		else
		{
			isBlue = false;
		}
		agentTolerance = f;
		isHappy = isItHappy;
		//System.out.println("<<Tile.addAgent>> agent added. isItBlue is " + isItBlue);
		return true;
	}

	public int[] getPosition()
	{
		return position;
	}

	public void setPosition(int[] newPosition)
	{
		position[0] = newPosition[0];
		position[1] = newPosition[1];
	}


}