/**
* Copyright StrongAuth, Inc. All Rights Reserved.
*
* Use of this source code is governed by the GNU Lesser General Public License v2.1
* The license can be found at https://github.com/StrongKey/fido2/blob/master/LICENSE
*
 * *********************************************
 *                    888
 *                    888
 *                    888
 *  88888b.   .d88b.  888888  .d88b.  .d8888b
 *  888 "88b d88""88b 888    d8P  Y8b 88K
 *  888  888 888  888 888    88888888 "Y8888b.
 *  888  888 Y88..88P Y88b.  Y8b.          X88
 *  888  888  "Y88P"   "Y888  "Y8888   88888P'
 *
 * *********************************************
 *
 * This EJB is responsible for executing the activation process of a specific
 * user registered key which ahs earlier been de-activated. FIDO U2F protocol
 * does not provide any specification for user key de-activation.
 *
 * This bean will just mark the specific key in the database to be ACTIVE.
 *
 */

package com.strongkey.skfs.txbeans.v1;

import com.strongkey.appliance.utilities.applianceCommon;
import com.strongkey.appliance.utilities.applianceConstants;
import com.strongkey.skfe.entitybeans.FidoKeys;
import com.strongkey.skfs.txbeans.getFidoKeysLocal;
import com.strongkey.skfs.txbeans.updateFidoKeysStatusLocal;
import com.strongkey.skfs.txbeans.updateFidoUserBeanLocal;
import com.strongkey.skfs.utilities.SKCEReturnObject;
import com.strongkey.skfs.utilities.SKFEException;
import com.strongkey.skfs.utilities.SKFSCommon;
import com.strongkey.skfs.utilities.SKFSConstants;
import com.strongkey.skfs.utilities.SKFSLogger;
import java.io.StringReader;
import java.util.Collection;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * This EJB is responsible for executing the activation process of a specific
 * user registered key
 */
@Stateless
public class u2fActivateBean_v1 implements u2fActivateBeanLocal_v1, u2fActivateBeanRemote_v1 {

    /*
     * This class' name - used for logging
     */
    private final String classname = this.getClass().getName();

    /*
     * Enterprise Java Beans used in this EJB.
     */
    @EJB getFidoKeysLocal                   getkeybean;
    @EJB updateFidoKeysStatusLocal          updatekeystatusbean;
    @EJB updateFidoUserBeanLocal             updateldapbean;

    /*************************************************************************
                                                 888
                                                 888
                                                 888
     .d88b.  888  888  .d88b.   .d8888b 888  888 888888  .d88b.
    d8P  Y8b `Y8bd8P' d8P  Y8b d88P"    888  888 888    d8P  Y8b
    88888888   X88K   88888888 888      888  888 888    88888888
    Y8b.     .d8""8b. Y8b.     Y88b.    Y88b 888 Y88b.  Y8b.
     "Y8888  888  888  "Y8888   "Y8888P  "Y88888  "Y888  "Y8888

     *************************************************************************/
    /**
     * This method is responsible for activating the user registered key from the
     * persistent storage. This method first checks if the given ramdom id is
     * mapped in memory to the specified user and if found yes, gets the registration
     * key id and then changes the key status to ACTIVE in the database.
     *
     * Additionally, if the key being activated is the only one for the user in
     * ACTIVE status, the ldap attribute of the user called 'FIDOKeysEnabled' is
     * set to 'yes'.
     *
     * @param did       - FIDO domain id
     * @param protocol  - U2F protocol version to comply with.
     * @param username  - username
     * @param randomid  - random id that is unique to one fido registered authenticator
     *                      for the user.
     * @param modifyloc - Geographic location from where the activation is happening
     * @return          - returns SKCEReturnObject in both error and success cases.
     *                  In error case, an error key and error msg would be populated
     *                  In success case, a simple msg saying that the process was
     *                  successful would be populated.
     */
    @Override
    public SKCEReturnObject execute(String did,
                                    String protocol,
                                    String username,
                                    String randomid,
                                    String modifyloc) {

        //  Log the entry and inputs
        SKFSLogger.entering(SKFSConstants.SKFE_LOGGER,classname, "execute");
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.FINE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-MSG-5001"),
                        " EJB name=" + classname +
                        " did=" + did +
                        " protocol=" + protocol +
                        " username=" + username +
                        " randomid=" + randomid +
                        " modifyloc=" + modifyloc);

