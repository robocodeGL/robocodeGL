package net.sf.robocode.ui.mac;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MacMenuHelper {
	private MacMenuHelper() {
	}

	public static void main(String[] args) {
		System.out.println(System.getProperty("os.name").toLowerCase());

		if (initMacMenu(new MacMenuHandler() {
			@Override
			public void handleAbout(Object e) {
				JOptionPane.showMessageDialog(null, "About dialog");
			}

			@Override
			public void handlePreferences(Object e) {
				JOptionPane.showMessageDialog(null, "Preferences dialog");
			}

			@Override
			public void handleQuitRequestWith(Object e, Object r) {
				JOptionPane.showMessageDialog(null, "Quit dialog");
				System.exit(0);
			}
		})) {
			System.out.println("mac menu supported!");
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame("MacMenuHelper");
				frame.setSize(new Dimension(600, 400));
				frame.setLocationRelativeTo(null);
				frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}

	@SuppressWarnings({"JavaReflectionMemberAccess", "JavaReflectionInvocation", "RedundantSuppression"})
	public static boolean initMacMenu(Object handler) {
		try {
			Class<?> desktopClass = Class.forName("java.awt.Desktop");
			Method getDesktopMethod = desktopClass.getMethod("getDesktop");
			Object desktopInstance = getDesktopMethod.invoke(null);

			Class<?> aboutHandlerClass = Class.forName("java.awt.desktop.AboutHandler");
			Class<?> preferencesHandlerClass = Class.forName("java.awt.desktop.PreferencesHandler");
			Class<?> quitHandlerClass = Class.forName("java.awt.desktop.QuitHandler");

			Method setAboutHandlerMethod = desktopClass.getMethod("setAboutHandler", aboutHandlerClass);
			Method setPreferencesHandlerMethod = desktopClass.getMethod("setPreferencesHandler", preferencesHandlerClass);
			Method setQuitHandlerMethod = desktopClass.getMethod("setQuitHandler", quitHandlerClass);

			Object o = DynamicImplement.makeImplement(new Class<?>[]{
				aboutHandlerClass,
				preferencesHandlerClass,
				quitHandlerClass
			}, handler);

			setAboutHandlerMethod.invoke(desktopInstance, o);
			setPreferencesHandlerMethod.invoke(desktopInstance, o);
			setQuitHandlerMethod.invoke(desktopInstance, o);

			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (NoSuchMethodException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
	}
}
