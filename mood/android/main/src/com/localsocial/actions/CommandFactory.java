package com.localsocial.actions;

import java.util.concurrent.Executor;

import com.localsocial.model.ObjectCache;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

public class CommandFactory {

    /**
     * create a command to execute in a ui handler thread
     * 
     * @param command the command
     * @return the ui command
     */
    public static void toast(String msg) {
        Context ctx = (Context) ObjectCache.getObject("com.localsocial.tulsi.Context");
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();

    }

    /**
     * create a command to execute in a ui handler thread
     * 
     * @param command the command
     * @return the ui command
     */
//    public static Command createCommand(final Handler handler, Command command) {
//        if (command == null)
//            return null;
//        return new DelegatingCommand(command) {
//
//            @Override
//            public void execute(final Object param) {
//                handler.post(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        try {
//                            m_command.execute(param);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            toast("Unexpected error processing command");
//                        }
//                    }
//                });
//            }
//        };
//    };

    /**
     * create a command to execute in a non-ui handler thread
     * 
     * @param command the command
     * @return the non-ui command
     */
//    public static Command createCommand(final Executor executor, Command command) {
//        if (command == null)
//            return null;
//        return new DelegatingCommand(command) {
//            @Override
//            public void execute(final Object param) {
//                executor.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            m_command.execute(param);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            toast("Unexpected error processing command");
//                        }
//                    }
//
//                    public String toString() {
//                        return new StringBuffer().append(m_command).append("  : ").append(param).toString();
//                    }
//
//                });
//            }
//        };
//    };

}