        SKCEReturnObject skcero = new SKCEReturnObject();

        //  input checks
                if (did == null || Long.parseLong(did) < 1) {
            skcero.setErrorkey("FIDO-ERR-0002");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0002") + " did=" + did);
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " did=" + did);
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }
        if (username == null || username.isEmpty() ) {
            skcero.setErrorkey("FIDO-ERR-0002");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0002") + " username=" + username);
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " username=" + username);
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }

        if (username.trim().length() > Integer.parseInt(applianceCommon.getApplianceConfigurationProperty("appliance.cfg.maxlen.256charstring"))) {
            skcero.setErrorkey("FIDO-ERR-0027");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0027") + " username should be limited to 256 characters");
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0027", " username should be limited to 256 characters");
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }

        if (randomid == null || randomid.isEmpty() ) {
            skcero.setErrorkey("FIDO-ERR-0002");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0002") + " randomid=" + randomid);
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " randomid=" + randomid);
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }

        if (protocol == null || protocol.isEmpty() ) {
            skcero.setErrorkey("FIDO-ERR-0002");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0002") + " protocol=" + protocol);
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-0002", " protocol=" + protocol);
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }

        if (!protocol.equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_VERSION_U2F_V2) && !protocol.equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_VERSION_2_0)) {
            skcero.setErrorkey("FIDO-ERR-5002");
            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-5002") + " protocol version passed =" + protocol);
            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.SEVERE, "FIDO-ERR-5002", " protocol version passed =" + protocol);
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
            return skcero;
        }

            Short sid_to_be_activated = null;
//            int userfkidhyphen ;
//            String fidouser;
            Long fkid_to_be_activated = null;
            try {
                String[] mapvaluesplit = randomid.split("-", 3);
                sid_to_be_activated = Short.parseShort(mapvaluesplit[0]);
                did = mapvaluesplit[1];
//                userfkidhyphen = mapvaluesplit[2].lastIndexOf("-");

//                fidouser = mapvaluesplit[2].substring(0, userfkidhyphen);
                fkid_to_be_activated = Long.parseLong(mapvaluesplit[2]);
            } catch (Exception ex) {
                    skcero.setErrorkey("FIDO-ERR-0029");
                            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0029") + "Invalid randomid= " + randomid);
                            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.SEVERE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-ERR-0029"),"Invalid randomid= " + randomid);
                            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
                            return skcero;
            }

            String current_pk = sid_to_be_activated + "-"+ did + "-"+ username + "-"+ fkid_to_be_activated;
            if(!randomid.equalsIgnoreCase(current_pk)){
                //user is not authorized to deactivate this key
                //  throw an error and return.
                skcero.setErrorkey("FIDO-ERR-0035");
                skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0035") + " username= " + username );
                SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.SEVERE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-ERR-0035"), " username= " + username );
                SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
                return skcero;
            }

            //  get the reg key id to be deleted based on the random id provided.
