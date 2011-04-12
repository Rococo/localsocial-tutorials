package com.localsocial.actions;


/**
 * attach a command to implement the action item
 * @author jimoleary
 *
 */
public interface Command {
    
    /**
     * execute the command
     * @param param the context of the command
     */
    void execute(Object param);

    /**
     * check if the action associated with this command should be shown
     * @param param the context of the command
     * 
     * @return true if the action should be shown
     */
    boolean show(Object param);

}
