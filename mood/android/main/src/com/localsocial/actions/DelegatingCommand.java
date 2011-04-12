package com.localsocial.actions;

/**
 * Command that delegates to another command
 * @author jimoleary
 *
 */
public class DelegatingCommand implements Command {
    
    /**
     * create the delegating command
     * @param command the command to delegate to
     */
    public DelegatingCommand(Command command) {
        this.m_command = command;
    }
    
    /**
     * execute the command
     * @param param the context of the command
     */
    public void execute(Object param){
        if(m_command != null) {
            m_command.execute(param);
        }
    }

    /**
     * check if the action associated with this command should be shown
     * @param param the context of the command
     * 
     * @return true if the action should be shown
     */
    public boolean show(Object param) {
        if(m_command != null) {
            return m_command.show(param);
        }
        return true;
    }
    
    protected Command m_command;
}
