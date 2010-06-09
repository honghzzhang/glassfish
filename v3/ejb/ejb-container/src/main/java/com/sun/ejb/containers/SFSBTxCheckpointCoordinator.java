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

package com.sun.ejb.containers;

import java.io.Serializable;
import java.util.ArrayList;

import com.sun.ejb.spi.sfsb.store.SFSBBeanState;

import java.util.logging.*;

import com.sun.logging.*;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;

/**
 * A class to checkpoint HA enabled SFSBs as a single transactional unit.
 *
 * @author Mahesh Kannan
 */
public class SFSBTxCheckpointCoordinator {

    private static final Logger _logger =
            LogDomains.getLogger(SFSBTxCheckpointCoordinator.class, LogDomains.EJB_LOGGER);

    private String haStoreType;

    private ArrayList ctxList = new ArrayList();

    SFSBTxCheckpointCoordinator(String haStoreType) {
        this.haStoreType = haStoreType;
    }

    void registerContext(SessionContextImpl ctx) {
        ctxList.add(ctx);
    }

    void doTxCheckpoint() {
        SessionContextImpl[] contexts = (SessionContextImpl[]) ctxList.toArray(
                new SessionContextImpl[0]);
        int size = contexts.length;
        ArrayList<StoreAndBeanState> states = new ArrayList<StoreAndBeanState>(size);

        for (int i = 0; i < size; i++) {
            SessionContextImpl ctx = contexts[i];
            StatefulSessionContainer container =
                    (StatefulSessionContainer) ctx.getContainer();
            SFSBBeanState beanState = container.getSFSBBeanState(ctx);
            if (beanState != null) {
                states.add(new StoreAndBeanState(container.getBackingStore(), beanState));
            }
        }

        if (states.size() > 0) {
            SFSBBeanState[] beanStates = (SFSBBeanState[]) states.toArray(
                    new SFSBBeanState[0]);

            try {
                for (StoreAndBeanState st : states) {
                    st.store.save(st.state.getSessionId(), st.state, st.state.isNew());
                }
            } catch (BackingStoreException sfsbEx) {
                _logger.log(Level.WARNING, "Exception during checkpointSave",
                        sfsbEx);
            } catch (Throwable th) {
                _logger.log(Level.WARNING, "Exception during checkpointSave",
                        th);
            }
        }

        for (int i = 0; i < size; i++) {
            SessionContextImpl ctx = contexts[i];
            StatefulSessionContainer container =
                    (StatefulSessionContainer) ctx.getContainer();
            container.txCheckpointCompleted(ctx);
        }
    }

    private static final class StoreAndBeanState {
        BackingStore<Serializable, SFSBBeanState> store;
        SFSBBeanState state;

        StoreAndBeanState(BackingStore<Serializable, SFSBBeanState> store, SFSBBeanState state) {
            this.store = store;
            this.state = state;
        }
    }

}
