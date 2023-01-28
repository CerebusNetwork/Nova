package xyz.xenondevs.nova.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class NovaClassLoader extends URLClassLoader {
    
    public NovaClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        
        // check if the class is already loaded
        synchronized (getClassLoadingLock(name)) {
            // check in jvm
            c = findLoadedClass(name);
            
            // check Nova classes and libraries before parent to prevent it from using old patch classes
            // (which stay in memory after reloading because patched code references them)
            if (c == null) {
                c = findClassOrNull(name);
            }
        }
        
        // check parent loader
        if (c == null) {
            c = getParent().loadClass(name);
        }
        
        if (resolve) {
            synchronized (getClassLoadingLock(name)) {
                resolveClass(c);
            }
        }
        
        return c;
    }
    
    private Class<?> findClassOrNull(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
}
