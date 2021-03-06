/**
* Copyright StrongAuth, Inc. All Rights Reserved.
*
* Use of this source code is governed by the GNU Lesser General Public License v2.1
* The license can be found at https://github.com/StrongKey/fido2/blob/master/LICENSE
*/
package com.strongkey.skfs.jwt;

import javax.ejb.Local;

/**
 *
 * @author dbeach
 */
@Local
public interface JWTCreateLocal {
    public String execute(String did, String username, String agent, String cip,String rpid);
}
