package pt.tecnico.distledger.namingserver.domain.entry;


public class ServerEntry {
    
    private String target;

    private String qualifier;

    public ServerEntry(String target, String qualifier) {
        this.target = target;
        this.qualifier = qualifier;
    }

    public String getTarget() {
        return target;
    } 

    public String getQualifier() {
        return qualifier;
    }  

    public void setTarget(String target) {
        this.target = target;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

}
