package migscape;


public class Position
{
	int x;
	int y;

	@Override
	public boolean equals(Object compareObject)
	{
		boolean returnVal = false;

		if (compareObject instanceof Position)
		{
			Position pointerTocompareObject = (Position) compareObject;
			returnVal = (pointerTocompareObject.getX() == this.x && pointerTocompareObject.getY() == this.y);
		}

		return returnVal;
	}

	public Position(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public Position getPosition()
	{
		return this;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

}