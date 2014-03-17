package org.n52.sos.ioos;

import java.util.HashMap;

import org.apache.xmlbeans.XmlOptionCharEscapeMap;

/**
 * Special class that overrides XmlOptionCharEscapeMap to allow for proper encoding
 * of XML entities (like &#10;, which otherwise becomes &amp;10;)
 */
public class IoosXmlOptionCharEscapeMap extends XmlOptionCharEscapeMap {
    private HashMap<Character,String> charMap = new HashMap<Character,String>();

    public void setEscapeString(char ch, String str){
        charMap.put(new Character(ch), str);
    }

    public boolean containsChar(char ch){
        return charMap.containsKey(new Character(ch)) || super.containsChar(ch);
    }    
    
    public String getEscapedString(char ch){
        Character chr = new Character(ch);        
        if (charMap.containsKey(chr)) {
            return charMap.get(chr);
        }
        return super.getEscapedString(ch);
    }    
}
