package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;


import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 *  \brief Shell switch.
 *
 * Executes a shell command to set the position of a switch.
 *
 * WARNING: If a command to get switch state is not specified, this object
 *          will assume current switch state based on the last command
 *          issued.  This switch should not be used without explicit
 *          construction of the physical system and controlling
 *          software to ensure reliable and safe operation.
 *
 * @author Copyright &copy; Thomas Rooney 2015
 * Based on NullSwitch and ShellSensor by
 * <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2010
 *
 * This software is provided under GPLv3 with NO WARRANTY
 */
 // TODO: select Logger or NDC for logging
 // TODO: #ifdef commands to run on other operating systems
 // TODO: test Java 8 waitfor implementation
public class ShellSwitch implements Switch, JmxAware {

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * String to identify switch.
     */
    private String m_address = "";

    /**
     * Command to open switch.
     */
    private String m_openCommand = "";

    /**
     * Command to close switch.
     */
    private String m_closeCommand = "";

    /**
     * Command to get switch state.
     */
    private String m_getStateCommand = "";

    /**
     * Maximum number of milliseconds to wait for process completion
     */
     private long m_maxWaitMilliseconds = 0;

    /**
     *  \brief Last commanded state.
     *
     * false represents switch open
     * true represents switch closed
     */
    private boolean m_lastCommandedState = false;

    /**
     * Flag to indicate integer output value has been read from command
     * execution.
     */
    private boolean m_outputValueRead = false;

    /**
     * Return code from command execution.
     */
    private int m_commandOutputValue = 0;

    /**
     *  \brief Create an instance.
     *
     * @param address A string to identify the switch.
     *
     * @param openCommand Shell command to open the switch.
     * must return 0 on success, non-zero on failure
     *
     * @param closeCommand Shell command to close the switch.
     * must return 0 on success, non-zero on failure
     *
     * @param getStateCommand Shell command to read switch state.
     * must return 0 on success, non-zero on failure
     * must output a value that is parseable into {@code int}.
     * must output 0 for switch open or 1 for switch closed.
     *
     * @param maxWaitMilliseconds Parameter to control blocking behavior
     * if maxWaitMilliseconds <= 0 call will block
     * if maxWaitMilliseconds > 0, block for a maximum of maxWaitMilliseconds
     * and then kill the process if not complete.
     */
    public ShellSwitch(String address,
                       String openCommand,
                       String closeCommand,
                       String getStateCommand,
                       long maxWaitMilliseconds) {
        initialize(address,
                   openCommand,
                   closeCommand,
                   getStateCommand,
                   maxWaitMilliseconds);
    }

    /**
     * 4-parameter call to create and instance.
     */
    public ShellSwitch(String address,
                       String openCommand,
                       String closeCommand,
                       String getStateCommand) {
        initialize(address,
                   openCommand,
                   closeCommand,
                   getStateCommand,
                   0);
    }

    /**
     * 3-parameter call to create and instance.
     */
    public ShellSwitch(String address,
                       String openCommand,
                       String closeCommand) {
        initialize(address,
                   openCommand,
                   closeCommand,
                   (String) "",
                   0);
    }

    /**
     * Common code for all constructors.
     */
    private void initialize(String address,
                            String openCommand,
                            String closeCommand,
                            String getStateCommand,
                            long maxWaitMilliseconds) {
        if ((openCommand == null || "".equals(openCommand)) ||
            (openCommand == null || "".equals(openCommand)) ) {
            // Orderly error handling not possible in constructor,
            // must resort to throw
            throw new IllegalArgumentException(address +
                            "open/close command cannot be null or empty");
        }
        m_address = address;
        m_openCommand = openCommand;
        m_closeCommand = closeCommand;
        m_getStateCommand = getStateCommand;
        m_maxWaitMilliseconds = maxWaitMilliseconds;
        m_lastCommandedState = false;
        m_outputValueRead = false;
        m_commandOutputValue = 0;
    }

