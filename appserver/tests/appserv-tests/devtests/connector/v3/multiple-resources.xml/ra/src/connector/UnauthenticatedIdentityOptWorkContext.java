/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

package connector;

import org.glassfish.security.common.PrincipalImpl;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.Subject;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.resource.spi.work.SecurityContext;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


public class UnauthenticatedIdentityOptWorkContext extends SecurityContext {

    private String userName;
    private String password;
    private String principalName;
    private boolean translationRequired;
    private Subject subject;
    private boolean principal;

    public UnauthenticatedIdentityOptWorkContext(boolean translationRequired, boolean principal, String principalName, String userName, String password) {
        this.translationRequired = translationRequired;
        this.principal = principal;
        this.userName = userName;
        this.password = password;
        this.principalName = principalName;
    }

    public boolean isTranslationRequired() {
        return translationRequired;
    }

    public void setupSecurityContext(CallbackHandler callbackHandler, Subject execSubject, Subject serviceSubject) {

        List<Callback> callbacks = new ArrayList<Callback>();

        //if (!translationRequired) {
            if (principal) {
                CallerPrincipalCallback cpc = null;
                if(principalName != null){
                    cpc = new CallerPrincipalCallback(execSubject, new PrincipalImpl(principalName));
                    callbacks.add(cpc);
                    debug("setting caller principal callback with principal : " + principalName);
                }else{
                    execSubject.getPrincipals().add(new PrincipalImpl(principalName));
                    debug("setting principal for execSubject : " + principalName);
                }
            } else {
                //empty execSubject
                //do nothing
            }
        //}
        addCallbackHandlers(callbacks, execSubject);

        Callback callbackArray[] = new Callback[callbacks.size()];
        try {
            callbackHandler.handle(callbacks.toArray(callbackArray));

        } catch (UnsupportedCallbackException e) {
            debug("exception occured : " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            debug("exception occured : " + e.getMessage());
        }
            //debug("Password validation callback succeded for user : " + userName);
    }

    protected void addCallbackHandlers(List<Callback> callbacks, Subject execSubject) {
        //do nothing
        //hook to test Dupilcate Inflow Context behavior
    }

    public Subject getSubject() {
        //if (translationRequired) {
            if (subject == null) {
                subject = new Subject();
                subject.getPrincipals().add(new PrincipalImpl(principalName));
                debug("setting translation required for principal : " + principalName);
            }
            return subject;
        /*} else {
            return null;
        }*/
    }

    public String toString() {
        StringBuffer toString = new StringBuffer("{");
        toString.append("userName : " + userName);
        toString.append(", password : " + password);
        toString.append(", principalName : " + principalName);
        toString.append(", translationRequired : " + translationRequired);
        toString.append("}");
        return toString.toString();
    }

    public void debug(String message) {
        System.out.println("JSR-322 [RA] [MySecurityContext]: " + message);
    }

}
