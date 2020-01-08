/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui.dialog;


import net.sf.robocode.async.Promise;
import net.sf.robocode.battle.IBattleManager;
import net.sf.robocode.host.ICpuManager;
import net.sf.robocode.recording.BattleRecordFormat;
import net.sf.robocode.recording.IRecordManager;
import net.sf.robocode.serialization.SerializableOptions;
import net.sf.robocode.settings.ISettingsListener;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.IWindowManagerExt;
import net.sf.robocode.ui.battleview.PreferredSizeMode;
import net.sf.robocode.ui.editor.IRobocodeEditor;
import net.sf.robocode.ui.mac.MacMenuHandler;
import net.sf.robocode.ui.mac.MacMenuHelper;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static net.sf.robocode.ui.util.ShortcutUtil.MENU_SHORTCUT_KEY_MASK;


/**
 * Handles menu display and interaction for Robocode.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Matthew Reeder (contributor)
 * @author Luis Crespo (contributor)
 */
@SuppressWarnings({"serial", "InstanceVariableUsedBeforeInitialized"})
public final class MenuBar extends JMenuBar implements ISettingsListener {

	// Battle menu
	private JMenu battleMenu;
	private JMenuItem battleNewMenuItem;
	private JMenuItem battleOpenMenuItem;
	private JMenuItem battleSaveMenuItem;
	private JMenuItem battleSaveAsMenuItem;
	private JMenuItem battleRestartMenuItem;
	private JMenuItem battleStopMenuItem;
	private JMenuItem battleTogglePauseMenuItem;
	private JMenuItem battleNextTurnMenuItem;
	private JMenuItem battleExitMenuItem;
	private JMenu battleRobotListMenu;
	private JMenuItem battleRobotListEmptyMenuItem;
	private JMenuItem battleMainBattleMenuItem;
	private JMenuItem battleOpenRecordMenuItem;
	private JMenuItem battleSaveRecordAsMenuItem;
	private JMenuItem battleExportRecordMenuItem;
	private JMenuItem battleImportRecordMenuItem;
	private JMenuItem battleTakeScreenshotMenuItem;

	// Robot menu
	private JMenu robotMenu;
	private JMenuItem robotEditorMenuItem;
	private JMenuItem robotImportMenuItem;
	private JMenuItem robotPackagerMenuItem;
	private JMenuItem robotCreateTeamMenuItem;

	// Options menu
	private JMenu optionsMenu;
	private JMenuItem optionsPreferencesMenuItem;
	private JMenuItem optionsAdjustTPSMenuItem;
	private JCheckBoxMenuItem optionsHideControlsMenuItem;
	private JMenuItem optionsFitWindowMenuItem;
	private JMenuItem optionsFitBattleFieldMenuItem;
	private JCheckBoxMenuItem optionsShowRankingCheckBoxMenuItem;
	private JMenuItem optionsRecalculateCpuConstantMenuItem;
	private JMenuItem optionsCleanRobotCacheMenuItem;

	// Help Menu
	private JMenu helpMenu;
	private JMenuItem helpReadMeMenuItem;
	private JMenuItem helpOnlineHelpMenuItem;
	private JMenuItem helpCheckForNewVersionMenuItem;
	private JMenuItem helpVersionsTxtMenuItem;
	private JMenuItem helpRobocodeApiMenuItem;
	private JMenuItem helpJavaDocumentationMenuItem;
	private JMenuItem helpFaqMenuItem;
	private JMenuItem helpAboutMenuItem;
	private JMenuItem helpRobocodeMenuItem;
	private JMenuItem helpRoboWikiMenuItem;
	private JMenuItem helpGoogleGroupRobocodeMenuItem;
	private JMenuItem helpRoboRumbleMenuItem;

	private class EventHandler implements ActionListener, MenuListener {
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			MenuBar mb = MenuBar.this;

