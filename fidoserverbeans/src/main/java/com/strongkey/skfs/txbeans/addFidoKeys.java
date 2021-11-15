/**
* Copyright StrongAuth, Inc. All Rights Reserved.
*
* Use of this source code is governed by the GNU Lesser General Public License v2.1
* The license can be found at https://github.com/StrongKey/fido2/blob/master/LICENSE
*/
package com.strongkey.skfs.txbeans;

import com.strongkey.appliance.entitybeans.Domains;
import com.strongkey.appliance.utilities.applianceCommon;
import com.strongkey.appliance.utilities.applianceConstants;
import com.strongkey.crypto.interfaces.initCryptoModule;
import com.strongkey.crypto.utility.CryptoException;
import com.strongkey.skfe.entitybeans.FidoKeys;
import com.strongkey.skfe.entitybeans.FidoKeysPK;
import com.strongkey.skfs.messaging.replicateSKFEObjectBeanLocal;
import com.strongkey.skfs.utilities.SKFEException;
import com.strongkey.skfs.utilities.SKFSCommon;
import com.strongkey.skfs.utilities.SKFSConstants;
import com.strongkey.skfs.utilities.SKFSLogger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class addFidoKeys implements addFidoKeysLocal {

    /**
     ** This class's name - used for logging & not persisted
     *
     */
    private final String classname = this.getClass().getName();

    /**
     * EJB's used by the Bean
     */
    @EJB
    getFidoKeysLocal getregkeysejb;
    @EJB
    SequenceGeneratorBeanLocal seqgenejb;
    @EJB
    replicateSKFEObjectBeanLocal replObj;
    @EJB
    getDomainsBeanLocal getdomain;

    /**
     * Persistence context for derby
     */
    @Resource
    private SessionContext sc;
    @PersistenceContext
    private EntityManager em;

    /**
     * This bean will add the generated key to the derby database after basic
     * input validation
     *
     * @param did - Domain identifier on SKFE
     * @param userid - A unique identifier for a user account with that Relying
     * Party
     * @param username - Username of the requestor
     * @param UKH - Key handle generated by the authenticator
     * @param UPK - Public key generated during registration
     * @param appid - appid for the registered key
     * @param transports - Transport mechanism of the registered key
     * @param attsid - Server id of the attestation certificate
     * @param attdid - Domain id of the attestation certificate
     * @param attcid - Certificate id of the attestation certificate
     * @param counter - Authenticator counter passed
     * @param fido_version - FIDO version used to generate key
     * @param aaguid - Manufacturer id for the FIDO2 key.
     * @param registrationSettings - Additional information only captured during
     * registration
     * @param registrationSettingsVersion - Version of registrationSettings
     * @param fido_protocol - FIDO Protocol used for authenticator
     * @param create_location - Location where the key was generated
     * @return - Returns a JSON string containing the status and the
     * error/success message
     */
    @Override
    public String execute(Long did,
            String userid,
            String username,
            String UKH,
            String UPK,
            String appid,
            Short transports,
            Short attsid,
            Short attdid,
            Integer attcid,
            int counter,
            String fido_version,
            String fido_protocol,
            String aaguid,
            String registrationSettings,
            Integer registrationSettingsVersion,
            String create_location) {
        SKFSLogger.entering(SKFSConstants.SKFE_LOGGER, classname, "execute");

        //Json return object
        JsonObject retObj;

        //Declaring variables
        Boolean status = true;
        String errmsg;

        //Input Validation
        if (did == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "did");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " did";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (did < 1) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "did");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " did";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "did=" + did);

        //USERNAME
        if (username == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "USERNAME");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " USERNAME";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (username.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "USERNAME");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " USERNAME";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (username.trim().length() > applianceCommon.getMaxLenProperty("appliance.cfg.maxlen.256charstring")) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "USERNAME");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002").replace("{0}", "") + " USERNAME";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "USERNAME=" + username);

        //USER KEY HANDLE
        if (UKH == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "USER KEY HANDLE");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " USER KEY HANDLE";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (UKH.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "USER KEY HANDLE");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " USER KEY HANDLE";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (UKH.trim().length() > applianceCommon.getMaxLenProperty("appliance.cfg.maxlen.512charstring")) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "USER KEY HANDLE");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " USER KEY HANDLE";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "USER KEY HANDLE=" + UKH);

        //USER PUBLIC KEY
        if (UPK == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "USER PUBLIC KEY");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " USER PUBLIC KEY";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (UPK.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "USER PUBLIC KEY");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " USER PUBLIC KEY";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (UPK.trim().length() > applianceCommon.getMaxLenProperty("appliance.cfg.maxlen.512charstring")) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "USER PUBLIC KEY");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " USER PUBLIC KEY";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "USER PUBLIC KEY=" + UPK);

        //USER create_location
        if (create_location == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "CREATE LOCATION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " CREATE LOCATION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (create_location.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "CREATE LOCATION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " CREATE LOCATION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (create_location.trim().length() > applianceCommon.getMaxLenProperty("appliance.cfg.maxlen.256charstring")) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "CREATE LOCATION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " CREATE LOCATION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "CREATE LOCATION=" + create_location);

        //USER fido_version
        if (fido_version == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "FIDO VERSION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " FIDO VERSION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (fido_version.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "FIDO VERSION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " FIDO VERSION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (!fido_version.trim().equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_VERSION_U2F_V2) && !fido_version.trim().equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_VERSION_2_0)) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "FIDO VERSION");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " FIDO VERSION";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "FIDO VERSION=" + fido_version);

        //USER fido_protocol
        if (fido_protocol == null) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "FIDO PROTOCOL");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " FIDO PROTOCOL";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (fido_protocol.trim().length() == 0) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1003", "FIDO PROTOCOL");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1003") + " FIDO PROTOCOL";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (fido_protocol.trim().equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_UAF)) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1004", "FIDO PROTOCOL");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1004") + " FIDO PROTOCOL";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        } else if (!fido_protocol.trim().equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_U2F) && !fido_protocol.trim().equalsIgnoreCase(SKFSConstants.FIDO_PROTOCOL_VERSION_2_0)) {
            status = false;
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1002", "FIDO PROTOCOL");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1002") + " FIDO PROTOCOL";
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2001", "FIDO PROTOCOL=" + fido_protocol);
        /*verify if username kh pair already exists-- this case should never
        occur if gnubby has done the job right but just in case adding code here*/
        FidoKeys rk = null;
        try {
            rk = getregkeysejb.getByUsernameKH(did, username, UKH);
        } catch (SKFEException ex) {
            Logger.getLogger(addFidoKeys.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (rk != null) {
            SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-2001", "");
            errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-2001");
            status = false;
            retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
            return retObj.toString();
        }

        Short sid = applianceCommon.getServerId().shortValue();
        long fkid = seqgenejb.nextFIDOKeyID(did);
        //Persist entry after successfully validating all inputs

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String crdate = df.format(new Date());
        Date createDateFormat = null;
        try {
            createDateFormat = df
                    .parse(crdate);
        } catch (ParseException e) {
        }

        FidoKeys newKey = new FidoKeys();
        FidoKeysPK fkpk = new FidoKeysPK(sid, did.shortValue(), username, fkid);
        String primarykey = sid + "-" + did + "-" + username + "-" + fkid;
        newKey.setFidoKeysPK(fkpk);
        newKey.setUserid(userid);
        newKey.setKeyhandle(UKH);
        newKey.setPublickey(UPK);
        newKey.setAppid(appid);
        newKey.setTransports(transports);
        newKey.setAttsid(attsid);
        newKey.setAttdid(attdid);
        newKey.setAttcid(attcid);
        newKey.setCounter(counter);
        newKey.setFidoVersion(fido_version);
        newKey.setFidoProtocol(fido_protocol);
        if (aaguid != null) {
            newKey.setAaguid(aaguid);
        }
        if (registrationSettings != null) {
            newKey.setRegistrationSettings(registrationSettings);
        }
        if (registrationSettingsVersion != null) {
            newKey.setRegistrationSettingsVersion(registrationSettingsVersion);
        }
        newKey.setCreateLocation(create_location);
        newKey.setCreateDate(createDateFormat);
        newKey.setStatus(applianceConstants.ACTIVE_STATUS);
        newKey.setId(primarykey);

        if (SKFSCommon.getConfigurationProperty("skfs.cfg.property.db.signature.rowlevel.add")
                .equalsIgnoreCase("true")) {
            String standalone = SKFSCommon.getConfigurationProperty("skfs.cfg.property.standalone.fidoengine");
            String signingKeystorePassword = "";
            if (standalone.equalsIgnoreCase("true")) {
                signingKeystorePassword = SKFSCommon.getConfigurationProperty("skfs.cfg.property.standalone.signingkeystore.password");
            }
            //  convert the java object into xml to get it signed.
//            StringWriter writer = new StringWriter();
//            JAXBContext jaxbContext;
//            Marshaller marshaller;
//            try {
//                jaxbContext = JAXBContext.newInstance(FidoKeys.class);
//                marshaller = jaxbContext.createMarshaller();
//                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//                marshaller.marshal(newKey, writer);
//            } catch (javax.xml.bind.JAXBException ex) {
//                Logger.getLogger(addFidoKeys.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            String efsXml = writer.toString();

            String efsXml = newKey.toJsonObject();
            if (efsXml == null) {
                status = false;
                SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "FK Xml");
                errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " FK Xml";
                retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
                return retObj.toString();
            }
            //  get signature for the xml
            Domains d = getdomain.byDid(did);
            //  get signature for the xml
            String signedxml = null;
            try {
                signedxml = initCryptoModule.getCryptoModule().signDBRow(did.toString(), d.getSkceSigningdn(), efsXml, Boolean.valueOf(standalone), signingKeystorePassword);
            } catch (CryptoException ex) {
                Logger.getLogger(addFidoKeys.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (signedxml == null) {
                status = false;
                SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.SEVERE, classname, "execute", "FIDOJPA-ERR-1001", "SignedXML");
                errmsg = SKFSCommon.getMessageProperty("FIDOJPA-ERR-1001") + " SignedXML";
                retObj = Json.createObjectBuilder().add("status", status).add("message", errmsg).build();
                return retObj.toString();
            } else {
                newKey.setSignatureKeytype("EC");
                newKey.setSignature(signedxml);
            }
        } else{
            newKey.setSignatureKeytype("EC");
        }
        
//        System.out.println("*********************");
//        System.out.println(newKey.getFidoKeysPK().getSid());
//        System.out.println(newKey.getFidoKeysPK().getDid());
//        System.out.println(newKey.getFidoKeysPK().getUsername());
//        System.out.println(newKey.getFidoKeysPK().getFkid());
//        System.out.println(newKey.getUserid());
//        System.out.println(newKey.getKeyhandle());
//        System.out.println(newKey.getAppid());
//        System.out.println(newKey.getPublickey());
//        System.out.println(newKey.getTransports());
//        System.out.println(newKey.getAttsid());
//        System.out.println(newKey.getAttdid());
//        System.out.println(newKey.getAttcid());
//        System.out.println(newKey.getCounter());
//        System.out.println(newKey.getFidoVersion());
//        System.out.println(newKey.getFidoProtocol());
//        System.out.println(newKey.getAaguid());
//        System.out.println(newKey.getRegistrationSettings());
//        System.out.println(newKey.getRegistrationSettingsVersion());
//        System.out.println(newKey.getCreateDate());
//        System.out.println(newKey.getCreateLocation());
//        System.out.println(newKey.getModifyDate());
//        System.out.println(newKey.getModifyLocation());
//        System.out.println(newKey.getStatus());
//        System.out.println(newKey.getSignatureKeytype());
//        System.out.println(newKey.getSignature());
//        System.out.println("*********************");

        em.persist(newKey);
        em.flush();
        em.clear();

        //add fido keys transport - RFE
        try {
            if (applianceCommon.replicate()) {
                if (!Boolean.valueOf(SKFSCommon.getConfigurationProperty("skfs.cfg.property.replicate.hashmapsonly"))) {
                    String response = replObj.execute(applianceConstants.ENTITY_TYPE_FIDO_KEYS, applianceConstants.REPLICATION_OPERATION_ADD, primarykey, newKey);
                    if (response != null) {
                        return response;
                    }
                }
            }
        } catch (Exception e) {
            sc.setRollbackOnly();
            SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER, classname, "execute");
            throw new RuntimeException(e.getLocalizedMessage());
        }

        //return a successful json string
        SKFSLogger.logp(SKFSConstants.SKFE_LOGGER, Level.FINE, classname, "execute", "FIDOJPA-MSG-2002", "");
        retObj = Json.createObjectBuilder().add("status", status).add("message", SKFSCommon.getMessageProperty("FIDOJPA-MSG-2002")).build();
        SKFSLogger.exiting(SKFSConstants.SKFE_LOGGER, classname, "execute");
        return retObj.toString();
    }
}