    /**
     *  \brief Utility method to execute command.
     *
     * sets m_outputValueRead
     * sets m_commandOutputValue = -1;
     *
     * returns 0 on success
     * returns -1 on exec error
     * returns -2 on parse error
     * returns -4 on other error
     */
    private int executeCommand(String command) {
        int retVal = 0;
        // set logging parameters
        NDC.push("executeCommand#" + Integer.toHexString(hashCode()));
        // initial values of modified member variables - assume failure
        m_outputValueRead = false;
        m_commandOutputValue = -1;
        // create process and execute command
        Process process = null;
        BufferedReader reader = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            logger.debug("Switch " + m_address + " executing: '/bin/sh -c " +
                         command + "'");
            // execute command
            process = runtime.exec(command);
            // wait for process completion
            int returnCode = 0;
            if (m_maxWaitMilliseconds > 0) {
                boolean processHasEnded = process.waitfor(m_maxWaitMilliseconds,
                                                          MILLISECONDS);
                // check for process completion
                if (processHasEnded) {
                    // read return code
                    returnCode = process.exitValue();
                } else {
                    // process is terminated in finally{} block
                    // process.destroy();
                    returnCode = 255;
                    logger.error("Switch " + m_address + " process timed out "
                                 + m_maxWaitMilliseconds +
                                 " milliseconds exceeded");
                }
            } else {
                returnCode = process.waitFor();
            }
            // capture command output
            reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
            String output = read(reader);
            logger.debug("Switch " + m_address + " output: " + output);
            // test for success or failure
            if (returnCode == 0) {
                // parse command output
                try {
                    int outVal = Integer.parseInt(output);
                    m_outputValueRead = true;
                    m_commandOutputValue = outVal;
                } catch (Exception err) {
                    // member variables set above
                    retVal = -2;
                }
            } else {
                // Error, switch position not reliable
                // member variables set above
                retVal = -1;
                logger.error("Switch " + m_address +
                             " command returned error code " + returnCode +
                             ": " + command);
            }
        } catch (Exception err) {
            // member variables set above
            retVal = -4;
        } finally {
            // terminate process if not null
            if (process != null) {
                process.destroy();
            }
            // close stream
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException err) {
                    logger.info("Switch " + m_address +
                                "cannot close() the process stream, ignored:",
                                err);
                }
            }
            NDC.pop();
        }
        return retVal;
    }


    /**
     *  \brief Set state executes the shell command m_getStateCommand or returns
     *  m_lastCommandedState if m_getStateCommand="".
     *
     */
    @Override
    public boolean getState() throws IOException {
        boolean retVal = false;
        if (m_getStateCommand == "") {
            retVal = m_lastCommandedState;
        } else {
            int execRet = executeCommand(m_getStateCommand);
            // proper operation requires:
            // execRet == 0
            // m_outputValueRead == true
            // m_commandOutputValue = 0 or 1
            if ((execRet == 0) &&
                (m_outputValueRead == true) &&
                ((m_commandOutputValue ==0) || (m_commandOutputValue == 1))) {
                    // set return value based on command output
                    if (m_commandOutputValue == 0) {
                        retVal = false;
                    } else {
                        retVal = true;
                    }
            } else {
                // WARNING - this implementation assumes the last
                // command setState() was successful
                retVal = m_lastCommandedState;
                // alternative implementation
                // throw new IOException("Unable to read switch state");
            }
        }
        return retVal;
    }

    /**
     *  \brief Set state executes a shell command, m_openCommand for state=false,
     *  m_closeCommand for state=true.
     *
     */
    @Override
    public void setState(boolean state) throws IOException {
        // record last commanded state
        m_lastCommandedState = state;
        // select command string to be executed
        String commandToExecute = "";
        if (state == false)
        {
            // open switch
            commandToExecute = m_openCommand;
            logger.debug("Setting switch " + m_address + " to open");
        } else {
            // close switch
            commandToExecute = m_closeCommand;
            logger.debug("Setting switch " + m_address + " to closed");
        }
        // execute command
        int execRet = executeCommand(commandToExecute);
        // void function cannot return error code
        if (execRet != 0) {
            logger.debug("Unable to set switch " + m_address);
            throw new IOException("Unable to set switch state");
        }
    }

    public String getOpenCommand() {
        return m_openCommand;
    }

    public String getCloseCommand() {
        return m_closeCommand;
    }

    public String getGetStateCommand() {
        return m_getStateCommand;
    }

    public long getMaxWaitMilliseconds() {
        return m_maxWaitMilliseconds;
    }

    public boolean getLastCommandedState() {
            return m_lastCommandedState;
    }

    public boolean getOutputValueRead() {
            return m_outputValueRead;
    }

    public long getCommandOutputValue() {
            return m_commandOutputValue;
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Shell Switch",
                Integer.toHexString(hashCode()),
                "Executes open, close and status commands to operate switch");
    }

}