			// Battle menu
			if (source == mb.getBattleNewMenuItem()) {
				battleNewActionPerformed();
			} else if (source == mb.getBattleOpenMenuItem()) {
				battleOpenActionPerformed();
			} else if (source == mb.getBattleSaveMenuItem()) {
				battleSaveActionPerformed();
			} else if (source == mb.getBattleSaveAsMenuItem()) {
				battleSaveAsActionPerformed();
			} else if (source == mb.getBattleRestartMenuItem()) {
				battleRestartActionPerformed();
			} else if (source == mb.getBattleStopMenuItem()) {
				battleStopActionPerformed();
			} else if (source == mb.getBattleTogglePauseMenuItem()) {
				battleTogglePauseActionPerformed();
			} else if (source == mb.getBattleNextTurnMenuItem()) {
				battleNextTurnActionPerformed();
			} else if (source == mb.getBattleOpenRecordMenuItem()) {
				battleOpenRecordActionPerformed();
			} else if (source == mb.getBattleImportRecordMenuItem()) {
				battleImportRecordActionPerformed();
			} else if (source == mb.getBattleSaveRecordAsMenuItem()) {
				battleSaveRecordAsActionPerformed();
			} else if (source == mb.getBattleExportRecordMenuItem()) {
				battleExportRecordActionPerformed();
			} else if (source == mb.getBattleTakeScreenshotMenuItem()) {
				battleTakeScreenshotActionPerformed();
			} else if (source == mb.getBattleExitMenuItem()) {
				battleExitActionPerformed();

				// Robot Editor menu
			} else if (source == mb.getRobotEditorMenuItem()) {
				robotEditorActionPerformed();
			} else if (source == mb.getRobotImportMenuItem()) {
				robotImportActionPerformed();
			} else if (source == mb.getRobotPackagerMenuItem()) {
				robotPackagerActionPerformed();

				// Team / Create Team menu
			} else if (source == mb.getRobotCreateTeamMenuItem()) {
				teamCreateTeamActionPerformed();

				// Options / Preferences menu
			} else if (source == mb.getOptionsPreferencesMenuItem()) {
				optionsPreferencesActionPerformed();
			} else if (source == mb.getOptionsAdjustTPSMenuItem()) {
				optionsAdjustTPSActionPerformed();
			} else if (source == mb.getOptionsHideControlsCheckBoxMenuItem()) {
				optionsHideControlsActionPerformed();
			} else if (source == mb.getOptionsFitWindowMenuItem()) {
				optionsFitWindowActionPerformed();
			} else if (source == mb.getOptionsFitBattleFieldMenuItem()) {
				optionsFitBattleFieldActionPerformed();
			} else if (source == mb.getOptionsShowRankingCheckBoxMenuItem()) {
				optionsShowRankingActionPerformed();
			} else if (source == mb.getOptionsRecalculateCpuConstantMenuItem()) {
				optionsRecalculateCpuConstantPerformed();
			} else if (source == mb.getOptionsCleanRobotCacheMenuItem()) {
				optionsCleanRobotCachePerformed();

				// Help menu
			} else if (source == mb.getReadMeMenuItem()) {
				helpReadMeActionPerformed();
			} else if (source == mb.getHelpOnlineHelpMenuItem()) {
				helpOnlineHelpActionPerformed();
			} else if (source == mb.getHelpRobocodeApiMenuItem()) {
				helpRobocodeApiActionPerformed();
			} else if (source == mb.getHelpJavaDocumentationMenuItem()) {
				helpJavaDocumentationActionPerformed();
			} else if (source == mb.getHelpFaqMenuItem()) {
				helpFaqActionPerformed();
			} else if (source == mb.getHelpRobocodeMenuItem()) {
				helpRobocodeHomeMenuItemActionPerformed();
			} else if (source == mb.getHelpRoboWikiMenuItem()) {
				helpRoboWikiMenuItemActionPerformed();
			} else if (source == mb.getHelpGoogleGroupRobocodeMenuItem()) {
				helpGoogleGroupRobocodeActionPerformed();
			} else if (source == mb.getHelpRoboRumbleMenuItem()) {
				helpRoboRumbleActionPerformed();
			} else if (source == mb.getHelpCheckForNewVersionMenuItem()) {
				helpCheckForNewVersionActionPerformed();
			} else if (source == mb.getHelpVersionsTxtMenuItem()) {
				helpVersionsTxtActionPerformed();
			} else if (source == mb.getHelpAboutMenuItem()) {
				helpAboutActionPerformed();
			}
		}

		public void menuDeselected(MenuEvent e) {
			battleManager.resumeBattle();
		}

		public void menuSelected(MenuEvent e) {
			battleManager.pauseBattle();
		}

