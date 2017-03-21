package events;

import java.util.Observable;

public class EventManager extends Observable
{
	public void fireEvent(Object event)
	{
		setChanged();
		notifyObservers(event);
	}
}
