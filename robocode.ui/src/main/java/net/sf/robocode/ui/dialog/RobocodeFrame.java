/**
 * Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import net.sf.robocode.battle.IBattleManager;
import net.sf.robocode.io.Logger;
import net.sf.robocode.recording.IRecordManager;
import net.sf.robocode.settings.ISettingsListener;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.BrowserManager;
import net.sf.robocode.ui.IRobotDialogManager;
import net.sf.robocode.ui.IWindowManager;
import net.sf.robocode.ui.IWindowManagerExt;
import net.sf.robocode.ui.RobotDialogManager;
import net.sf.robocode.ui.battleview.BattleView;
import net.sf.robocode.ui.battleview.InteractiveHandler;
import net.sf.robocode.ui.battleview.PreferredSizeMode;
import net.sf.robocode.ui.battleview.ScreenshotUtil;
import net.sf.robocode.ui.gfx.ImageUtil;
import net.sf.robocode.version.IVersionManager;
import net.sf.robocode.version.Version;
import robocode.control.events.BattleAdaptor;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.BattleFinishedEvent;
import robocode.control.events.BattlePausedEvent;
import robocode.control.events.BattleResumedEvent;
import robocode.control.events.BattleStartedEvent;
import robocode.control.events.RoundStartedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.snapshot.IRobotSnapshot;
import robocode.control.snapshot.ITurnSnapshot;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static net.sf.robocode.ui.util.ShortcutUtil.MENU_SHORTCUT_KEY_MASK;


/**
 * @author Mathew Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Matthew Reeder (contributor)
 * @author Luis Crespo (contributor)
 * @author Pavel Savara (contributor)
 */
@SuppressWarnings("serial")
public class RobocodeFrame extends JFrame implements ISettingsListener {

	private final static String ROBOCODE_TITLE = "RobocodeGL";

	private static final int SLOW_MO_WAIT_MS = 400;

	private final static int MAX_TPS = 10000;
	private final static int MAX_TPS_SLIDER_VALUE = 61;

	private final static int UPDATE_TITLE_INTERVAL = 100; // 500; // milliseconds
	private final static String INSTALL_URL = "https://robocode.sourceforge.io/installer";

	private static final Cursor BUSY_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	private static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

	private final EventHandler eventHandler = new EventHandler();
	private BattleObserver battleObserver;

	private final InteractiveHandler interactiveHandler;

	private JPanel robocodeContentPane;
	private JLabel statusLabel;

	private JScrollPane robotButtonsScrollPane;

	private JPanel mainPanel;
	private JPanel battleViewPanel;
	private JPanel sidePanel;
	private JPanel robotButtonsPanel;

	private JToolBar toolBar;

	private JToggleButton pauseButton;
	private JButton nextTurnButton;
	private JButton stopButton;
	private JButton restartButton;
	private JButton replayButton;

	private DoubleJSlider tpsSlider;
	private JLabel tpsLabel;

	private boolean iconified;
	private boolean exitOnClose = true;

	private final ISettingsManager properties;

	private final IWindowManagerExt windowManager;
	private final IVersionManager versionManager;
	private final IBattleManager battleManager;
	private final IRobotDialogManager dialogManager;
	private final IRecordManager recordManager;
	private final BattleView battleView;
	private final MenuBar menuBar;

	final List<RobotButton> robotButtons = new ArrayList<RobotButton>();
	private FileDropHandler fileDropHandler;

	private final Timer longPressTimer;
	private long spacePressedTime;

