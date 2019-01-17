/**
 * Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.ui;


import net.sf.robocode.battle.BattleProperties;
import net.sf.robocode.battle.BattleResultsTableModel;
import net.sf.robocode.battle.IBattleManager;
import net.sf.robocode.core.Container;
import net.sf.robocode.host.ICpuManager;
import net.sf.robocode.io.FileUtil;
import net.sf.robocode.repository.IRepositoryManager;
import net.sf.robocode.settings.ISettingsManager;
import net.sf.robocode.ui.battle.AwtBattleAdaptor;
import net.sf.robocode.ui.dialog.*;
import net.sf.robocode.ui.editor.IRobocodeEditor;
import net.sf.robocode.ui.packager.RobotPackager;
import net.sf.robocode.version.IVersionManager;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.IBattleListener;
import robocode.control.snapshot.ITurnSnapshot;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map.Entry;


/**
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 * @author Luis Crespo (contributor)
 */
public class WindowManager implements IWindowManagerExt {

	private final static int TIMER_TICKS_PER_SECOND = 50;
	private final AwtBattleAdaptor awtAdaptor;
	private RobotPackager robotPackager;
	private RobotExtractor robotExtractor;
	private final ISettingsManager settingsManager;
	private final IBattleManager battleManager;
	private final ICpuManager cpuManager;
	private final IRepositoryManager repositoryManager;
	private final IVersionManager versionManager;
	private final IImageManager imageManager;
	private IRobotDialogManager robotDialogManager;
	private RobocodeFrame robocodeFrame;

	private boolean isGUIEnabled = true;
	private boolean isSlave;
	private boolean centerRankings = true;
	private boolean oldRankingHideState = true;
	private boolean showResults = true;

