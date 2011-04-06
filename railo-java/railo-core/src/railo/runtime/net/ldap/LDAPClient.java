package railo.runtime.net.ldap;

import java.io.IOException;
import java.security.Security;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;

import railo.commons.lang.ClassException;
import railo.commons.lang.ClassUtil;
import railo.runtime.exp.PageException;
import railo.runtime.op.Caster;
import railo.runtime.type.List;
import railo.runtime.type.Query;
import railo.runtime.type.QueryImpl;

import com.sun.jndi.ldap.ctl.SortControl;
import com.sun.jndi.ldap.ctl.SortKey;
import com.sun.net.ssl.internal.ssl.Provider;


/**
 * Ldap Client
 */
public final class LDAPClient {

	/**
	 * Field <code>SECURE_NONE</code>
	 */
	public static final short SECURE_NONE=0;
	/**
	 * Field <code>SECURE_CFSSL_BASIC</code>
	 */
	public static final short SECURE_CFSSL_BASIC=1;
	/**
	 * Field <code>SECURE_CFSSL_CLIENT_AUTH</code>
	 */
	public static final short SECURE_CFSSL_CLIENT_AUTH=2;
	
	/**
	 * Field <code>SORT_TYPE_CASE</code>
	 */
	public static final int SORT_TYPE_CASE = 0;
	/**
	 * Field <code>SORT_TYPE_NOCASE</code>
	 */
	public static final int SORT_TYPE_NOCASE = 1;

	/**
	 * Field <code>SORT_DIRECTION_ASC</code>
	 */
	public static final int SORT_DIRECTION_ASC = 0;
	/**
	 * Field <code>SORT_DIRECTION_DESC</code>
	 */
    public static final int SORT_DIRECTION_DESC = 1;
    
	Hashtable env=new Hashtable();
	
	
	/**
	 * constructor of the class
	 * @param server
	 * @param port
	 * @param binaryColumns
	 */
	public LDAPClient(String server, int port,String[] binaryColumns) {

			env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
			env.put("java.naming.provider.url", "ldap://" + server+":"+port);
      
       // rEAD AS bINARY
            for(int i = 0; i < binaryColumns.length; i++)env.put("java.naming.ldap.attributes.binary", binaryColumns[i]);

       // Referral
            env.put("java.naming.referral", "ignore");
	}
    
    /**
     * sets username password for the connection
     * @param username
     * @param password
     */
    public void setCredential(String username, String password) {
        if(username != null) {
            env.put("java.naming.security.principal", username);
            env.put("java.naming.security.credentials", password);
        } 
        else {
            env.remove("java.naming.security.principal");
            env.remove("java.naming.security.credentials");
        }
    }
    
    /**
     * sets the secure Level
     * @param secureLevel [SECURE_CFSSL_BASIC, SECURE_CFSSL_CLIENT_AUTH, SECURE_NONE]
     * @throws ClassNotFoundException 
     * @throws ClassException 
     */
    public void setSecureLevel(short secureLevel) throws ClassException {
        // Security
        if(secureLevel==SECURE_CFSSL_BASIC) {
            env.put("java.naming.security.protocol", "ssl");
            env.put("java.naming.ldap.factory.socket", "javax.net.ssl.SSLSocketFactory");
            //Class.orName("com.sun.net.ssl.internal.ssl.Provider");
            ClassUtil.loadClass("com.sun.net.ssl.internal.ssl.Provider");
            
            Security.addProvider(new Provider());
            
        } 
        else if(secureLevel==SECURE_CFSSL_CLIENT_AUTH) {
            env.put("java.naming.security.protocol", "ssl");
            env.put("java.naming.security.authentication", "EXTERNAL");
        }
        else {
            env.put("java.naming.security.authentication", "simple");
            env.remove("java.naming.security.protocol");
            env.remove("java.naming.ldap.factory.socket");
        } 
    }
    
    /**
     * sets thr referral
     * @param referral
     */
    public void setReferral(int referral) {
        if(referral > 0) {
            env.put("java.naming.referral", "follow");
            env.put("java.naming.ldap.referral.limit", Caster.toString(referral));
        } 
        else {
            env.put("java.naming.referral", "ignore");
            env.remove("java.naming.ldap.referral.limit");
        }
    }
    


