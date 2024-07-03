package org.example;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.InitialDirContext;

public class Utilities {
    public static DirContext createLdapContext(String Registry) throws Exception{
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + Registry + "/");

        return new InitialDirContext(env);
    }
}
