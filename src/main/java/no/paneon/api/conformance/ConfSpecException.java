package no.paneon.api.conformance;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

public class ConfSpecException extends Exception {

    static final Logger LOG = LogManager.getLogger(ConfSpecException.class);

	protected static final long serialVersionUID = 3649840739892120559L;

	public ConfSpecException() {
		if(LOG.isDebugEnabled())
			LOG.log(Level.DEBUG, "Exception: {}", this.toString());
	}

}