	/**
	 * adds LDAP entries to LDAP server
	 * @param dn
	 * @param attributes
	 * @param delimiter 
	 * @throws NamingException 
	 * @throws PageException 
	 */
	public void add(String dn, String attributes, String delimiter, String seperator) throws NamingException, PageException {
		DirContext ctx = new InitialDirContext(env);
	    ctx.createSubcontext(dn, toAttributes(attributes,delimiter,seperator));
	    ctx.close();
	}
	
    /**
     * deletes LDAP entries on an LDAP server
     * @param dn
     * @throws NamingException
     */
    public void delete(String dn)  throws NamingException{
        DirContext ctx = new InitialDirContext(env);
        ctx.destroySubcontext(dn);
        ctx.close();
    }
    
    /**
     *  modifies distinguished name attribute for LDAP entries on LDAP server
     * @param dn
     * @param attributes
     * @throws NamingException 
     */
    public void modifydn(String dn, String attributes)  throws NamingException{
    	DirContext ctx = new InitialDirContext(env);
	    ctx.rename(dn, attributes);
	    ctx.close();
    }
    
    public void modify(String dn, int modifytype, String strAttributes, String delimiter, String separator) throws NamingException, PageException {

            DirContext context = new InitialDirContext(env);
            String strArrAttributes[] = toStringAttributes(strAttributes,delimiter);
            
            int count = 0;
            for(int i=0; i<strArrAttributes.length; i++) {
                String[] attributesValues = getAttributesValues(strArrAttributes[i], separator);
                if(attributesValues == null)count++;
                else count+=attributesValues.length;
            }
            
            ModificationItem modItems[] = new ModificationItem[count];
            BasicAttribute basicAttr = null;
            int k = 0;
            for(int i = 0; i < strArrAttributes.length; i++) {
                String attribute = strArrAttributes[i];
                String type = getAttrValueType(attribute);
                String values[] = getAttributesValues(attribute,separator);
                
                if(modifytype==DirContext.REPLACE_ATTRIBUTE) {
                    if(values == null) basicAttr = new BasicAttribute(type);
                    else basicAttr = new BasicAttribute(type, values[0]);
                    
                    modItems[k] = new ModificationItem(modifytype, basicAttr);
                    k++;
                    if(values != null && values.length > 1) {
                        for(int j = 1; j < values.length; j++) {
                            basicAttr = new BasicAttribute(type, values[j]);
                            modItems[k] = new ModificationItem(DirContext.ADD_ATTRIBUTE, basicAttr);
                            k++;
                        }
                    }
                } 
                else {
                    for(int j = 0; j < values.length; j++) {
                        if(type != null || modifytype==DirContext.ADD_ATTRIBUTE)
                             basicAttr = new BasicAttribute(type, values[j]);
                        else basicAttr = new BasicAttribute(values[j]);
                        modItems[k] = new ModificationItem(modifytype, basicAttr);
                        k++;
                    }
                }
            }

            context.modifyAttributes(dn, modItems);
            context.close();

    }
    
    
    
