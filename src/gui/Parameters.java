package gui;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.JTextField;

import events.EventManager;
import events.SimulationStartedEvent;
import events.TickEnd;

@SuppressWarnings("serial")
public class Parameters extends JPanel implements Observer
{
	int tick = 0;
	int sim = 0;
	double h = -1;
	double[] tregb = { -2.0, -2.0 };
	int realtick;
	double mc = 0.0;
	double mt = 0.0;
	//int[] population = { 0, 0 };
	DecimalFormat df = new DecimalFormat("#.#");
	DecimalFormat df2 = new DecimalFormat("#.##");
	EventManager eventMgr;
	JTextField showTB = new JTextField("TB: " + tregb[0]); //blue thresh
	JTextField showTG = new JTextField("TG: " + tregb[1]); // green thresh
	JTextField paraH = new JTextField("H: " + h);
	JTextField paramC = new JTextField("mC: " + mc);
	JTextField paramT = new JTextField("mT: " + mt);

	public Parameters(final EventManager event, int[] rules)
	{
		eventMgr = event;

		this.add(showTB);
		this.add(showTG);
		this.add(paraH);
		this.add(paramC);
		this.add(paramT);

		showTB.setPreferredSize(new Dimension(70, 24));
		showTG.setPreferredSize(new Dimension(70, 24));
		paraH.setPreferredSize(new Dimension(60, 24));
		paramC.setPreferredSize(new Dimension(60, 24));
		paramT.setPreferredSize(new Dimension(60, 24));
	}

	@Override
	public void update(Observable o, Object arg)
	{

		if (arg instanceof TickEnd)
		{
			realtick = realtick + 1;
			TickEnd tickEndOtherName = (TickEnd) arg;
			tick = tickEndOtherName.getTick();
			h = tickEndOtherName.getGHapp();
			mc = tickEndOtherName.getMC();
			mt = tickEndOtherName.getMT();
			
			tregb = ((TickEnd) arg).getBGThresh();
			showTB.setText("TB: " + df2.format(tregb[0]));
			showTG.setText("TG: " + df2.format(tregb[1]));
			paraH.setText("H: " + df2.format(h));
			paramC.setText("mC: " + df2.format(mc));
			paramT.setText("mT: " + df2.format(mt));
		}

		if (arg instanceof SimulationStartedEvent)
		{
			sim = ((SimulationStartedEvent) arg).getSimNumber() + 1;
		}

	}
}
