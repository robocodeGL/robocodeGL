package net.sf.robocode.ui.dialog;

import javax.swing.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.awt.datatransfer.DataFlavor.javaFileListFlavor;

public final class FileDropHandler extends TransferHandler {
	private FileListConsumer consumer;

	public FileListConsumer getConsumer() {
		return consumer;
	}

	public void setConsumer(FileListConsumer consumer) {
		this.consumer = consumer;
	}

	@Override
	public boolean canImport(TransferSupport support) {
		if (support.isDataFlavorSupported(javaFileListFlavor)) {
			support.setDropAction(COPY);

			return true;
		}
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		if (!this.canImport(support)) {
			return false;
		}

		List<File> files = getFiles(support);
		if (files == null) {
			return false;
		}

		if (consumer != null) {
			try {
				consumer.accept(files);
			} catch (IOException e) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	private static List<File> getFiles(TransferSupport support) {
		List<File> files;
		try {
			files = (List<File>) support.getTransferable()
				.getTransferData(javaFileListFlavor);
			return files;
		} catch (UnsupportedFlavorException ex) {
			ex.printStackTrace();
			return null;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public interface FileListConsumer {
		void accept(List<File> files) throws IOException;
	}
}