	public WindowManager(ISettingsManager settingsManager, IBattleManager battleManager, ICpuManager cpuManager, IRepositoryManager repositoryManager, IImageManager imageManager, IVersionManager versionManager) {
		this.settingsManager = settingsManager;
		this.battleManager = battleManager;
		this.repositoryManager = repositoryManager;
		this.cpuManager = cpuManager;
		this.versionManager = versionManager;
		this.imageManager = imageManager;
		awtAdaptor = new AwtBattleAdaptor(battleManager, TIMER_TICKS_PER_SECOND, true);

		// we will set UI better priority than robots and battle have
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
				} catch (SecurityException ex) {// that's a pity
				}
			}
		});
	}

	public void setBusyPointer(boolean enabled) {
		robocodeFrame.setBusyPointer(enabled);
	}

	public synchronized void addBattleListener(IBattleListener listener) {
		awtAdaptor.addListener(listener);
	}

	public synchronized void removeBattleListener(IBattleListener listener) {
		awtAdaptor.removeListener(listener);
	}

	public boolean isGUIEnabled() {
		return isGUIEnabled;
	}

	public void setEnableGUI(boolean enable) {
		isGUIEnabled = enable;

		// Set the system property so the AWT headless mode.
		// Read more about headless mode here:
		// http://java.sun.com/developer/technicalArticles/J2SE/Desktop/headless/
		System.setProperty("java.awt.headless", "" + !enable);
	}

	public void setSlave(boolean value) {
		isSlave = value;
	}

	public boolean isSlave() {
		return isSlave;
	}

	public boolean isIconified() {
		return robocodeFrame.isIconified();
	}

	public boolean isShowResultsEnabled() {
		return settingsManager.getOptionsCommonShowResults() && showResults;
	}

	public void setEnableShowResults(boolean enable) {
		showResults = enable;
	}

	public ITurnSnapshot getLastSnapshot() {
		return awtAdaptor.getLastSnapshot();
	}

	public int getFPS() {
		return isIconified() ? 0 : awtAdaptor.getFPS();
	}

	public RobocodeFrame getRobocodeFrame() {
		if (robocodeFrame == null) {
			this.robocodeFrame = Container.getComponent(RobocodeFrame.class);
		}
		return robocodeFrame;
	}

	public void showRobocodeFrame(boolean visible, boolean iconified) {
		RobocodeFrame frame = getRobocodeFrame();

		if (iconified) {
			frame.setState(Frame.ICONIFIED);
		}

		if (visible) {
			// Pack frame to size all components
			WindowUtil.packCenterShow(frame);

			WindowUtil.setStatusLabel(frame.getStatusLabel());

			frame.checkUpdateOnStart();

			FileDropHandler handler = frame.getFileDropHandler();
			if (handler.getConsumer() == null) {
				handler.setConsumer(new FileDropHandler.FileListConsumer() {
					@Override
					public void accept(List<File> files) throws IOException {
						RobotJarFilter filter = new RobotJarFilter();

						List<File> accepted = new ArrayList<File>();

						for (File file : files) {
							if (filter.accept(file.getParentFile(), file.getName())) {
								accepted.add(file);
							}
						}

						if (accepted.isEmpty()) throw new IOException("Nothing to import");

						importRobots(accepted);
					}
				});
			}

		} else {
			frame.setVisible(false);
		}
	}

	public void showAboutBox() {
		packCenterShow(Container.getComponent(AboutBox.class), true);
	}

	public String showBattleOpenDialog(final String defExt, final String name) {
		return showNativeDialog(defExt, name, FileDialog.LOAD, battleManager.getBattlePath(), null);
	}

	private String showNativeDialog(final String defExt, String name, int mode, String directory, String file) {
		return showNativeDialog(name, mode, directory, file, new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().lastIndexOf(defExt.toLowerCase())
						== name.length() - defExt.length();
			}
		});
	}

	private String showNativeDialog(String name, int mode, String directory, String file, FilenameFilter filter) {
		FileDialog dialog = new FileDialog(getRobocodeFrame(), name, mode);

		dialog.setFilenameFilter(filter);

		dialog.setDirectory(directory);
		dialog.setFile(file);

		dialog.setVisible(true);

		if (dialog.getFile() == null) {
			return null;
		} else {
			return dialog.getDirectory() + dialog.getFile();
		}
	}

	private File[] showNativeDialogMultipleFile(String name, int mode, String directory, String file, FilenameFilter filter) {
		FileDialog dialog = new FileDialog(getRobocodeFrame(), name, mode);

		dialog.setFilenameFilter(filter);

		dialog.setDirectory(directory);
		dialog.setFile(file);

		Class<FileDialog> fileDialogClass = FileDialog.class;

		try {
			Method setMultipleMode = fileDialogClass.getMethod("setMultipleMode", Boolean.TYPE);
			Method getFiles = fileDialogClass.getMethod("getFiles");

			setMultipleMode.invoke(dialog, true);

			dialog.setVisible(true);

			File[] files = (File[]) getFiles.invoke(dialog);
			if (files.length == 0) {
				return null;
			} else {
				return files;
			}
		} catch (NoSuchMethodException e) {
			dialog.setVisible(true);

			if (dialog.getFile() == null) {
				return null;
			}

			return new File[]{new File(dialog.getDirectory() + dialog.getFile())};
		} catch (IllegalAccessException e) {
			e.printStackTrace();

			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace();

			return null;
		}
	}

	public String saveBattleDialog(String path, final String defExt, final String name) {
		String result = showNativeDialog(defExt, name, FileDialog.SAVE, path, null);

		if (result != null) {
			int idx = result.lastIndexOf('.');
			String extension = "";

			if (idx > 0) {
				extension = result.substring(idx);
			}
			if (!(extension.equalsIgnoreCase(defExt))) {
				result += defExt;
			}
		}

		return result;
	}

	public String showBattleOpenDialogLegacy(final String defExt, final String name) {
		JFileChooser chooser = new JFileChooser(battleManager.getBattlePath());

		chooser.setFileFilter(
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.isDirectory()
								|| pathname.getName().toLowerCase().lastIndexOf(defExt.toLowerCase())
								== pathname.getName().length() - defExt.length();
					}

					@Override
					public String getDescription() {
						return name;
					}
				});

		if (chooser.showOpenDialog(getRobocodeFrame()) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getPath();
		}
		return null;
	}

	public String saveBattleDialogLegacy(String path, final String defExt, final String name) {
		File f = new File(path);

		JFileChooser chooser;

		chooser = new JFileChooser(f);

		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory()
						|| pathname.getName().toLowerCase().lastIndexOf(defExt.toLowerCase())
						== pathname.getName().length() - defExt.length();
			}

			@Override
			public String getDescription() {
				return name;
			}
		};

		chooser.setFileFilter(filter);
		int rv = chooser.showSaveDialog(getRobocodeFrame());
		String result = null;

		if (rv == JFileChooser.APPROVE_OPTION) {
			result = chooser.getSelectedFile().getPath();
			int idx = result.lastIndexOf('.');
			String extension = "";

			if (idx > 0) {
				extension = result.substring(idx);
			}
			if (!(extension.equalsIgnoreCase(defExt))) {
				result += defExt;
			}
		}
		return result;
	}

	public void showVersionsTxt() {
		showInBrowser("file://" + new File(FileUtil.getCwd(), "").getAbsoluteFile() + File.separator + "versions.md");
	}

	public void showHelpApi() {
		showInBrowser(
				"file://" + new File(FileUtil.getCwd(), "").getAbsoluteFile() + File.separator + "javadoc" + File.separator
						+ "index.html");
	}

	public void showReadMe() {
		showInBrowser("file://" + new File(FileUtil.getCwd(), "ReadMe.html").getAbsoluteFile());
	}

	public void showFaq() {
		showInBrowser("http://robowiki.net/w/index.php?title=Robocode/FAQ");
	}

	public void showOnlineHelp() {
		showInBrowser("http://robowiki.net/w/index.php?title=Robocode/Getting_Started");
	}

	public void showJavaDocumentation() {
		showInBrowser("https://docs.oracle.com/javase/8/docs/api/");
	}

	public void showRobocodeHome() {
		showInBrowser("https://robocode.sourceforge.io");
	}

	public void showRoboWiki() {
		showInBrowser("http://robowiki.net");
	}

	public void showGoogleGroupRobocode() {
		showInBrowser("https://groups.google.com/forum/?fromgroups#!forum/robocode");
	}

	public void showRoboRumble() {
		showInBrowser("http://robowiki.net/wiki/RoboRumble");
	}

	public void showOptionsPreferences() {
		try {
			battleManager.pauseBattle();

			WindowUtil.packCenterShow(getRobocodeFrame(), Container.getComponent(PreferencesDialog.class));
		} finally {
			battleManager.resumeIfPausedBattle(); // THIS is just dirty hack-fix of more complex problem with desiredTPS and pausing.  resumeBattle() belongs here.
		}
	}

	public void showResultsDialog(BattleCompletedEvent event) {
		final ResultsDialog dialog = Container.getComponent(ResultsDialog.class);

		dialog.setup(event.getSortedResults(), event.getBattleRules().getNumRounds());
		packCenterShow(dialog, true);
	}

	public void showRankingDialog(boolean visible) {
		boolean currentRankingHideState = settingsManager.getOptionsCommonDontHideRankings();

		// Check if the Ranking hide states has changed
		if (currentRankingHideState != oldRankingHideState) {
			// Remove current visible RankingDialog, if it is there
			Container.getComponent(RankingDialog.class).dispose();

			// Replace old RankingDialog, as the owner window must be replaced from the constructor
			Container.cache.removeComponent(RankingDialog.class);
			Container.cache.addComponent(RankingDialog.class);

			// Reset flag for centering the dialog the first time it is shown
			centerRankings = true;
		}

		RankingDialog rankingDialog = Container.getComponent(RankingDialog.class);

		if (visible) {
			packCenterShow(rankingDialog, centerRankings);
			centerRankings = false; // only center the first time Rankings are shown
		} else {
			rankingDialog.dispose();
		}

		// Save current Ranking hide state
		oldRankingHideState = currentRankingHideState;
	}

	public void showRobocodeEditor() {
		JFrame editor = (JFrame) net.sf.robocode.core.Container.getComponent(IRobocodeEditor.class);

		if (!editor.isVisible()) {
			WindowUtil.packCenterShow(editor);
		} else {
			editor.setVisible(true);
		}
	}

	public void showRobotPackager() {
		if (robotPackager != null) {
			robotPackager.dispose();
			robotPackager = null;
		}

		robotPackager = net.sf.robocode.core.Container.factory.getComponent(RobotPackager.class);
		WindowUtil.packCenterShow(robotPackager);
	}

	public void showRobotExtractor(JFrame owner) {
		if (robotExtractor != null) {
			robotExtractor.dispose();
			robotExtractor = null;
		}

		robotExtractor = new net.sf.robocode.ui.dialog.RobotExtractor(owner, this, repositoryManager);
		WindowUtil.packCenterShow(robotExtractor);
	}

	public void showSplashScreen() {
		RcSplashScreen splashScreen = Container.getComponent(RcSplashScreen.class);

		packCenterShow(splashScreen, true);

		WindowUtil.setStatusLabel(splashScreen.getSplashLabel());

		repositoryManager.reload(versionManager.isLastRunVersionChanged());

		WindowUtil.setStatusLabel(splashScreen.getSplashLabel());
		cpuManager.getCpuConstant();

		WindowUtil.setStatus("");
		WindowUtil.setStatusLabel(null);

		splashScreen.dispose();
	}

	public void showNewBattleDialog(BattleProperties battleProperties) {
		try {
			battleManager.pauseBattle();
			final NewBattleDialog battleDialog = Container.createComponent(NewBattleDialog.class);

			battleDialog.setup(settingsManager, battleProperties);
			WindowUtil.packCenterShow(getRobocodeFrame(), battleDialog);
		} finally {
			battleManager.resumeBattle();
		}
	}

	public boolean closeRobocodeEditor() {
		IRobocodeEditor editor = net.sf.robocode.core.Container.getComponent(IRobocodeEditor.class);

		return editor == null || !((JFrame) editor).isVisible() || editor.close();
	}

	public void showCreateTeamDialog() {
		TeamCreator teamCreator = Container.getComponent(TeamCreator.class);

		WindowUtil.packCenterShow(teamCreator);
	}

	public void showImportRobotDialog() {
		File[] files = showNativeDialogMultipleFile("Select the robot .jar file to copy to " + repositoryManager.getRobotsDirectory(),
				FileDialog.LOAD, null, null,
			new RobotJarFilter());
		if (files != null) {
			importRobots(Arrays.asList(files));
		}
	}

	private void importRobots(List<File> files) {
		// for (File file : files) {
		// 	tryImportRobot(file);
		// }

		int skipped = 0;

		List<Entry<File, File>> todo = new ArrayList<Entry<File, File>>();
		List<File> to_overwrite = new ArrayList<File>();
		List<Entry<File, File>> overwrite = new ArrayList<Entry<File, File>>();

		for (File inputFile : files) {
			File outputFile = prepareImportRobot(inputFile);
			if (inputFile.equals(outputFile)) {
				skipped += 1;
				continue;
			}
			if (outputFile.exists()) {
				to_overwrite.add(outputFile);
				overwrite.add(new SimpleImmutableEntry<File, File>(inputFile, outputFile));
				continue;
			}
			todo.add(new SimpleImmutableEntry<File, File>(inputFile, outputFile));
		}

		int suc = 0;
		List<Exception> exceptions = new ArrayList<Exception>();

		if (!todo.isEmpty()) {
			exceptions.addAll(importRobotsImpl(todo));
			suc += todo.size();
		}

		if (!overwrite.isEmpty()) {
			if (JOptionPane.showConfirmDialog(getRobocodeFrame(), to_overwrite + " already exists.  Overwrite?",
				"Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				skipped += overwrite.size();
			} else {
				exceptions.addAll(importRobotsImpl(overwrite));
				suc += overwrite.size();
			}
		}

		suc -= exceptions.size();

		String exceptionMsg = exceptions.size() == 0 ? "" : String.format("Exceptions: %s", exceptions);
		String msg = String.format(
			"%d Robots imported successfully, %d skipped, %d failed. %s", suc, skipped, exceptions.size(), exceptionMsg);

		JOptionPane.showMessageDialog(getRobocodeFrame(), msg);
	}

	private List<Exception> importRobotsImpl(List<Entry<File, File>> todo) {
		List<Exception> exceptions = new ArrayList<Exception>();

		for (Entry<File, File> pair : todo) {
			try {
				FileUtil.copy(pair.getKey(), pair.getValue());
			} catch (IOException e) {
				exceptions.add(e);
			}
		}

		repositoryManager.refresh();

		return exceptions;
	}

	private File prepareImportRobot(File inputFile) {
		String fileName = inputFile.getName();
		String extension = "";

		int idx = fileName.lastIndexOf('.');

		if (idx >= 0) {
			extension = fileName.substring(idx);
		}
		if (!extension.equalsIgnoreCase(".jar")) {
			fileName += ".jar";
		}
		return new File(repositoryManager.getRobotsDirectory(), fileName);
	}

	/**
	 * Shows a web page using the browser manager.
	 *
	 * @param url The URL of the web page
	 */
	private void showInBrowser(String url) {
		try {
			BrowserManager.openURL(url);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(getRobocodeFrame(), e.getMessage(), "Unable to open browser!",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void showSaveResultsDialog(BattleResultsTableModel tableModel) {
		JFileChooser chooser = new JFileChooser();

		chooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if (pathname.isHidden()) {
					return false;
				}
				if (pathname.isDirectory()) {
					return true;
				}
				String filename = pathname.getName();
				int idx = filename.lastIndexOf('.');

				String extension = "";

				if (idx >= 0) {
					extension = filename.substring(idx);
				}
				return extension.equalsIgnoreCase(".csv");
			}

			@Override
			public String getDescription() {
				return "Comma Separated Value (CSV) File Format";
			}
		});

		chooser.setDialogTitle("Save battle results");

		if (chooser.showSaveDialog(getRobocodeFrame()) == JFileChooser.APPROVE_OPTION) {

			String filename = chooser.getSelectedFile().getPath();

			if (!filename.endsWith(".csv")) {
				filename += ".csv";
			}

			boolean append = settingsManager.getOptionsCommonAppendWhenSavingResults();

			tableModel.saveToFile(filename, append);
		}
	}

	/**
	 * Packs, centers, and shows the specified window on the screen.
	 *
	 * @param window the window to pack, center, and show
	 * @param center {@code true} if the window must be centered; {@code false} otherwise
	 */
	private void packCenterShow(Window window, boolean center) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		window.pack();
		if (center) {
			window.setLocation((screenSize.width - window.getWidth()) / 2, (screenSize.height - window.getHeight()) / 2);
		}
		window.setVisible(true);
	}

	public void cleanup() {
		if (isGUIEnabled()) {
			getRobocodeFrame().dispose();
		}
	}

	public void setStatus(String s) {
		WindowUtil.setStatus(s);
	}

	public void messageWarning(String s) {
		WindowUtil.messageWarning(s);
	}

	public IRobotDialogManager getRobotDialogManager() {
		if (robotDialogManager == null) {
			robotDialogManager = new RobotDialogManager();
		}
		return robotDialogManager;
	}

	public void init() {
		setLookAndFeel();
		imageManager.initialize(); // Make sure this one is initialized so all images are available
		awtAdaptor.subscribe(isGUIEnabled);
	}

	/**
	 * Sets the Look and Feel (LAF). This method first try to set the LAF to the
	 * system's LAF. If this fails, it try to use the cross platform LAF.
	 * If this also fails, the LAF will not be changed.
	 */
	private void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
			// Work-around for problems with setting Look and Feel described here:
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6468089
			Locale.setDefault(Locale.US);

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Throwable t2) {
				// For some reason Ubuntu 7 can cause a NullPointerException when trying to getting the LAF
				System.err.println("Could not set the Look and Feel (LAF).  The default LAF is used instead");
			}
		}
		// Java 1.6 provide system specific anti-aliasing. Enable it, if it has not been set
		if (new Double(System.getProperty("java.specification.version")) >= 1.6) {
			String aaFontSettings = System.getProperty("awt.useSystemAAFontSettings");

			if (aaFontSettings == null) {
				System.setProperty("awt.useSystemAAFontSettings", "on");
			}
		}
	}

	public void runIntroBattle() {
		final File intro = new File(FileUtil.getCwd(), "battles/intro.battle");
		if (intro.exists()) {
			battleManager.setBattleFilename(intro.getPath());
			battleManager.loadBattleProperties();

			final boolean origShowResults = showResults; // save flag for showing the results

			showResults = false;
			try {
				battleManager.startNewBattle(battleManager.loadBattleProperties(), true, false);
				battleManager.setDefaultBattleProperties();
				robocodeFrame.afterIntroBattle();
			} finally {
				showResults = origShowResults; // always restore the original flag for showing the results
			}
		}
	}

	public void setVisibleForRobotEngine(boolean visible) {
		if (visible && !isGUIEnabled()) {
			// The GUI must be enabled in order to show the window
			setEnableGUI(true);

			// Set the Look and Feel (LAF)
			init();
		}

		if (isGUIEnabled()) {
			showRobocodeFrame(visible, false);
			showResults = visible;
		}
	}

	private static class RobotJarFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			if (name.equals("robocode.jar")) {
				return false;
			}
			int idx = name.lastIndexOf('.');

			String extension = "";

			if (idx >= 0) {
				extension = name.substring(idx);
			}
			return extension.equalsIgnoreCase(".jar") || extension.equalsIgnoreCase(".zip");
		}
	}
}