		public void menuCanceled(MenuEvent e) {}
	}

	public final MenuBar.EventHandler eventHandler = new EventHandler();

	private RobocodeFrame robocodeFrame;
	private final ISettingsManager properties;
	private final IWindowManagerExt windowManager;
	private final IBattleManager battleManager;
	private final IRecordManager recordManager;
	private final ICpuManager cpuManager;

	private final boolean macMenuEnabled;

	public MenuBar(ISettingsManager properties,
			IWindowManagerExt windowManager,
			IBattleManager battleManager,
			IRecordManager recordManager,
			ICpuManager cpuManager) {
		this.properties = properties;
		this.windowManager = windowManager;
		this.battleManager = battleManager;
		this.recordManager = recordManager;
		this.cpuManager = cpuManager;

		if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
			macMenuEnabled = MacMenuHelper.initMacMenu(new MyMacMenuHandler());
		} else {
			macMenuEnabled = false;
		}

		// FNL: Make sure that menus are heavy-weight components so that the menus are not painted
		// behind the BattleView which is a heavy-weight component. This must be done before
		// adding any menu to the menubar.
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

		add(getBattleMenu());
		add(getRobotMenu());
		add(getOptionsMenu());
		add(getHelpMenu());

		properties.addPropertyListener(this);
		settingChanged(null);
	}

	@Override
	public void settingChanged(String ignore) {
		boolean hideControls = properties.getOptionsUiHideControls();

		getOptionsHideControlsCheckBoxMenuItem().setState(hideControls);
	}

	public void setup(RobocodeFrame robocodeFrame) {
		this.robocodeFrame = robocodeFrame;
	}

	private void battleExitActionPerformed() {
		robocodeFrame.saveAndDispose();
	}

	/**
	 * Handle battleNew menu item action
	 */
	private void battleNewActionPerformed() {
		battleManager.setBattleFilename(null);
		windowManager.showNewBattleDialog(battleManager.getBattleProperties());
	}

	private void battleOpenActionPerformed() {
		try {
			battleManager.pauseBattle();

			String path = windowManager.showBattleOpenDialog(".battle", "Battles");
			if (path != null) {
				battleManager.setBattleFilename(path);
				windowManager.showNewBattleDialog(battleManager.loadBattleProperties());
			}
		} finally {
			battleManager.resumeBattle();
		}
	}

	private void battleSaveActionPerformed() {
		try {
			battleManager.pauseBattle();
			String path = battleManager.getBattleFilename();

			if (path == null) {
				path = windowManager.saveBattleDialog(battleManager.getBattlePath(), ".battle", "Battles");
			}
			if (path != null) {
				battleManager.setBattleFilename(path);
				battleManager.saveBattleProperties();
			}
		} finally {
			battleManager.resumeBattle();
		}
	}

	private void battleSaveAsActionPerformed() {
		try {
			battleManager.pauseBattle();

			String path = windowManager.saveBattleDialog(battleManager.getBattlePath(), ".battle", "Battles");

			if (path != null) {
				battleManager.setBattleFilename(path);
				battleManager.saveBattleProperties();
			}
		} finally {
			battleManager.resumeBattle();
		}
	}

	private void battleRestartActionPerformed() {
		robocodeFrame.restartBattleAsync();
	}

	private void battleStopActionPerformed() {
		robocodeFrame.stopBattleAsync();
	}

	private void battleTogglePauseActionPerformed() {
		robocodeFrame.togglePause();
	}

	private void battleNextTurnActionPerformed() {
		robocodeFrame.pauseOrNextTurn();
	}

	private void battleOpenRecordActionPerformed() {
		try {
			battleManager.pauseBattle();

			final String path = windowManager.showBattleOpenDialog(".br", "Records");

			if (path != null) {
				battleManager.stopAsync(true).then(new Runnable() {
					@Override
					public void run() {
						robocodeFrame.getReplayButton().setVisible(true);
						robocodeFrame.getReplayButton().setEnabled(true);

						getBattleSaveRecordAsMenuItem().setEnabled(true);
						getBattleExportRecordMenuItem().setEnabled(true);

						try {
							robocodeFrame.setBusyPointer(true);
							recordManager.loadRecord(path, BattleRecordFormat.BINARY_ZIP);
						} finally {
							robocodeFrame.setBusyPointer(false);
						}
						battleManager.replay();
					}
				});
			}
		} finally {
			battleManager.resumeBattle();
		}
	}

	private void battleImportRecordActionPerformed() {
		try {
			battleManager.pauseBattle();

			final String path = windowManager.showBattleOpenDialog(".br.xml", "XML Records");

			if (path != null) {
				battleManager.stopAsync(true).then(new Runnable() {
					@Override
					public void run() {
						robocodeFrame.getReplayButton().setVisible(true);
						robocodeFrame.getReplayButton().setEnabled(true);

						getBattleSaveRecordAsMenuItem().setEnabled(true);
						getBattleExportRecordMenuItem().setEnabled(true);

						try {
							robocodeFrame.setBusyPointer(true);
							recordManager.loadRecord(path, BattleRecordFormat.XML);
						} finally {
							robocodeFrame.setBusyPointer(false);
						}
						battleManager.replay();
					}
				});
			}
		} finally {
			battleManager.resumeBattle();
		}
	}

	private void battleSaveRecordAsActionPerformed() {
		if (recordManager.hasRecord()) {
			try {
				battleManager.pauseBattle();

				String path = windowManager.saveBattleDialog(battleManager.getBattlePath(), ".br", "Records");

				if (path != null) {
					try {
						robocodeFrame.setBusyPointer(true);
						recordManager.saveRecord(path, BattleRecordFormat.BINARY_ZIP, new SerializableOptions(false));
					} finally {
						robocodeFrame.setBusyPointer(false);
					}
				}
			} finally {
				battleManager.resumeBattle();
			}
		}
	}

	private void battleExportRecordActionPerformed() {
		if (recordManager.hasRecord()) {
			try {
				battleManager.pauseBattle();

				String path = windowManager.saveBattleDialog(battleManager.getBattlePath(), ".br.xml", "XML Records");

				if (path != null) {
					try {
						robocodeFrame.setBusyPointer(true);
						recordManager.saveRecord(path, BattleRecordFormat.XML, new SerializableOptions(false));
					} finally {
						robocodeFrame.setBusyPointer(false);
					}
				}
			} finally {
				battleManager.resumeBattle();
			}
		}
	}

	private void battleTakeScreenshotActionPerformed() {
		robocodeFrame.takeScreenshot();
	}

	private JMenuItem getBattleExitMenuItem() {
		if (battleExitMenuItem == null) {
			battleExitMenuItem = new JMenuItem();
			battleExitMenuItem.setText("Exit");
			battleExitMenuItem.setMnemonic('x');
			battleExitMenuItem.setDisplayedMnemonicIndex(1);
			battleExitMenuItem.addActionListener(eventHandler);
		}
		return battleExitMenuItem;
	}

	public JMenu getBattleMenu() {
		if (battleMenu == null) {
			battleMenu = new JMenu();
			battleMenu.setText("Battle");
			battleMenu.setMnemonic('B');
			battleMenu.add(getBattleNewMenuItem());
			battleMenu.add(getBattleOpenMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleRestartMenuItem());
			battleMenu.add(getBattleStopMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleTogglePauseMenuItem());
			battleMenu.add(getBattleNextTurnMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleRobotListMenu());
			battleMenu.add(getBattleMainBattleMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleSaveMenuItem());
			battleMenu.add(getBattleSaveAsMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleOpenRecordMenuItem());
			battleMenu.add(getBattleSaveRecordAsMenuItem());
			battleMenu.add(getBattleImportRecordMenuItem());
			battleMenu.add(getBattleExportRecordMenuItem());
			battleMenu.add(new JSeparator());
			battleMenu.add(getBattleTakeScreenshotMenuItem());
			if (!macMenuEnabled) {
				battleMenu.add(new JSeparator());
				battleMenu.add(getBattleExitMenuItem());
			}
			battleMenu.addMenuListener(eventHandler);
		}
		return battleMenu;
	}

	private JMenuItem getBattleNewMenuItem() {
		if (battleNewMenuItem == null) {
			battleNewMenuItem = new JMenuItem();
			battleNewMenuItem.setText("New");
			battleNewMenuItem.setMnemonic('N');
			battleNewMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_SHORTCUT_KEY_MASK, false));
			battleNewMenuItem.addActionListener(eventHandler);
		}
		return battleNewMenuItem;
	}

	private JMenuItem getBattleOpenMenuItem() {
		if (battleOpenMenuItem == null) {
			battleOpenMenuItem = new JMenuItem();
			battleOpenMenuItem.setText("Open");
			battleOpenMenuItem.setMnemonic('O');
			battleOpenMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_SHORTCUT_KEY_MASK, false));
			battleOpenMenuItem.addActionListener(eventHandler);
		}
		return battleOpenMenuItem;
	}

	public JMenuItem getBattleSaveAsMenuItem() {
		if (battleSaveAsMenuItem == null) {
			battleSaveAsMenuItem = new JMenuItem();
			battleSaveAsMenuItem.setText("Save As");
			battleSaveAsMenuItem.setMnemonic('A');
			battleSaveAsMenuItem.setDisplayedMnemonicIndex(5);
			battleSaveAsMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_SHORTCUT_KEY_MASK | InputEvent.SHIFT_MASK, false));
			battleSaveAsMenuItem.setEnabled(false);
			battleSaveAsMenuItem.addActionListener(eventHandler);
		}
		return battleSaveAsMenuItem;
	}

	public JMenuItem getBattleSaveMenuItem() {
		if (battleSaveMenuItem == null) {
			battleSaveMenuItem = new JMenuItem();
			battleSaveMenuItem.setText("Save");
			battleSaveMenuItem.setMnemonic('S');
			battleSaveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_SHORTCUT_KEY_MASK, false));
			battleSaveMenuItem.setEnabled(false);
			battleSaveMenuItem.addActionListener(eventHandler);
		}
		return battleSaveMenuItem;
	}

	public JMenuItem getBattleRestartMenuItem() {
		if (battleRestartMenuItem == null) {
			battleRestartMenuItem = new JMenuItem();
			battleRestartMenuItem.setText("Restart");
			battleRestartMenuItem.setMnemonic('R');
			battleRestartMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MENU_SHORTCUT_KEY_MASK, false));
			battleRestartMenuItem.setEnabled(false);
			battleRestartMenuItem.addActionListener(eventHandler);
		}
		return battleRestartMenuItem;
	}

	public JMenuItem getBattleStopMenuItem() {
		if (battleStopMenuItem == null) {
			battleStopMenuItem = new JMenuItem();
			battleStopMenuItem.setText("Stop");
			battleStopMenuItem.setEnabled(false);
			battleStopMenuItem.addActionListener(eventHandler);
		}
		return battleStopMenuItem;
	}

	public JMenuItem getBattleTogglePauseMenuItem() {
		if (battleTogglePauseMenuItem == null) {
			battleTogglePauseMenuItem = new JMenuItem();
			battleTogglePauseMenuItem.setText("Pause / Resume");
			battleTogglePauseMenuItem.setMnemonic('P');
			battleTogglePauseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
			battleTogglePauseMenuItem.setEnabled(true);
			battleTogglePauseMenuItem.addActionListener(eventHandler);
		}
		return battleTogglePauseMenuItem;
	}

	public JMenuItem getBattleNextTurnMenuItem() {
		if (battleNextTurnMenuItem == null) {
			battleNextTurnMenuItem = new JMenuItem();
			battleNextTurnMenuItem.setText("Pause / Next Turn");
			battleNextTurnMenuItem.setMnemonic('T');
			battleNextTurnMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));
			battleNextTurnMenuItem.setEnabled(false);
			battleNextTurnMenuItem.addActionListener(eventHandler);
		}
		return battleNextTurnMenuItem;
	}

	public JMenu getBattleRobotListMenu() {
		if (battleRobotListMenu == null) {
			battleRobotListMenu = new JMenu();
			battleRobotListMenu.setText("Robots");
			battleRobotListMenu.setMnemonic('R');
			battleRobotListMenu.addActionListener(eventHandler);
			battleRobotListMenu.add(getBattleRobotListEmptyMenuItem());
		}
		return battleRobotListMenu;
	}

	public JMenuItem getBattleRobotListEmptyMenuItem() {
		if (battleRobotListEmptyMenuItem == null) {
			battleRobotListEmptyMenuItem = new JMenuItem();
			battleRobotListEmptyMenuItem.setText("(empty)");
			battleRobotListEmptyMenuItem.setEnabled(false);
		}
		return battleRobotListEmptyMenuItem;
	}

	public JMenuItem getBattleMainBattleMenuItem() {
		if (battleMainBattleMenuItem == null) {
			battleMainBattleMenuItem = new JMenuItem();
			battleMainBattleMenuItem.setText("Main Battle");
			battleMainBattleMenuItem.setMnemonic('M');
		}
		return battleMainBattleMenuItem;
	}

	private JMenuItem getBattleOpenRecordMenuItem() {
		if (battleOpenRecordMenuItem == null) {
			battleOpenRecordMenuItem = new JMenuItem();
			battleOpenRecordMenuItem.setText("Open Record");
			battleOpenRecordMenuItem.setMnemonic('d');
			battleOpenRecordMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_SHORTCUT_KEY_MASK | InputEvent.SHIFT_MASK, false));
			battleOpenRecordMenuItem.addActionListener(eventHandler);
		}
		return battleOpenRecordMenuItem;
	}

	private JMenuItem getBattleImportRecordMenuItem() {
		if (battleImportRecordMenuItem == null) {
			battleImportRecordMenuItem = new JMenuItem();
			battleImportRecordMenuItem.setText("Import XML Record");
			battleImportRecordMenuItem.setMnemonic('I');
			battleImportRecordMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_I, MENU_SHORTCUT_KEY_MASK, false));
			battleImportRecordMenuItem.addActionListener(eventHandler);
		}
		return battleImportRecordMenuItem;
	}

	public JMenuItem getBattleSaveRecordAsMenuItem() {
		if (battleSaveRecordAsMenuItem == null) {
			battleSaveRecordAsMenuItem = new JMenuItem();
			battleSaveRecordAsMenuItem.setText("Save Record");
			battleSaveRecordAsMenuItem.setMnemonic('R');
			battleSaveRecordAsMenuItem.setDisplayedMnemonicIndex(5);
			battleSaveRecordAsMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_R, MENU_SHORTCUT_KEY_MASK, false));
			battleSaveRecordAsMenuItem.setEnabled(false);
			battleSaveRecordAsMenuItem.addActionListener(eventHandler);

			ISettingsManager props = properties;

			props.addPropertyListener(
					new ISettingsListener() {
				public void settingChanged(String property) {
					if (property.equals(ISettingsManager.OPTIONS_COMMON_ENABLE_REPLAY_RECORDING)) {
						final boolean canReplayRecord = recordManager.hasRecord();
						final boolean enableSaveRecord = properties.getOptionsCommonEnableReplayRecording()
								& canReplayRecord;

						battleSaveRecordAsMenuItem.setEnabled(enableSaveRecord);
					}
				}
			});
		}
		return battleSaveRecordAsMenuItem;
	}

	public JMenuItem getBattleExportRecordMenuItem() {
		if (battleExportRecordMenuItem == null) {
			battleExportRecordMenuItem = new JMenuItem();
			battleExportRecordMenuItem.setText("Export XML Record");
			battleExportRecordMenuItem.setMnemonic('E');
			battleExportRecordMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_SHORTCUT_KEY_MASK, false));
			battleExportRecordMenuItem.setEnabled(false);
			battleExportRecordMenuItem.addActionListener(eventHandler);

			ISettingsManager props = properties;

			props.addPropertyListener(
					new ISettingsListener() {
				public void settingChanged(String property) {
					if (property.equals(ISettingsManager.OPTIONS_COMMON_ENABLE_REPLAY_RECORDING)) {
						final boolean canReplayRecord = recordManager.hasRecord();
						final boolean enableSaveRecord = properties.getOptionsCommonEnableReplayRecording()
								& canReplayRecord;

						battleExportRecordMenuItem.setEnabled(enableSaveRecord);
					}
				}
			});
		}
		return battleExportRecordMenuItem;
	}

	private JMenuItem getBattleTakeScreenshotMenuItem() {
		if (battleTakeScreenshotMenuItem == null) {
			battleTakeScreenshotMenuItem = new JMenuItem();
			battleTakeScreenshotMenuItem.setText("Take Screenshot");
			battleTakeScreenshotMenuItem.setMnemonic('T');
			battleTakeScreenshotMenuItem.setAccelerator(
					KeyStroke.getKeyStroke(KeyEvent.VK_T, MENU_SHORTCUT_KEY_MASK, false));
			battleTakeScreenshotMenuItem.addActionListener(eventHandler);
		}
		return battleTakeScreenshotMenuItem;
	}

	private JMenuItem getHelpAboutMenuItem() {
		if (helpAboutMenuItem == null) {
			helpAboutMenuItem = new JMenuItem();
			helpAboutMenuItem.setText("About");
			helpAboutMenuItem.setMnemonic('A');
			helpAboutMenuItem.addActionListener(eventHandler);
		}
		return helpAboutMenuItem;
	}

	private JMenuItem getHelpCheckForNewVersionMenuItem() {
		if (helpCheckForNewVersionMenuItem == null) {
			helpCheckForNewVersionMenuItem = new JMenuItem();
			helpCheckForNewVersionMenuItem.setText("Check for new version");
			helpCheckForNewVersionMenuItem.setMnemonic('C');
			helpCheckForNewVersionMenuItem.addActionListener(eventHandler);
		}
		return helpCheckForNewVersionMenuItem;
	}

	@Override
	public JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.setMnemonic('H');
			helpMenu.add(getReadMeMenuItem());
			helpMenu.add(getHelpRobocodeApiMenuItem());
			helpMenu.add(getHelpJavaDocumentationMenuItem());
			helpMenu.add(new JSeparator());
			helpMenu.add(getHelpOnlineHelpMenuItem());
			helpMenu.add(getHelpRoboWikiMenuItem());
			helpMenu.add(getHelpGoogleGroupRobocodeMenuItem());
			helpMenu.add(getHelpFaqMenuItem());
			helpMenu.add(new JSeparator());
			helpMenu.add(getHelpRobocodeMenuItem());
			helpMenu.add(getHelpRoboRumbleMenuItem());
			helpMenu.add(new JSeparator());
			helpMenu.add(getHelpCheckForNewVersionMenuItem());
			helpMenu.add(getHelpVersionsTxtMenuItem());
			if (!macMenuEnabled) {
				helpMenu.add(new JSeparator());
				helpMenu.add(getHelpAboutMenuItem());
			}
			helpMenu.addMenuListener(eventHandler);
		}
		return helpMenu;
	}

	private JMenuItem getHelpFaqMenuItem() {
		if (helpFaqMenuItem == null) {
			helpFaqMenuItem = new JMenuItem();
			helpFaqMenuItem.setText("Robocode FAQ");
			helpFaqMenuItem.setMnemonic('F');
			helpFaqMenuItem.setDisplayedMnemonicIndex(9);
			helpFaqMenuItem.addActionListener(eventHandler);
		}
		return helpFaqMenuItem;
	}

	private JMenuItem getReadMeMenuItem() {
		if (helpReadMeMenuItem == null) {
			helpReadMeMenuItem = new JMenuItem();
			helpReadMeMenuItem.setText("ReadMe");
			helpReadMeMenuItem.setMnemonic('M');
			helpReadMeMenuItem.setDisplayedMnemonicIndex(4);
			helpReadMeMenuItem.addActionListener(eventHandler);
		}
		return helpReadMeMenuItem;
	}

	private JMenuItem getHelpOnlineHelpMenuItem() {
		if (helpOnlineHelpMenuItem == null) {
			helpOnlineHelpMenuItem = new JMenuItem();
			helpOnlineHelpMenuItem.setText("Online help");
			helpOnlineHelpMenuItem.setMnemonic('O');
			helpOnlineHelpMenuItem.addActionListener(eventHandler);
		}
		return helpOnlineHelpMenuItem;
	}

	private JMenuItem getHelpVersionsTxtMenuItem() {
		if (helpVersionsTxtMenuItem == null) {
			helpVersionsTxtMenuItem = new JMenuItem();
			helpVersionsTxtMenuItem.setText("Version info");
			helpVersionsTxtMenuItem.setMnemonic('V');
			helpVersionsTxtMenuItem.addActionListener(eventHandler);
		}
		return helpVersionsTxtMenuItem;
	}

	private JMenuItem getHelpRobocodeApiMenuItem() {
		if (helpRobocodeApiMenuItem == null) {
			helpRobocodeApiMenuItem = new JMenuItem();
			helpRobocodeApiMenuItem.setText("Robocode API");
			helpRobocodeApiMenuItem.setMnemonic('I');
			helpRobocodeApiMenuItem.setDisplayedMnemonicIndex(11);
			helpRobocodeApiMenuItem.addActionListener(eventHandler);
		}
		return helpRobocodeApiMenuItem;
	}

	private JMenuItem getHelpRobocodeMenuItem() {
		if (helpRobocodeMenuItem == null) {
			helpRobocodeMenuItem = new JMenuItem();
			helpRobocodeMenuItem.setText("Robocode home page");
			helpRobocodeMenuItem.setMnemonic('H');
			helpRobocodeMenuItem.setDisplayedMnemonicIndex(9);
			helpRobocodeMenuItem.addActionListener(eventHandler);
		}
		return helpRobocodeMenuItem;
	}

	private JMenuItem getHelpJavaDocumentationMenuItem() {
		if (helpJavaDocumentationMenuItem == null) {
			helpJavaDocumentationMenuItem = new JMenuItem();
			helpJavaDocumentationMenuItem.setText("Java API documentation");
			helpJavaDocumentationMenuItem.setMnemonic('J');
			helpJavaDocumentationMenuItem.addActionListener(eventHandler);
		}
		return helpJavaDocumentationMenuItem;
	}

	private JMenuItem getHelpRoboWikiMenuItem() {
		if (helpRoboWikiMenuItem == null) {
			helpRoboWikiMenuItem = new JMenuItem();
			helpRoboWikiMenuItem.setText("RoboWiki site");
			helpRoboWikiMenuItem.setMnemonic('W');
			helpRoboWikiMenuItem.setDisplayedMnemonicIndex(4);
			helpRoboWikiMenuItem.addActionListener(eventHandler);
		}
		return helpRoboWikiMenuItem;
	}

	private JMenuItem getHelpGoogleGroupRobocodeMenuItem() {
		if (helpGoogleGroupRobocodeMenuItem == null) {
			helpGoogleGroupRobocodeMenuItem = new JMenuItem();
			helpGoogleGroupRobocodeMenuItem.setText("Google Group for Robocode");
			helpGoogleGroupRobocodeMenuItem.setMnemonic('Y');
			helpGoogleGroupRobocodeMenuItem.addActionListener(eventHandler);
		}
		return helpGoogleGroupRobocodeMenuItem;
	}

	private JMenuItem getHelpRoboRumbleMenuItem() {
		if (helpRoboRumbleMenuItem == null) {
			helpRoboRumbleMenuItem = new JMenuItem();
			helpRoboRumbleMenuItem.setText("RoboRumble");
			helpRoboRumbleMenuItem.setMnemonic('R');
			helpRoboRumbleMenuItem.setDisplayedMnemonicIndex(9);
			helpRoboRumbleMenuItem.addActionListener(eventHandler);
		}
		return helpRoboRumbleMenuItem;
	}

	private JMenuItem getOptionsAdjustTPSMenuItem() {
		if (optionsAdjustTPSMenuItem == null) {
			optionsAdjustTPSMenuItem = new JMenuItem();
			optionsAdjustTPSMenuItem.setText("Set TPS");
			optionsAdjustTPSMenuItem.setMnemonic('T');
			optionsAdjustTPSMenuItem.addActionListener(eventHandler);
		}
		return optionsAdjustTPSMenuItem;
	}

	private JCheckBoxMenuItem getOptionsHideControlsCheckBoxMenuItem() {
		if (optionsHideControlsMenuItem == null) {
			optionsHideControlsMenuItem = new JCheckBoxMenuItem();
			optionsHideControlsMenuItem.setText("Hide controls");
			optionsHideControlsMenuItem.setMnemonic('H');
			optionsHideControlsMenuItem.addActionListener(eventHandler);
		}
		return optionsHideControlsMenuItem;
	}

	private JMenuItem getOptionsFitWindowMenuItem() {
		if (optionsFitWindowMenuItem == null) {
			optionsFitWindowMenuItem = new JMenuItem();
			optionsFitWindowMenuItem.setText("Default window size");
			optionsFitWindowMenuItem.setMnemonic('D');
			optionsFitWindowMenuItem.addActionListener(eventHandler);
		}
		return optionsFitWindowMenuItem;
	}

	private JMenuItem getOptionsFitBattleFieldMenuItem() {
		if (optionsFitBattleFieldMenuItem == null) {
			optionsFitBattleFieldMenuItem = new JMenuItem();
			optionsFitBattleFieldMenuItem.setText("Fit battle view");
			optionsFitBattleFieldMenuItem.setMnemonic('F');
			optionsFitBattleFieldMenuItem.addActionListener(eventHandler);
		}
		return optionsFitBattleFieldMenuItem;
	}

	public JCheckBoxMenuItem getOptionsShowRankingCheckBoxMenuItem() {
		if (optionsShowRankingCheckBoxMenuItem == null) {
			optionsShowRankingCheckBoxMenuItem = new JCheckBoxMenuItem();
			optionsShowRankingCheckBoxMenuItem.setText("Show current rankings");
			optionsShowRankingCheckBoxMenuItem.setMnemonic('r');
			optionsShowRankingCheckBoxMenuItem.setDisplayedMnemonicIndex(13);
			optionsShowRankingCheckBoxMenuItem.addActionListener(eventHandler);
			optionsShowRankingCheckBoxMenuItem.setEnabled(false);
		}
		return optionsShowRankingCheckBoxMenuItem;
	}

	private JMenuItem getOptionsRecalculateCpuConstantMenuItem() {
		if (optionsRecalculateCpuConstantMenuItem == null) {
			optionsRecalculateCpuConstantMenuItem = new JMenuItem();
			optionsRecalculateCpuConstantMenuItem.setText("Recalculate CPU constant");
			optionsRecalculateCpuConstantMenuItem.setMnemonic('e');
			optionsRecalculateCpuConstantMenuItem.setDisplayedMnemonicIndex(1);
			optionsRecalculateCpuConstantMenuItem.addActionListener(eventHandler);
		}
		return optionsRecalculateCpuConstantMenuItem;
	}

	private JMenuItem getOptionsCleanRobotCacheMenuItem() {
		if (optionsCleanRobotCacheMenuItem == null) {
			optionsCleanRobotCacheMenuItem = new JMenuItem();
			optionsCleanRobotCacheMenuItem.setText("Clean robot cache");
			optionsCleanRobotCacheMenuItem.setMnemonic('C');
			optionsCleanRobotCacheMenuItem.addActionListener(eventHandler);
		}
		return optionsCleanRobotCacheMenuItem;
	}

	private JMenu getOptionsMenu() {
		if (optionsMenu == null) {
			optionsMenu = new JMenu();
			optionsMenu.setText("Options");
			optionsMenu.setMnemonic('O');
			if (!macMenuEnabled) {
				optionsMenu.add(getOptionsPreferencesMenuItem());
			}
			optionsMenu.add(getOptionsAdjustTPSMenuItem());
			optionsMenu.add(new JSeparator());
			optionsMenu.add(getOptionsHideControlsCheckBoxMenuItem());
			optionsMenu.add(getOptionsFitWindowMenuItem());
			optionsMenu.add(getOptionsFitBattleFieldMenuItem());
			optionsMenu.add(new JSeparator());
			optionsMenu.add(getOptionsShowRankingCheckBoxMenuItem());
			optionsMenu.add(new JSeparator());
			optionsMenu.add(getOptionsRecalculateCpuConstantMenuItem());
			optionsMenu.add(getOptionsCleanRobotCacheMenuItem());
			optionsMenu.addMenuListener(eventHandler);
		}
		return optionsMenu;
	}

	private JMenuItem getOptionsPreferencesMenuItem() {
		if (optionsPreferencesMenuItem == null) {
			optionsPreferencesMenuItem = new JMenuItem();
			optionsPreferencesMenuItem.setText("Preferences");
			optionsPreferencesMenuItem.setMnemonic('P');
			optionsPreferencesMenuItem.addActionListener(eventHandler);
		}
		return optionsPreferencesMenuItem;
	}

	private JMenuItem getRobotEditorMenuItem() {
		if (robotEditorMenuItem == null) {
			robotEditorMenuItem = new JMenuItem();
			robotEditorMenuItem.setText("Source Editor");
			robotEditorMenuItem.setMnemonic('E');
			robotEditorMenuItem.setEnabled(net.sf.robocode.core.Container.getComponent(IRobocodeEditor.class) != null);
			robotEditorMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, MENU_SHORTCUT_KEY_MASK, false));
			robotEditorMenuItem.addActionListener(eventHandler);
		}
		return robotEditorMenuItem;
	}

	private JMenuItem getRobotImportMenuItem() {
		if (robotImportMenuItem == null) {
			robotImportMenuItem = new JMenuItem();
			robotImportMenuItem.setText("Import robot or team");
			robotImportMenuItem.setMnemonic('I');
			robotImportMenuItem.addActionListener(eventHandler);
		}
		return robotImportMenuItem;
	}

	public JMenu getRobotMenu() {
		if (robotMenu == null) {
			robotMenu = new JMenu();
			robotMenu.setText("Robot");
			robotMenu.setMnemonic('R');
			robotMenu.add(getRobotEditorMenuItem());
			robotMenu.add(new JSeparator());
			robotMenu.add(getRobotImportMenuItem());
			robotMenu.add(getRobotPackagerMenuItem());
			robotMenu.add(new JSeparator());
			robotMenu.add(getRobotCreateTeamMenuItem());
			robotMenu.addMenuListener(eventHandler);
		}
		return robotMenu;
	}

	private JMenuItem getRobotPackagerMenuItem() {
		if (robotPackagerMenuItem == null) {
			robotPackagerMenuItem = new JMenuItem();
			robotPackagerMenuItem.setText("Package robot or team");
			robotPackagerMenuItem.setMnemonic('P');
			robotPackagerMenuItem.addActionListener(eventHandler);
		}
		return robotPackagerMenuItem;
	}

	private JMenuItem getRobotCreateTeamMenuItem() {
		if (robotCreateTeamMenuItem == null) {
			robotCreateTeamMenuItem = new JMenuItem();
			robotCreateTeamMenuItem.setText("Create a robot team");
			robotCreateTeamMenuItem.setMnemonic('C');
			robotCreateTeamMenuItem.addActionListener(eventHandler);
		}
		return robotCreateTeamMenuItem;
	}

	private void teamCreateTeamActionPerformed() {
		windowManager.showCreateTeamDialog();
	}

	private void helpAboutActionPerformed() {
		windowManager.showAboutBox();
	}

	private void helpCheckForNewVersionActionPerformed() {
		if (!robocodeFrame.checkForNewVersion(true)) {
			WindowUtil.messageError("Unable to check for new version ");
		}
	}

	private void helpFaqActionPerformed() {
		windowManager.showFaq();
	}

	private void helpReadMeActionPerformed() {
		windowManager.showReadMe();
	}

	private void helpOnlineHelpActionPerformed() {
		windowManager.showOnlineHelp();
	}

	private void helpVersionsTxtActionPerformed() {
		windowManager.showVersionsTxt();
	}

	private void helpRobocodeApiActionPerformed() {
		windowManager.showHelpApi();
	}

	private void helpRobocodeHomeMenuItemActionPerformed() {
		windowManager.showRobocodeHome();
	}

	private void helpJavaDocumentationActionPerformed() {
		windowManager.showJavaDocumentation();
	}

	private void helpRoboWikiMenuItemActionPerformed() {
		windowManager.showRoboWiki();
	}

	private void helpGoogleGroupRobocodeActionPerformed() {
		windowManager.showGoogleGroupRobocode();
	}

	private void helpRoboRumbleActionPerformed() {
		windowManager.showRoboRumble();
	}

	private void optionsFitWindowActionPerformed() {
		final RobocodeFrame robocodeFrame = (RobocodeFrame) windowManager.getRobocodeFrame();

		if (robocodeFrame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
			return;
		}

		Promise.delayed(100).then(new Runnable() {
			@Override
			public void run() {
				robocodeFrame.clearPreferredSize();
				robocodeFrame.setPreferredSizeMode(PreferredSizeMode.MINIMAL);
				robocodeFrame.pack();
			}
		});
	}

	private void optionsFitBattleFieldActionPerformed() {
		robocodeFrame.resetRobocodeFrameSize(PreferredSizeMode.SHRINK_TO_FIT);
	}

	private void optionsHideControlsActionPerformed() {
		properties.setOptionsUiHideControls(!properties.getOptionsUiHideControls());
	}

	private void optionsAdjustTPSActionPerformed() {
		battleManager.pauseBattle();

		int tps = properties.getOptionsBattleDesiredTPS();
		String res = JOptionPane.showInputDialog(this, "Input new TPS: ", tps);
		if (res != null) {
			try {
				tps = Integer.parseInt(res);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "Invalid number entered. ",
					"Failed to change TPS", JOptionPane.ERROR_MESSAGE);
			}
		}
		if (tps >= 0) {
			robocodeFrame.setTPS(tps, true);
		}
	}

	private void optionsShowRankingActionPerformed() {
		windowManager.showRankingDialog(getOptionsShowRankingCheckBoxMenuItem().getState());
	}

	private void optionsRecalculateCpuConstantPerformed() {
		int ok = JOptionPane.showConfirmDialog(this, "Do you want to recalculate the CPU constant?",
				"Recalculate CPU constant", JOptionPane.YES_NO_OPTION);

		if (ok == JOptionPane.YES_OPTION) {
			try {
				robocodeFrame.setBusyPointer(true);
				cpuManager.calculateCpuConstant();
			} finally {
				robocodeFrame.setBusyPointer(false);
			}

			long cpuConstant = cpuManager.getCpuConstant();

			JOptionPane.showMessageDialog(this, "CPU constant: " + cpuConstant + " nanoseconds per turn",
					"New CPU constant", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void optionsCleanRobotCachePerformed() {
		int ok = JOptionPane.showConfirmDialog(this, "Do you want to clean the robot cache?", "Clean Robot Cache",
				JOptionPane.YES_NO_OPTION);

		if (ok == JOptionPane.YES_OPTION) {
			try {
				robocodeFrame.setBusyPointer(true);
				net.sf.robocode.cachecleaner.CacheCleaner.clean();
			} finally {
				robocodeFrame.setBusyPointer(false);
			}
		}
	}

	private void optionsPreferencesActionPerformed() {
		windowManager.showOptionsPreferences();
	}

	private void robotEditorActionPerformed() {
		windowManager.showRobocodeEditor();
	}

	private void robotImportActionPerformed() {
		windowManager.showImportRobotDialog();
	}

	private void robotPackagerActionPerformed() {
		windowManager.showRobotPackager();
	}

	public class MyMacMenuHandler implements MacMenuHandler {
		@Override
		public void handleAbout(Object e) {
			helpAboutActionPerformed();
		}

		@Override
		public void handlePreferences(Object e) {
			optionsPreferencesActionPerformed();
		}

		@Override
		public void handleQuitRequestWith(Object e, Object r) {
			battleExitActionPerformed();
		}
	}
}