//            Integer fkid_to_be_activated = Integer.parseInt(mapvaluesplit[1]);
            if ( fkid_to_be_activated != null ) {
                if (fkid_to_be_activated >= 0) {

                    SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.FINE, classname, "execute",
                            SKFSCommon.getMessageProperty("FIDO-MSG-5005"), "");
                    try {
                        //  if the fkid_to_be_activated is valid, delete the entry from the database
                        String jparesult = updatekeystatusbean.execute(sid_to_be_activated, Long.parseLong(did), fkid_to_be_activated, modifyloc, applianceConstants.ACTIVE_STATUS);
                        JsonObject jo;
                        try (JsonReader jr = Json.createReader(new StringReader(jparesult))) {
                            jo = jr.readObject();
                        }

                        Boolean status = jo.getBoolean(SKFSConstants.JSON_KEY_FIDOJPA_RETURN_STATUS);
                        if ( !status ) {
                            //  error deleting user key
                            //  throw an error and return.
                            skcero.setErrorkey("FIDO-ERR-0029");
                            skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0029") + " username= " + username + "   randomid= " + randomid);
                            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.SEVERE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-ERR-0029"), " username= " + username + "   randomid= " + randomid);
                            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
                            return skcero;
                        } else {
                            //  Successfully activated key from the database
                            SKFSLogger.log(SKFSConstants.SKFE_LOGGER,Level.FINE, SKFSCommon.getMessageProperty("FIDO-MSG-0050"), "key id = " + fkid_to_be_activated);
                        }

                        if (SKFSCommon.updateFidoUsers()) {
                            Collection<FidoKeys> keys = getkeybean.getByUsernameStatus(Long.parseLong(did), username, applianceConstants.ACTIVE_STATUS);
                            if (keys == null || keys.isEmpty()) {
                                SKFSLogger.log(SKFSConstants.SKFE_LOGGER, Level.FINE, SKFSCommon.getMessageProperty("FIDO-MSG-5006"), "");
                                //  Update the "FIDOKeysEnabled" attribute of the user to 'false'
                                //  if the key that was just activated is the only key registered
                                //  for the user in an ACTIVE status.
                                try {
                                    String result = updateldapbean.execute(Long.parseLong(did), username, SKFSConstants.LDAP_ATTR_KEY_FIDOENABLED, "true", false);
                                    try (JsonReader jr = Json.createReader(new StringReader(result))) {
                                        jo = jr.readObject();
                                    }
                                    status = jo.getBoolean(SKFSConstants.JSON_KEY_FIDOJPA_RETURN_STATUS);
                                    if (status) {
                                        SKFSLogger.log(SKFSConstants.SKFE_LOGGER, Level.FINE, SKFSCommon.getMessageProperty("FIDO-MSG-0029"), "true");
                                    } else {
                                        SKFSLogger.log(SKFSConstants.SKFE_LOGGER, Level.SEVERE, SKFSCommon.getMessageProperty("FIDO-ERR-0024"), "true");
                                    }
                                } catch (SKFEException ex) {
                                    //  Do we need to return with an error at this point?
                                    //  Just throw an err msg and proceed.
                                    SKFSLogger.log(SKFSConstants.SKFE_LOGGER, Level.SEVERE, SKFSCommon.getMessageProperty("FIDO-ERR-0024"), "false");
                                }
                            }
                        }
                    } catch (SKFEException ex) {
                        //  error activating user key
                        //  throw an error and return.
                        skcero.setErrorkey("FIDO-ERR-0029");
                        skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0029") + " username= " + username + "   randomid= " + randomid);
                        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.SEVERE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-ERR-0029"), " username= " + username + "   randomid= " + randomid);
                        SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
                        return skcero;
                    }
                }
            } else {
                //  user key information does not exist or has been timed out (flushed away).
                //  throw an error and return.
                skcero.setErrorkey("FIDO-ERR-0022");
                skcero.setErrormsg(SKFSCommon.getMessageProperty("FIDO-ERR-0022") + " username= " + username + "   randomid= " + randomid);
                SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.SEVERE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-ERR-0022"), " username= " + username + "   randomid= " + randomid);
                SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
                return skcero;
            }
//        }

        skcero.setReturnval("Successfully activated the key");

        //  log the exit and return
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER,Level.FINE, classname, "execute", SKFSCommon.getMessageProperty("FIDO-MSG-5002"), classname);
        SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER,classname, "execute");
        return skcero;
    }

    @Override
    public SKCEReturnObject remoteExecute(String did,
                                    String protocol,
                                    String username,
                                    String randomid,
                                    String modifyloc) {
        return execute(did, protocol, username, randomid, modifyloc);
    }
}