	public RobocodeFrame(ISettingsManager properties,
	                     IWindowManager windowManager,
	                     IRobotDialogManager dialogManager,
	                     IVersionManager versionManager,
	                     final IBattleManager battleManager,
	                     IRecordManager recordManager,
	                     InteractiveHandler interactiveHandler,
	                     MenuBar menuBar,
	                     BattleView battleView
			) {
		this.windowManager = (IWindowManagerExt) windowManager;
		this.properties = properties;
		this.interactiveHandler = interactiveHandler;
		this.versionManager = versionManager;
		this.battleManager = battleManager;
		this.dialogManager = dialogManager;
		this.recordManager = recordManager;
		this.battleView = battleView;
		this.menuBar = menuBar;
		menuBar.setup(this);
		initialize();

		longPressTimer = new Timer(16, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (spacePressedTime == -1) {
					return;
				}
				if (System.nanoTime() - spacePressedTime > SLOW_MO_WAIT_MS * 1000000) {
					battleManager.resumeIfPausedBattle();
				}
			}
		});
	}

	protected void finalize() throws Throwable {
		try {
			windowManager.removeBattleListener(battleObserver);
		} finally {
			super.finalize();
		}
	}

	public void setBusyPointer(boolean enabled) {
		setCursor(enabled ? BUSY_CURSOR : DEFAULT_CURSOR);
	}

	private void clearRobotButtons() {
		// menuBar.getBattleRobotListMenu().removeAll();
		// menuBar.getBattleRobotListMenu().add(menuBar.getBattleRobotListEmptyMenuItem());

		for (RobotButton robotButton : robotButtons) {
			robotButton.detach();
		}
		robotButtons.clear();
	}

	private void addRobotButton(RobotButton b) {
		JMenuItem robotMenuItem = new JMenuItem(b.getText());
		robotMenuItem.addActionListener(b);
		int keyIndex = b.getRobotIndex();
		if (keyIndex < 10) {
			if (keyIndex == 9) {
				keyIndex = -1;
			}
			robotMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + keyIndex, MENU_SHORTCUT_KEY_MASK, false));
		}
		menuBar.getBattleRobotListMenu().add(robotMenuItem);

		robotButtons.add(b);
		getRobotButtonsPanel().add(b);
		b.setVisible(true);
		getRobotButtonsPanel().validate();
	}

	public void checkUpdateOnStart() {
		if (!isIconified()) {
			Date lastCheckedDate = properties.getVersionChecked();

			Date today = new Date();

			if (lastCheckedDate == null) {
				lastCheckedDate = today;
				properties.setVersionChecked(lastCheckedDate);
				properties.saveProperties();
			}
			Calendar checkDate = Calendar.getInstance();

			checkDate.setTime(lastCheckedDate);
			checkDate.add(Calendar.DATE, 5);

			if (checkDate.getTime().before(today) && checkForNewVersion(false)) {
				properties.setVersionChecked(today);
				properties.saveProperties();
			}
		}
	}

	public boolean checkForNewVersion(boolean notifyNoUpdate) {
		String currentVersion = versionManager.getVersion();
		String newVersion = versionManager.checkForNewVersion();

		boolean newVersionAvailable = false;

		if (newVersion != null && currentVersion != null) {
			if (Version.compare(newVersion, currentVersion) > 0) {
				newVersionAvailable = true;
				if (Version.isFinal(newVersion)
						|| (Version.isBeta(newVersion) && properties.getOptionsCommonNotifyAboutNewBetaVersions())) {
					showNewVersion(newVersion, false);
				}
			}
		}
		if (notifyNoUpdate && !newVersionAvailable) {
			showLatestVersion(currentVersion);
		}
		return true;
	}

	public void takeScreenshot() {
		setBusyPointer(true);
		try {
			ScreenshotUtil.saveScreenshot(battleView.getScreenshot(), "PNG", 1);
		} finally {
			setBusyPointer(false);
		}
	}

	private void showLatestVersion(String version) {
		JOptionPane.showMessageDialog(this, "You have version " + version + ".  This is the latest version of Robocode.",
				"No update available", JOptionPane.INFORMATION_MESSAGE);
	}

	private void showNewVersion(String newVersion, boolean dialog) {
		Logger.logMessage("Version " + newVersion + " of Robocode is now available. ");

		if (!dialog) return;

		if (JOptionPane.showConfirmDialog(this,
				"Version " + newVersion + " of Robocode is now available.  Would you like to download it?",
				"Version " + newVersion + " available", JOptionPane.YES_NO_OPTION)
				== JOptionPane.YES_OPTION) {
			try {
				BrowserManager.openURL(INSTALL_URL);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Unable to open browser!",
						JOptionPane.INFORMATION_MESSAGE);
			}
		} else if (Version.isFinal(newVersion)) {
			JOptionPane.showMessageDialog(this,
					"It is highly recommended that you always download the latest version.  You may get it at " + INSTALL_URL,
					"Update when you can!", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Rather than use a layout manager for the battleview panel, we just
	 * calculate the proper aspect ratio and set the battleview's size. We could
	 * use a layout manager if someone wants to write one...
	 */
	private void battleViewPanelResized() {
		battleView.setBounds(getBattleViewPanel().getBounds());
	}

	/**
	 * Return the MainPanel (which contains the BattleView and the robot
	 * buttons)
	 *
	 * @return JPanel
	 */
	private JPanel getMainPanel() {
		if (mainPanel == null) {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.add(getSidePanel(), BorderLayout.EAST);
			mainPanel.add(getBattleViewPanel());
		}
		return mainPanel;
	}

	/**
	 * Return the BattleViewMainPanel (which contains the BattleView and a
	 * spacer)
	 *
	 * @return JPanel
	 */
	private JPanel getBattleViewPanel() {
		if (battleViewPanel == null) {
			battleViewPanel = new JPanel();
			battleViewPanel.setBackground(Color.BLACK);
			// battleViewPanel.setPreferredSize(new Dimension(800, 600));
			// battleViewPanel.setLayout(null);
			battleViewPanel.setLayout(new BorderLayout());
			battleViewPanel.add(battleView);
			battleViewPanel.addComponentListener(eventHandler);
		}
		return battleViewPanel;
	}

	/**
	 * Return the JFrameContentPane.
	 *
	 * @return JPanel
	 */
	private JPanel getRobocodeContentPane() {
		if (robocodeContentPane == null) {
			robocodeContentPane = new JPanel();
			robocodeContentPane.setLayout(new BorderLayout());
			robocodeContentPane.add(getToolBar(), "South");
			robocodeContentPane.add(getMainPanel(), "Center");
		}
		return robocodeContentPane;
	}

	/**
	 * Return the sidePanel.
	 *
	 * @return JPanel
	 */
	private JPanel getSidePanel() {
		if (sidePanel == null) {
			sidePanel = new JPanel();
			sidePanel.setLayout(new BorderLayout());
			sidePanel.add(getRobotButtonsScrollPane(), BorderLayout.CENTER);
			final BattleButton btn = net.sf.robocode.core.Container.getComponent(BattleButton.class);

			menuBar.getBattleMainBattleMenuItem().addActionListener(btn);

			btn.attach();
			sidePanel.add(btn, BorderLayout.SOUTH);
		}
		return sidePanel;
	}

	/**
	 * Return the robotButtons panel.
	 *
	 * @return JPanel
	 */
	private JPanel getRobotButtonsPanel() {
		if (robotButtonsPanel == null) {
			robotButtonsPanel = new JPanel();
			robotButtonsPanel.setLayout(new BoxLayout(robotButtonsPanel, BoxLayout.Y_AXIS));
			robotButtonsPanel.addContainerListener(eventHandler);
		}
		return robotButtonsPanel;
	}

	/**
	 * Return the robotButtonsScrollPane
	 *
	 * @return JScrollPane
	 */
	private JScrollPane getRobotButtonsScrollPane() {
		if (robotButtonsScrollPane == null) {
			robotButtonsScrollPane = new JScrollPane();
			robotButtonsScrollPane.setBorder(BorderFactory.createEmptyBorder());
			robotButtonsScrollPane.setAutoscrolls(false);
			robotButtonsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			robotButtonsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			robotButtonsScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
			robotButtonsScrollPane.setMaximumSize(new Dimension(113, 32767));
			robotButtonsScrollPane.setPreferredSize(new Dimension(113, 28));
			robotButtonsScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
			robotButtonsScrollPane.setMinimumSize(new Dimension(113, 53));
			robotButtonsScrollPane.setViewportView(getRobotButtonsPanel());
		}
		return robotButtonsScrollPane;
	}

	/**
	 * Return the statusLabel
	 *
	 * @return JLabel
	 */
	public JLabel getStatusLabel() {
		if (statusLabel == null) {
			statusLabel = new JLabel();
			statusLabel.setText("");
		}
		return statusLabel;
	}

	/**
	 * Return the pauseButton
	 *
	 * @return JToggleButton
	 */
	private JToggleButton getPauseButton() {
		if (pauseButton == null) {
			pauseButton = new JToggleButton("Pause/Debug");
			pauseButton.setMnemonic('P');
			pauseButton.setHorizontalTextPosition(SwingConstants.CENTER);
			pauseButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			pauseButton.addActionListener(eventHandler);
		}
		return pauseButton;
	}

	/**
	 * Return the nextTurnButton
	 *
	 * @return JButton
	 */
	private Component getNextTurnButton() {
		if (nextTurnButton == null) {
			nextTurnButton = new JButton("Next Turn");
			nextTurnButton.setMnemonic('N');
			nextTurnButton.setHorizontalTextPosition(SwingConstants.CENTER);
			nextTurnButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			nextTurnButton.addActionListener(eventHandler);

			nextTurnButton.setEnabled(false);
		}
		return nextTurnButton;
	}

	/**
	 * Return the stopButton
	 *
	 * @return JButton
	 */
	private JButton getStopButton() {
		if (stopButton == null) {
			stopButton = new JButton("Stop");
			stopButton.setMnemonic('S');
			stopButton.setHorizontalTextPosition(SwingConstants.CENTER);
			stopButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			stopButton.addActionListener(eventHandler);

			stopButton.setEnabled(false);
		}
		return stopButton;
	}

	/**
	 * Return the restartButton
	 *
	 * @return JButton
	 */
	private JButton getRestartButton() {
		if (restartButton == null) {
			restartButton = new JButton("Restart");
			restartButton.setMnemonic('t');
			restartButton.setDisplayedMnemonicIndex(3);
			restartButton.setHorizontalTextPosition(SwingConstants.CENTER);
			restartButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			restartButton.addActionListener(eventHandler);

			restartButton.setEnabled(false);
		}
		return restartButton;
	}

	/**
	 * Return the replayButton
	 *
	 * @return JButton
	 */
	public JButton getReplayButton() {
		if (replayButton == null) {
			replayButton = new JButton("Replay");
			replayButton.setMnemonic('y');
			replayButton.setDisplayedMnemonicIndex(5);
			replayButton.setHorizontalTextPosition(SwingConstants.CENTER);
			replayButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			replayButton.addActionListener(eventHandler);

			ISettingsManager props = properties;

			replayButton.setVisible(props.getOptionsCommonEnableReplayRecording());

			props.addPropertyListener(new ISettingsListener() {
				public void settingChanged(String property) {
					if (property.equals(ISettingsManager.OPTIONS_COMMON_ENABLE_REPLAY_RECORDING)) {
						replayButton.setVisible(properties.getOptionsCommonEnableReplayRecording());
					}
				}
			});

			replayButton.setEnabled(false);
		}
		return replayButton;
	}

	/**
	 * Return the tpsSlider
	 *
	 * @return JSlider
	 */
	private DoubleJSlider getTpsSlider() {
		if (tpsSlider == null) {
			final ISettingsManager props = properties;

			int tps = Math.max(props.getOptionsBattleDesiredTPS(), 1);
			
			int scale = 100;
			tpsSlider = new DoubleJSlider(0, MAX_TPS_SLIDER_VALUE, tpsToSliderValue(tps), scale);
			tpsSlider.setPaintLabels(true);
			tpsSlider.setPaintTicks(true);
			tpsSlider.setMinorTickSpacing(1 * scale);

			tpsSlider.addChangeListener(eventHandler);

			Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();

			for (int i = 0; i < 61; ++i) {
				int v = i * 5;
				labels.put(v * scale, new JLabel(String.format("%.0f", sliderToTps(v))));
			}

			tpsSlider.setMajorTickSpacing(5 * scale);
			tpsSlider.setLabelTable(labels);

			WindowUtil.setFixedSize(tpsSlider, new Dimension((MAX_TPS_SLIDER_VALUE + 1) * 6, 40));

			props.addPropertyListener(new ISettingsListener() {
				public void settingChanged(String property) {
					if (property.equals(ISettingsManager.OPTIONS_BATTLE_DESIREDTPS)) {
						setTpsOnSlider(props.getOptionsBattleDesiredTPS());
					}
				}
			});
		}
		return tpsSlider;
	}

	/**
	 * Return the tpsLabel
	 *
	 * @return JLabel
	 */
	private JLabel getTpsLabel() {
		if (tpsLabel == null) {
			tpsLabel = new JLabel(getTpsFromSliderAsString());
		}
		return tpsLabel;
	}

	/**
	 * Return the toolBar.
	 *
	 * @return JToolBar
	 */
	private JToolBar getToolBar() {
		if (toolBar == null) {
			toolBar = new JToolBar();
			toolBar.add(getPauseButton());
			toolBar.add(getNextTurnButton());
			toolBar.add(getStopButton());
			toolBar.add(getRestartButton());
			toolBar.add(getReplayButton());

			toolBar.addSeparator();

			toolBar.add(getTpsSlider());
			toolBar.add(getTpsLabel());

			toolBar.addSeparator();

			toolBar.add(getStatusLabel());
			WindowUtil.setDefaultStatusLabel(getStatusLabel());
		}
		return toolBar;
	}

	/**
	 * Initialize the class.
	 */
	private void initialize() {
		try {
			Class<?> util = Class.forName("com.apple.eawt.FullScreenUtilities");
			Method method = util.getMethod("setWindowCanFullScreen", Window.class, Boolean.TYPE);
			method.invoke(util, this, true);
		} catch (Exception ignore) {
			// no full screen support
		}

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setTitle(ROBOCODE_TITLE);
		setIconImage(ImageUtil.getImage("/net/sf/robocode/ui/icons/robocode-icon.png"));
		setResizable(true);
		setVisible(false);

		setContentPane(getRobocodeContentPane());
		setJMenuBar(menuBar);

		battleObserver = new BattleObserver();

		addWindowListener(eventHandler);

		interactiveHandler.setScaleProvider(battleView);

		Component battleViewComp = battleView.init();

		battleViewComp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiersEx() == 0) {
					if (spacePressedTime == -1) {
						pauseOrNextTurn();
						battleManager.setSlowMoMode(true);
						spacePressedTime = System.nanoTime();
						longPressTimer.start();
					}
					e.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiersEx() == 0) {
					if (spacePressedTime != -1) {
						stopSlowMoMode();
					}
					e.consume();
				}
			}
		});

		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				if (spacePressedTime != -1) {
					stopSlowMoMode();
				}
			}
		});

		battleViewComp.addMouseListener(interactiveHandler);
		battleViewComp.addMouseMotionListener(interactiveHandler);
		battleViewComp.addMouseWheelListener(interactiveHandler);
		battleViewComp.setFocusable(true);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(interactiveHandler);

		if (windowManager.isSlave()) {
			menuBar.getBattleMenu().setEnabled(false);
			menuBar.getRobotMenu().setEnabled(false);
			setEnableStopButton(false);
			setEnablePauseButton(false);
			setEnableNextTurnButton(false, true);
			setEnableRestartButton(false);
			getReplayButton().setEnabled(false);
			exitOnClose = false;
		}

		fileDropHandler = new FileDropHandler();

		this.setTransferHandler(fileDropHandler);


		setControlsVisible(!properties.getOptionsUiHideControls());

		properties.addPropertyListener(this);
	}

	private void stopSlowMoMode() {
		spacePressedTime = -1;
		longPressTimer.stop();
		battleManager.setSlowMoMode(false);
		battleManager.pauseIfResumedBattle();
		windowManager.signalPauseBattle();
	}

	private void setEnableNextTurnButton(boolean b, boolean setMenu) {
		getNextTurnButton().setEnabled(b);
		if (setMenu) {
			menuBar.getBattleNextTurnMenuItem().setEnabled(b);
		}
	}

	private void setEnablePauseButton(boolean b) {
		getPauseButton().setEnabled(b);
	}

	private void pauseResumeButtonActionPerformed() {
		togglePause();
	}

	/**
	 * Gets the iconified.
	 *
	 * @return Returns a boolean
	 */
	public boolean isIconified() {
		return iconified;
	}

	public void afterIntroBattle() {
		setEnableRestartButton(false);
		getRobotButtonsPanel().removeAll();
		getRobotButtonsPanel().repaint();
	}

	/**
	 * Sets the iconified.
	 *
	 * @param iconified The iconified to set
	 */
	private void setIconified(boolean iconified) {
		this.iconified = iconified;
	}

	private double getTpsFromSlider() {
		final double value = getTpsSlider().getScaledValue();

		return sliderToTps(value);
	}

	private double sliderToTps(double value) {
		return linearTicksMapping(value, sliderValues, tpsValues);
	}

	private void setTpsOnSlider(int tps) {
		tpsSlider.setScaledValue(tpsToSliderValue(tps));
	}
	
	private final double[] sliderValues = new double[]{
		0, 30, 40, 45, 53,  54,  56,  57,  58,  59,  60.9, 60.9001, MAX_TPS_SLIDER_VALUE,
	};
	private final double[] tpsValues = new double[]{
		0, 30, 50, 65, 100, 110, 150, 200, 300, 500, 1000, MAX_TPS, MAX_TPS,
	};
	
	private double linearTicksMapping(double x, double[] xs, double[] ys) {
		int n = xs.length;
		if (x < xs[0]) {
			return ys[0];
		}
		
		for (int i = 1; i < n; i++) {
			double x1 = xs[i];
			if (x < x1) {
				double x0 = xs[i - 1];
				double y0 = ys[i - 1];
				double y1 = ys[i];
				return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
			}
		}
		return ys[n - 1];
	}

	private double tpsToSliderValue(double tps) {
		return linearTicksMapping(tps, tpsValues, sliderValues);

		// if (tps <= 30) {
		// 	return 0 + (tps - 0) / 1;
		// }
		// if (tps <= 50) {
		// 	return 30 + (tps - 30) / 2;
		// }
		// if (tps <= 65) {
		// 	return 40 + (tps - 50) / 3;
		// }
		// if (tps <= 100) {
		// 	return 45 + (tps - 65) / 5;
		// }
		// if (tps <= 110) {
		// 	return 53 + (tps - 100) / 10;
		// }
		// if (tps <= 150) {
		// 	return 54 + (tps - 110) / 20;
		// }
		// if (tps <= 200) {
		// 	return 56 + (tps - 150) / 50;
		// }
		// if (tps <= 300) {
		// 	return 57 + (tps - 200) / 100;
		// }
		// if (tps <= 500) {
		// 	return 58 + (tps - 300) / 200;
		// }
		// if (tps <= 1000) {
		// 	return 59 + (tps - 500) / 250;
		// }
		// return MAX_TPS_SLIDER_VALUE;
	}

	private String getTpsFromSliderAsString() {
		int tps = (int) Math.round(getTpsFromSlider());

		return formatTPS(tps);
	}

	private String formatTPS(int tps) {
		return "  " + ((tps >= MAX_TPS) ? "max" : "" + tps) + "  ";
	}

	public FileDropHandler getFileDropHandler() {
		return fileDropHandler;
	}

	public void resetRobocodeFrameSize(final PreferredSizeMode preferredSizeMode) {
		if (getExtendedState() == MAXIMIZED_BOTH) {
			return;
		}

		clearPreferredSize();
		setPreferredSizeMode(preferredSizeMode);
		pack();
	}

	public void onHideControlsChange() {
		boolean hide = properties.getOptionsUiHideControls();
		if (isControlsVisible() == !hide) {
			return;
		}

		clearPreferredSize();
		getBattleViewPanel().setPreferredSize(battleView.getSize());

		battleView.setVisible(false);

		setControlsVisible(!hide);

		if (getExtendedState() != MAXIMIZED_BOTH) {
			pack();
		}

		getBattleViewPanel().setPreferredSize(null);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				battleView.setVisible(true);
			}
		});
	}

	@Override
	public void settingChanged(String property) {
		onHideControlsChange();
	}

	public void togglePause() {
		if (battleManager.togglePauseResumeBattle()) {
			windowManager.signalPauseBattle();
		}
	}

	public void pauseOrNextTurn() {
		if (!battleManager.isPaused()) {
			windowManager.pauseBattle();
		} else {
			nextTurn();
		}
	}

	private void nextTurn() {
		battleManager.nextTurn();
		windowManager.signalNextTurn();
	}

	private class EventHandler implements ComponentListener, ActionListener, ContainerListener, WindowListener,
			ChangeListener {

		public void actionPerformed(ActionEvent e) {
			final Object source = e.getSource();

			if (source == getPauseButton()) {
				pauseResumeButtonActionPerformed();
			} else if (source == getStopButton()) {
				stopBattleAsync();
			} else if (source == getRestartButton()) {
				restartBattleAsync();
			} else if (source == getNextTurnButton()) {
				nextTurn();
			} else if (source == getReplayButton()) {
				battleManager.replay();
			}
		}

		public void componentResized(ComponentEvent e) {
			// if (e.getSource() == getBattleViewPanel()) {
			// 	battleViewPanelResized();
			// }
		}

		public void componentShown(ComponentEvent e) {}

		public void componentHidden(ComponentEvent e) {}

		public void componentRemoved(ContainerEvent e) {}

		public void componentAdded(ContainerEvent e) {}

		public void componentMoved(ComponentEvent e) {}

		public void windowActivated(WindowEvent e) {}

		public void windowClosed(WindowEvent e) {
			if (exitOnClose) {
				System.exit(0);
			}
		}

		public void windowClosing(WindowEvent e) {
			exitOnClose = true;
			if (windowManager.isSlave()) {
				WindowUtil.message("If you wish to exit Robocode, please exit the program controlling it.");
				exitOnClose = false;
				return;
			}
			saveAndDispose();
		}

		public void windowDeactivated(WindowEvent e) {}

		public void windowDeiconified(WindowEvent e) {
			setIconified(false);
			battleManager.setManagedTPS(true);
		}

		public void windowIconified(WindowEvent e) {
			setIconified(true);
			battleManager.setManagedTPS(properties.getOptionsViewPreventSpeedupWhenMinimized());
		}

		public void windowOpened(WindowEvent e) {
			battleManager.setManagedTPS(true);
		}

		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == getTpsSlider()) {
				setTPS((int) Math.round(getTpsFromSlider()), false);
			}
		}
	}

	public void restartBattleAsync() {
		setEnableRestartButton(false);
		setEnableStopButton(false);
		windowManager.signalStopBattle();
		battleManager.restart();
	}

	public void stopBattleAsync() {
		setEnableRestartButton(false);
		setEnableStopButton(false);
		battleManager.stopAsync(true).then(new Runnable() {
			@Override
			public void run() {
				updateRestartButtonStatus();
			}
		});
		windowManager.signalStopBattle();
	}

	public void saveAndDispose() {
		properties.saveProperties();

		if (windowManager.closeRobocodeEditor()) {
			WindowUtil.saveWindowPositions();
			battleObserver = null;
			dispose();
		}
	}

	public void setTPS(int tps, boolean setSlider) {
		// TODO refactor
		if (tps == 0) {
			battleManager.pauseIfResumedBattle();
		} else {
			// Only set desired TPS if it is not set to zero
			properties.setOptionsBattleDesiredTPS(tps);
			battleManager.resumeIfPausedBattle(); // TODO causing problems when called from PreferencesViewOptionsTab.storePreferences()
		}

		tpsLabel.setText(formatTPS(tps));
		if (setSlider) {
			setTpsOnSlider(tps);
		}
	}

	private class BattleObserver extends BattleAdaptor {
		private int tps;
		private int currentRound;
		private int numberOfRounds;
		private int currentTurn;
		private boolean isBattleRunning;
		private boolean isBattlePaused;
		private boolean isBattleReplay;
		private long lastTitleUpdateTime;

		public BattleObserver() {
			windowManager.addBattleListener(this);
		}

		protected void finalize() throws Throwable {
			try {
				windowManager.removeBattleListener(this);
			} finally {
				super.finalize();
			}
		}

		@Override
		public void onBattleStarted(BattleStartedEvent event) {
			numberOfRounds = event.getBattleRules().getNumRounds();
			isBattleRunning = true;
			isBattleReplay = event.isReplay();

			tps = 0;
			currentRound = 0;
			currentTurn = -1;

			setEnableStopButton(true);
			updateRestartButtonStatus();
			getReplayButton().setEnabled(event.isReplay());
			menuBar.getBattleSaveRecordAsMenuItem().setEnabled(false);
			menuBar.getBattleExportRecordMenuItem().setEnabled(false);
			menuBar.getBattleSaveAsMenuItem().setEnabled(true);
			menuBar.getBattleSaveMenuItem().setEnabled(true);
			menuBar.getBattleRestartMenuItem().setEnabled(true);

			JCheckBoxMenuItem rankingCheckBoxMenuItem = menuBar.getOptionsShowRankingCheckBoxMenuItem();

			rankingCheckBoxMenuItem.setEnabled(!isBattleReplay);
			if (rankingCheckBoxMenuItem.isSelected()) {
				windowManager.showRankingDialog(!isBattleReplay);
			}

			validate();

			updateTitle();
		}

		public void onRoundStarted(final RoundStartedEvent event) {
			if (event.getRound() == 0) {
				getRobotButtonsPanel().removeAll();
				menuBar.getBattleRobotListMenu().removeAll();

				final List<IRobotSnapshot> robots = Arrays.asList(event.getStartSnapshot().getRobots());

				dialogManager.trim(robots);

				int maxEnergy = 0;

				for (IRobotSnapshot robot : robots) {
					if (maxEnergy < robot.getEnergy()) {
						maxEnergy = (int) robot.getEnergy();
					}
				}
				if (maxEnergy == 0) {
					maxEnergy = 1;
				}
				for (int index = 0; index < robots.size(); index++) {
					final IRobotSnapshot robot = robots.get(index);
					final boolean attach = index < RobotDialogManager.MAX_PRE_ATTACHED;
					final RobotButton button = net.sf.robocode.core.Container.createComponent(RobotButton.class);

					button.setup(robot.getName(), maxEnergy, index, robot.getContestantIndex(), robot.getTeamIndex(), attach);
					button.setText(robot.getShortName());
					addRobotButton(button);
				}
				getRobotButtonsPanel().repaint();
			}
		}

		@Override
		public void onBattleFinished(BattleFinishedEvent event) {
			isBattleRunning = false;

			clearRobotButtons();

			final boolean canReplayRecord = recordManager.hasRecord();
			final boolean enableSaveRecord = (properties.getOptionsCommonEnableReplayRecording() & canReplayRecord);

			setEnableStopButton(false);
			getReplayButton().setEnabled(canReplayRecord);
			setEnableNextTurnButton(false, true);

			menuBar.getBattleRestartMenuItem().setEnabled(false);
			menuBar.getBattleSaveRecordAsMenuItem().setEnabled(enableSaveRecord);
			menuBar.getBattleExportRecordMenuItem().setEnabled(enableSaveRecord);
			menuBar.getOptionsShowRankingCheckBoxMenuItem().setEnabled(false);

			updateTitle();
		}

		@Override
		public void onBattlePaused(BattlePausedEvent event) {
			isBattlePaused = true;

			setPauseButtonSelected(true);
			setEnableNextTurnButton(true, true);

			updateTitle();
		}

		@Override
		public void onBattleResumed(BattleResumedEvent event) {
			isBattlePaused = false;

			setPauseButtonSelected(false);
			setEnableNextTurnButton(false, false);

			// TODO: Refactor?
			if (getTpsFromSlider() == 0) {
				setTpsOnSlider(1);
			}

			updateTitle();
		}

		public void onTurnEnded(TurnEndedEvent event) {
			if (event == null) {
				return;
			}
			final ITurnSnapshot turn = event.getTurnSnapshot();

			if (turn == null) {
				return;
			}

			tps = event.getTurnSnapshot().getTPS();
			currentRound = event.getTurnSnapshot().getRound();
			currentTurn = event.getTurnSnapshot().getTurn();

			// Only update every half second to spare CPU cycles
			if (isBattlePaused || (System.currentTimeMillis() - lastTitleUpdateTime) >= UPDATE_TITLE_INTERVAL) {
				updateTitle();
			}
		}

		private void updateTitle() {
			StringBuffer title = new StringBuffer(ROBOCODE_TITLE);

			if (isBattleRunning) {
				title.append(": ");

				if (currentTurn == -1) {
					title.append("Before start");
				} else if (currentTurn == 0) {
					title.append("Starting round");
				} else {
					if (isBattleReplay) {
						title.append("Replaying: ");
					}
					title.append("Turn ");
					title.append(currentTurn);

					title.append(", Round ");
					title.append(currentRound + 1).append(" of ").append(numberOfRounds);

					if (!isBattlePaused) {
						boolean dispTps = properties.getOptionsViewTPS();
						boolean dispFps = properties.getOptionsViewFPS();

						if (dispTps | dispFps) {
							title.append(", ");

							if (dispTps) {
								title.append(tps).append(" TPS");
							}
							if (dispTps & dispFps) {
								title.append(", ");
							}
							if (dispFps) {
								title.append(windowManager.getFPS()).append(" FPS");
							}
						}
					}
				}
			}
			if (isBattlePaused) {
				title.append(" (paused)");
			}

			MemoryUsage memUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

			long usedMem = memUsage.getUsed() / (1024 * 1024);

			title.append(", Used mem: ").append(usedMem);

			long maxMem = memUsage.getMax();

			if (maxMem >= 0) {
				maxMem /= (1024 * 1024);
				title.append(" of ").append(maxMem);
			}
			title.append(" MB");

			setTitle(title.toString());

			lastTitleUpdateTime = System.currentTimeMillis();
		}

		@Override
		public void onBattleCompleted(BattleCompletedEvent event) {
			if (windowManager.isShowResultsEnabled()) {
				// show on ATW thread
				ResultsTask resultTask = new ResultsTask(event);

				EventQueue.invokeLater(resultTask);
			}
		}

		private class ResultsTask implements Runnable {
			final BattleCompletedEvent event;

			ResultsTask(BattleCompletedEvent event) {
				this.event = event;
			}

			public void run() {
				windowManager.showResultsDialog(event);
			}
		}
	}

	private void setPauseButtonSelected(boolean paused) {
		// menuBar.getBattleTogglePauseMenuItem().setState(paused);
		getPauseButton().setSelected(paused);
	}

	private void setEnableRestartButton(boolean b) {
		menuBar.getBattleRestartMenuItem().setEnabled(b);
		getRestartButton().setEnabled(b);
	}

	private void setEnableStopButton(boolean b) {
		menuBar.getBattleStopMenuItem().setEnabled(b);
		getStopButton().setEnabled(b);
	}

	public void updateRestartButtonStatus() {
		String robots = battleManager.getBattleProperties().getSelectedRobots();
		boolean canRestart = robots != null && robots.length() > 0;
		setEnableRestartButton(canRestart);
	}

	public void clearPreferredSize() {
		for (Component c = battleView; c.getParent() != null; c = c.getParent()) {
			c.getParent().setPreferredSize(null);
		}
	}

	public void setPreferredSizeMode(PreferredSizeMode b) {
		battleView.setPreferredSizeMode(b);
	}

	public void toggleControlsVisible() {
		setControlsVisible(!isControlsVisible());
	}

	public void setControlsVisible(boolean b) {
		getToolBar().setVisible(b);
		getSidePanel().setVisible(b);
	}

	public boolean isControlsVisible() {
		return getToolBar().isVisible() && getSidePanel().isVisible();
	}
}
