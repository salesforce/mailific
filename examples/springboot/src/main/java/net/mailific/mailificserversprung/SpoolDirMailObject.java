package net.mailific.mailificserversprung;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.mailific.server.reference.BaseMailObject;
import net.mailific.server.session.Reply;
import net.mailific.server.session.SmtpSession;

/**
 * 
 * An example MailObject that writes each message to a file in a specified directory.
 * 
 * @author jhumphreys
 *
 */
public class SpoolDirMailObject extends BaseMailObject {

	private static final Logger log = Logger.getLogger(SpoolDirMailObject.class.getName());
	
	final private File spoolDir;
	
	private boolean complete = false;
	private File spoolFile; 
	private OutputStream out;
	
	public SpoolDirMailObject(String spoolPath) {
		spoolDir = new File(spoolPath);
		if (!spoolDir.exists()) {
			if (!spoolDir.mkdirs()) {
				throw new RuntimeException(String.format("Directory %s does not exist and could not be created", spoolPath));
			}
		}
		if (!spoolDir.isDirectory()) {
			throw new RuntimeException(spoolPath + " is not a directory");
		}
		if (!spoolDir.isDirectory()) {
			throw new RuntimeException(spoolPath + " is not writeable");
		}
	}
	
	@Override
	public void writeLine(byte[] line, int offset, int length) throws IOException {
		out.write(line, offset, length);
	}
	
	@Override
	public Reply complete(SmtpSession session) {
		complete = true;
			try {
				out.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Exception closing spooled file", e);
			}
		return COMPLETE_MAIL_OK;
	}

	@Override
	public void dispose() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Exception closing spooled file", e);
			}
		}
		if (complete) {
			return;
		}
		if (spoolFile != null) {
			boolean deleted = spoolFile.delete();
			if (!deleted) {
				log.log(Level.WARNING, "Unable to delete incomplete message file " + spoolFile);
			}
		}
	}

	@Override
	public void prepareForData(SmtpSession session) {
		try {
			spoolFile = File.createTempFile("xxx", ".eml", spoolDir);
			out = new BufferedOutputStream(new FileOutputStream(spoolFile));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
