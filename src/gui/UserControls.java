package gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import events.EventManager;
import events.SimulationStartedEvent;
import events.TickEnd;
import events.TickPause;

@SuppressWarnings("serial")
public class UserControls extends JPanel implements Observer
{

	JButton exitButton = new JButton("Exit");
	int tick = 0;
	int sim = 0;
	int realtick;
	int iS =0;
	int iN =0;
	int w = -1;
	double m = -1;
	int[] population = { 0, 0 };
	boolean buttonOn = false;
	boolean pauseOn = false;
	DecimalFormat df3 = new DecimalFormat("#.###");
	EventManager eventMgr;
	JButton pauseToggle = new JButton("PAUSE");
	JButton viewFToggle = new JButton("F OFF");
	JTextField showInflux = new JTextField("Influx is ");
	JTextField showTick = new JTextField("Tick: " + tick);
	JTextField showSim = new JTextField("Sim: " + sim);
	JTextField showInSz = new JTextField(""); //the number of influxes
	JTextField showInNo = new JTextField(""); //the size of each influx 
	JTextField paraW = new JTextField("W: " + w);
	JTextField paraM = new JTextField("M: " + m);

	String influx = "";
	public UserControls(final Grid grid, EventManager event, int[] rules)
	{
		eventMgr = event;
		this.add(exitButton);
		this.add(pauseToggle);
		this.add(viewFToggle);
		this.add(showInflux);
		this.add(showTick);
		this.add(showSim);
		this.add(showInNo);
		this.add(showInSz);
		this.add(paraW);
		this.add(paraM);
		
		if (rules[1] == 0)
		{
			influx = "OFF";
		}
		else if (rules[1] == 1)
		{
			influx = "ON";
		}
		else
		{
			influx = "ERR";
		}

		exitButton.setMargin(new Insets(0, 0, 0, 0));
		exitButton.setPreferredSize(new Dimension(40, 25));

		viewFToggle.setMargin(new Insets(0, 0, 0, 0));
		viewFToggle.setPreferredSize(new Dimension(50, 25));

		pauseToggle.setMargin(new Insets(0, 0, 0, 0));
		pauseToggle.setPreferredSize(new Dimension(55, 25));

		showTick.setPreferredSize(new Dimension(80, 24));
		showSim.setPreferredSize(new Dimension(55, 24));
		showInflux.setPreferredSize(new Dimension(75, 24));
		showInNo.setPreferredSize(new Dimension(30, 24));
		showInSz.setPreferredSize(new Dimension(50, 24));
		
		paraW.setPreferredSize(new Dimension(60, 24));
		paraM.setPreferredSize(new Dimension(60, 24));

		exitButton.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});

		viewFToggle.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				if (buttonOn == false)
				{
					grid.setViewMode(true);
					viewFToggle.setText("F: ON");
					buttonOn = true;
				}
				else
				{
					grid.setViewMode(false);
					viewFToggle.setText("F: OFF");
					buttonOn = false;
				}
			}
		});

		pauseToggle.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent e)
			{
				if (pauseOn == false)
				{
					pauseToggle.setText("RUN");
					pauseOn = true;
					eventMgr.fireEvent(new TickPause());

				}
				else if (pauseOn == true)
				{
					pauseToggle.setText("PAUSE");
					pauseOn = false;
					eventMgr.fireEvent(new TickPause());

				}
			}
		});
	}

	@Override
	public void update(Observable o, Object arg)
	{

		if (arg instanceof TickEnd)
		{
			realtick = realtick + 1;
			showTick.setText("Tick: " + realtick);
			TickEnd tickEndOtherName = (TickEnd) arg;
			tick = tickEndOtherName.getTick();
			population = tickEndOtherName.getPopulationData();
			showInflux.setText("Influx is " + influx);
		}

		if (arg instanceof SimulationStartedEvent)
		{
			sim = ((SimulationStartedEvent) arg).getSimNumber() + 1;
			iS = ((SimulationStartedEvent) arg).getiS();
			iN = ((SimulationStartedEvent) arg).getiN();
			w = ((SimulationStartedEvent) arg).getW();
			m = ((SimulationStartedEvent) arg).getM();

			showInNo.setText(""+iN);
			showInSz.setText(""+iS);
			showSim.setText("Sim: " + sim);
			paraW.setText("w: " + w);
			paraM.setText("m: " + df3.format(m));
		}

	}

}
