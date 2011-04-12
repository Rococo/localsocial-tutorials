package com.localsocial.actions;

/**
 * Command adapter that does nothing
 * @author jimoleary
 *
 */
public class  CommandAdapter implements Command {
    
    /**
     * execute the command
     * @param param the context of the command
     */
    public void execute(Object param) {}

    /**
     * check if the action associated with this command should be shown
     * @param param the context of the command
     * 
     * @return true if the action should be shown
     */
    public boolean show(Object param) {
        return true;
    }

}
