package net.sf.dz3.view.http.v3;

/**
 * Protobuf renderer abstraction.
 *  
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2010-2017
 */
public interface ProtobufRenderer<State> {
    
    /**
     * Render the given object as a Protobuf encoded string.
     * 
     * @param source Object to render.
     * 
     * @return String representing the source object encoded with Protobuf.
     */
    String render(Object source);
}
