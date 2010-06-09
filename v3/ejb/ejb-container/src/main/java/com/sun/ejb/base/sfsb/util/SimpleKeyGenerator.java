/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.ejb.base.sfsb.util;

import com.sun.ejb.spi.sfsb.util.SFSBUUIDUtil;
import com.sun.enterprise.util.Utility;

/**
 * A utility class that generates stateful session keys using two longs
 * The session id generated by this class is guarenteed to be unique as 
 * long as the system clock is never reset to a previous value
 * 
 * The hashCode of the SessionKey generated by SimpleKeyGenerator 
 * also allows uniform distribution of keys when hashed in a HashMap
 *
 * @author  Mahesh Kannan
 */
public class SimpleKeyGenerator
    implements SFSBUUIDUtil<SimpleKeyGenerator.SimpleSessionKey>
{
   
    protected long prefix;
    protected long suffix;
    protected int idCounter;

    public SimpleKeyGenerator() {
	long now = System.currentTimeMillis();
	now = ((int) (now >>> 32)) + ((int) now);
	scramble((int) now, System.identityHashCode(this));
    }

    public SimpleKeyGenerator(byte[] ipAddress, int port) {
	scramble(Utility.bytesToInt(ipAddress, 0), port);
    }

    public SimpleKeyGenerator(long uniquePrefix) {
	this.prefix = uniquePrefix;

	//Initial suffix
	this.suffix = System.currentTimeMillis();

	//Inital isCounter value
	this.idCounter = 0;

    }

    /**
     * Create and return the sessionKey.
     * @return the sessionKey object
     */
    public SimpleSessionKey createSessionKey() {
        int id = 0;
        synchronized (this) {
            id = idCounter++;
            if (id < 0) {
                //idCounter wrapped around!!
                id = idCounter = 0;
                suffix = System.currentTimeMillis();
            }
        }

        return new SimpleSessionKey(prefix, suffix, id);
    }
    
    /**
     * Called from the Container before publishing an IOR. 
     * The method must convert the sessionKey into a byte[]
     * @return A byte[] representation of the key. The byte[] 
     * could be created using serialization.
     */
    public byte[] keyToByteArray(SimpleSessionKey key) {
        byte[] array = new byte[20];

        Utility.longToBytes(key.prefix, array, 0);
        Utility.longToBytes(key.suffix, array, 8);
        Utility.intToBytes(key.id, array, 16);

        return array;
    }
    
     /**
      * Return the sessionKey that represents the sessionKey. 
      * This has to be super efficient as the container calls this 
      * method on every invocation. Two objects obtained from identical 
      * byte[] must satisfy both o1.equals(o2) and 
      * o1.hashCode() == o2.hashCode()
      * @return the sessionKey object
      */
    public SimpleSessionKey byteArrayToKey(byte[] array, int startIndex, int len) {
        long myPrefix = Utility.bytesToLong(array, startIndex);
        long mySuffix = Utility.bytesToLong(array, startIndex+8);
        int  myId = Utility.bytesToInt(array, startIndex+16);

        return new SimpleSessionKey(myPrefix, mySuffix, myId);
    }

    private void scramble(int hi, int lo) {
	byte[] hiBytes = new byte[4];
	Utility.intToBytes(hi, hiBytes, 0);
	byte[] loBytes = new byte[4];
	Utility.intToBytes(lo, loBytes, 0);

	swapBytes(hiBytes, loBytes, 2, 3);
	swapBytes(hiBytes, loBytes, 3, 0);
	swapBytes(hiBytes, loBytes, 1, 3);
        
    swapBytes(hiBytes, hiBytes, 0, 3);
	swapBytes(loBytes, loBytes, 2, 3);

        this.prefix = Utility.bytesToInt(hiBytes, 0);

        this.prefix = 
	    (this.prefix << 32) + Utility.bytesToInt(loBytes, 0);

        //Inital isCounter value
        this.idCounter = 0;

        //Set the default suffix value
        this.suffix = (int) System.currentTimeMillis();
    }

    private static final void swapBytes(byte[] a, byte[] b,
	    int index1, int index2)
    {
	byte temp = a[index1];
	a[index1] = b[index2];
	b[index2] = temp;
    }


    protected static class SimpleSessionKey
	    implements java.io.Serializable
    {
    
	long prefix;
	long suffix;
	int id;

	public SimpleSessionKey(long prefix, long suffix, int id) {
	    this.prefix = prefix;
	    this.suffix = suffix;
	    this.id = id;
	}

	public int hashCode() {
	    return (int) id;
	}

	public boolean equals(Object otherObj) {
	    if (otherObj instanceof SimpleSessionKey) {
		SimpleSessionKey other = (SimpleSessionKey) otherObj;
		return (
		    (id == other.id) && (prefix == other.prefix)
		    && (suffix == other.suffix)
		);
	    }

	    return false;
	}

	public String toString() {
	    StringBuffer sbuf = new StringBuffer();
	    sbuf.append(Long.toHexString(prefix)).append("-")
		.append(Long.toHexString(suffix)).append("-")
		.append(Integer.toHexString(id));
	    return sbuf.toString();
	}
    }

}
    