    /**
     * @param dn
     * @param strAttributes
     * @param scope
     * @param startrow
     * @param maxrows
     * @param timeout
     * @param sort
     * @param sortType
     * @param sortDirection
     * @param start
     * @param separator
     * @param filter
     * @return
     * @throws NamingException
     * @throws PageException
     * @throws IOException 
     */
    public Query query(String strAttributes,int scope, int startrow, int maxrows, int timeout, 
            String[] sort, int sortType, int sortDirection, 
            String start, String separator, String filter) 
        throws NamingException, PageException, IOException {
        //strAttributes=strAttributes.trim();
        boolean attEQAsterix=strAttributes.trim().equals("*");
        String[] attributes = attEQAsterix?new String[]{"name","value"}:toStringAttributes(strAttributes,",");
        
        
        
        // Control
        SearchControls controls = new SearchControls();
        controls.setReturningObjFlag(true);
        controls.setSearchScope(scope);
        if(!attEQAsterix)controls.setReturningAttributes(toStringAttributes(strAttributes,","));
        if(maxrows>0)controls.setCountLimit(startrow + maxrows + 1);
        if(timeout>0)controls.setTimeLimit(timeout);
        

        InitialLdapContext context = new InitialLdapContext(env, null);
        
        
        // Sort
        if(sort!=null && sort.length>0) {
            boolean isSortAsc=sortDirection==SORT_DIRECTION_ASC;
            
            SortKey keys[] = new SortKey[sort.length];
            for(int i=0;i<sort.length;i++) {
                String item=sort[i].equalsIgnoreCase("dn")?"name":sort[i];
                if(item.indexOf(' ')!=-1)item=List.first(item," ",true);
                keys[i] = new SortKey(item,isSortAsc ,sortType==LDAPClient.SORT_TYPE_CASE?null/*"CASE"*/:null);
                //keys[i] = new SortKey(item);
            }
            context.setRequestControls(new Control[]{new SortControl(keys, Control.CRITICAL)});
        }        
        
        // Search
        Query qry=new QueryImpl(attributes,0,"query");
        try {
            NamingEnumeration results = context.search(start, filter, controls);
            
            // Fill result
            int row=1;
            if(!attEQAsterix) {
                while(results.hasMoreElements()) {
                    SearchResult resultRow = (SearchResult)results.next();
                    if(row++<startrow)continue;
                    
                    int len=qry.addRow();
                    NamingEnumeration rowEnum = resultRow.getAttributes().getAll();
                    String dn = resultRow.getNameInNamespace(); 
                    qry.setAtEL("dn",len,dn);
                    while(rowEnum.hasMore()) {
                        Attribute attr = (Attribute)rowEnum.next();
                        String key = attr.getID();
                        Enumeration values = attr.getAll();
                        while(values.hasMoreElements()) {
                            qry.setAtEL(key,len,values.nextElement());
                            //print.ln(id+":"+values.nextElement());
                        }            
                    }
                    if(maxrows>0 && len>=maxrows)break;
                }
            }
            else {
                
                outer:while(results.hasMoreElements()) {
                    SearchResult resultRow = (SearchResult)results.next();
                    if(row++<startrow)continue;
                    
                    Attributes attributesRow = resultRow.getAttributes();
                    NamingEnumeration rowEnum = attributesRow.getIDs();
                    while(rowEnum.hasMoreElements()) {
                        int len=qry.addRow();
                        String name = Caster.toString(rowEnum.next());
                        Object value=null;
                        
                        try {
                            value=attributesRow.get(name).get();
                        }catch(Exception e) {}
                        
                        qry.setAtEL("name",len,name);
                        qry.setAtEL("value",len,value);
                        if(maxrows>0 && len>=maxrows)break outer;
                    }
                    qry.setAtEL("name",qry.size(),"dn");
                }
            }
        }
        finally {
            if(context!=null)context.close();
        }
        
        return qry;
    }

    private static String[] toStringAttributes(String strAttributes,String delimeter) throws PageException {
		return List.toStringArrayTrim(List.listToArrayRemoveEmpty(strAttributes,delimeter));		
	}
	
	private static Attributes toAttributes(String strAttributes,String delimeter, String separator) throws PageException {
		String[] arrAttr = toStringAttributes(strAttributes,delimeter);
		
		
		BasicAttributes attributes = new BasicAttributes();
        for(int i=0; i<arrAttr.length; i++) {
            String strAttr = arrAttr[i];
            
            // Type
            int eqIndex=strAttr.indexOf('=');
            Attribute attr = new BasicAttribute((eqIndex != -1)?strAttr.substring(0, eqIndex).trim():null);
            
            // Value
            String strValue = (eqIndex!=-1)?strAttr.substring( eqIndex+ 1):strAttr;
            String[] arrValue=List.toStringArray(List.listToArrayRemoveEmpty(strValue,separator));
            
            // Fill
            for(int y=0; y<arrValue.length; y++) {
                attr.add(arrValue[y]);
            }
            attributes.put(attr);
        }
        return attributes;
		
	}
    
    private String getAttrValueType(String attribute) {
        int eqIndex=attribute.indexOf("=");
        if(eqIndex != -1) return attribute.substring(0, eqIndex).trim();
        return null;
    }
    
    private String[] getAttributesValues(String attribute, String separator) throws PageException {
        String strValue = attribute.substring(attribute.indexOf("=") + 1);
        if(strValue.length() == 0) return null;
        return List.toStringArray(List.listToArrayRemoveEmpty(strValue,separator.equals(", ") ? "," : separator));
    }
		
}