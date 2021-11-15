/**
* Copyright StrongAuth, Inc. All Rights Reserved.
*
* Use of this source code is governed by the GNU Lesser General Public License v2.1
* The license can be found at https://github.com/StrongKey/fido2/blob/master/LICENSE
*/
package com.strongkey.skfs.messaging;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;

@Remote
public interface publishSKCEObjectRemote {

    @Asynchronous
    public void remoteExecute(String repobjpk, int objectype, int objectop, String objectpk, Object o);
}
